/*
 *
 *  Copyright (c) 2020 Cobo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * in the file COPYING.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cobo.cold.ui.fragment.main;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.cobo.cold.R;
import com.cobo.cold.callables.FingerprintPolicyCallable;
import com.cobo.cold.databinding.AbiItemBinding;
import com.cobo.cold.databinding.EthTxConfirmBinding;
import com.cobo.cold.db.entity.TxEntity;
import com.cobo.cold.ui.fragment.BaseFragment;
import com.cobo.cold.ui.fragment.setup.PreImportFragment;
import com.cobo.cold.ui.modal.ModalDialog;
import com.cobo.cold.ui.modal.SigningDialog;
import com.cobo.cold.ui.views.AuthenticateModal;
import com.cobo.cold.viewmodel.EthTxConfirmViewModel;
import com.cobo.cold.viewmodel.TxConfirmViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cobo.cold.callables.FingerprintPolicyCallable.READ;
import static com.cobo.cold.callables.FingerprintPolicyCallable.TYPE_SIGN_TX;
import static com.cobo.cold.ui.fragment.main.BroadcastTxFragment.KEY_TXID;
import static com.cobo.cold.ui.fragment.main.TxConfirmFragment.KEY_TX_DATA;
import static com.cobo.cold.ui.fragment.setup.PreImportFragment.ACTION;

public class EthTxConfirmFragment extends BaseFragment<EthTxConfirmBinding> {

    private EthTxConfirmViewModel viewModel;
    private SigningDialog signingDialog;
    private TxEntity txEntity;
    private final Runnable forgetPassword = () -> {
        Bundle bundle = new Bundle();
        bundle.putString(ACTION, PreImportFragment.ACTION_RESET_PWD);
        navigate(R.id.action_to_preImportFragment, bundle);
    };

    public static Pattern pattern = Pattern.compile("(?<=\\()[^\\)]+");
    public static Pattern pattern1 = Pattern.compile("(?<=\\[)[^]]+");

    @Override
    protected int setView() {
        return R.layout.eth_tx_confirm;
    }

    @Override
    protected void init(View view) {
        Bundle data = Objects.requireNonNull(getArguments());
        mBinding.toolbar.setNavigationOnClickListener(v -> navigateUp());
        viewModel = ViewModelProviders.of(this).get(EthTxConfirmViewModel.class);
        try {
            JSONObject txData = new JSONObject(data.getString(KEY_TX_DATA));
            viewModel.parseTxData(txData);
            viewModel.getObservableTx().observe(this, txEntity -> {
                this.txEntity = txEntity;
                if (this.txEntity != null) {
                    updateUI();
                }
            });
            viewModel.parseTxException().observe(this, this::handleParseException);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mBinding.sign.setOnClickListener(v -> handleSign());
    }

    private void handleParseException(Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
            ModalDialog.showCommonModal(mActivity,
                    getString(R.string.scan_failed),
                    getString(R.string.incorrect_tx_data),
                    getString(R.string.confirm),
                    null);
            viewModel.parseTxException().setValue(null);
            popBackStack(R.id.assetFragment, false);
        }
    }

    private void handleSign() {
        boolean fingerprintSignEnable = new FingerprintPolicyCallable(READ, TYPE_SIGN_TX).call();
        AuthenticateModal.show(mActivity,
                getString(R.string.password_modal_title), "", fingerprintSignEnable,
                token -> {
                    viewModel.setToken(token);
                    viewModel.handleSign();
                    subscribeSignState();
                }, forgetPassword);
    }

    private void subscribeSignState() {
        viewModel.getSignState().observe(this, s -> {
            if (TxConfirmViewModel.STATE_SIGNING.equals(s)) {
                signingDialog = SigningDialog.newInstance();
                signingDialog.show(mActivity.getSupportFragmentManager(), "");
            } else if (TxConfirmViewModel.STATE_SIGN_SUCCESS.equals(s)) {
                if (signingDialog != null) {
                    signingDialog.setState(SigningDialog.STATE_SUCCESS);
                }
                new Handler().postDelayed(() -> {
                    if (signingDialog != null) {
                        signingDialog.dismiss();
                    }
                    signingDialog = null;
                    onSignSuccess();
                }, 500);
            } else if (TxConfirmViewModel.STATE_SIGN_FAIL.equals(s)) {
                if (signingDialog == null) {
                    signingDialog = SigningDialog.newInstance();
                    signingDialog.show(mActivity.getSupportFragmentManager(), "");
                }
                new Handler().postDelayed(() -> signingDialog.setState(SigningDialog.STATE_FAIL), 1000);
                new Handler().postDelayed(() -> {
                    if (signingDialog != null) {
                        signingDialog.dismiss();
                    }
                    signingDialog = null;
                    viewModel.getSignState().removeObservers(this);
                }, 2000);
            }
        });
    }

    private void onSignSuccess() {
        String txId = viewModel.getTxId();
        Bundle data = new Bundle();
        data.putString(KEY_TXID, txId);
        navigate(R.id.action_to_ethBroadcastTxFragment, data);
        viewModel.getSignState().setValue("");
        viewModel.getSignState().removeObservers(this);
    }

    private void updateUI() {
        updateNetworkName();
        JSONObject abi = viewModel.getAbi();
        if (abi != null) {
            updateAbiView(abi);
        } else {
            mBinding.ethTx.data.setVisibility(View.GONE);
        }
        mBinding.ethTx.setTx(txEntity);
        processAndUpdateTo();
    }

    private void updateNetworkName() {
        mBinding.ethTx.network.setText(viewModel.getNetwork(viewModel.getChainId()));
    }

    private void processAndUpdateTo() {
        String to = txEntity.getTo();
        String addressSymbol = viewModel.recognizeAddress(to);
        if (addressSymbol != null) {
            to = to + String.format(" (%s)", addressSymbol);
        } else {
            to = to + String.format(" [%s]", "Unknown Address");
        }
        mBinding.ethTx.to.setText(highLight(to));
    }

    private void updateAbiView(JSONObject abi) {
        if (abi != null) {
            try {
                String contract = abi.getString("contract");
                boolean isUniswap = contract.toLowerCase().contains("uniswap");
                List<AbiItemAdapter.AbiItem> itemList = new AbiItemAdapter(txEntity.getFrom(),viewModel).adapt(abi);
                for (AbiItemAdapter.AbiItem item : itemList) {
                    AbiItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mActivity),
                            R.layout.abi_item, null, false);
                    binding.key.setText(item.key);
                    if (isUniswap && "to".equals(item.key)) {
                        if (!item.value.equalsIgnoreCase(txEntity.getFrom())) {
                            item.value += String.format(" [%s]",getString(R.string.inconsistent_address));
                        }
                        binding.value.setText(highLight(item.value));
                    } else {
                        binding.value.setText(highLight(item.value));
                    }
                    mBinding.ethTx.container.addView(binding.getRoot());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }
    public static SpannableStringBuilder highLight(String content) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find())
            spannable.setSpan(new ForegroundColorSpan(0xff00cdc3), matcher.start() - 1 ,
                    matcher.end() + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        matcher = pattern1.matcher(spannable);
        while (matcher.find()) {
            spannable.replace(matcher.start() - 1, matcher.start(),"(");
            spannable.replace(matcher.end(), matcher.end() + 1,")");
            spannable.setSpan(new ForegroundColorSpan(Color.RED), matcher.start() - 1 ,
                    matcher.end() + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return spannable;
    }
}
