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

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.cobo.cold.R;
import com.cobo.cold.databinding.AbiItemBinding;
import com.cobo.cold.databinding.EthTxBinding;
import com.cobo.cold.db.entity.TxEntity;
import com.cobo.cold.ui.fragment.BaseFragment;
import com.cobo.cold.viewmodel.CoinListViewModel;
import com.cobo.cold.viewmodel.EthTxConfirmViewModel;
import com.cobo.cold.viewmodel.WatchWallet;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.cobo.cold.ui.fragment.main.EthTxConfirmFragment.highLight;
import static com.cobo.cold.ui.fragment.main.TxFragment.KEY_TX_ID;

public class EthTxFragment extends BaseFragment<EthTxBinding> {

    private TxEntity txEntity;
    private EthTxConfirmViewModel viewModel;

    @Override
    protected int setView() {
        return R.layout.eth_tx;
    }

    @Override
    protected void init(View view) {
        Bundle bundle = Objects.requireNonNull(getArguments());
        mBinding.toolbar.setNavigationOnClickListener(v -> navigateUp());
        mBinding.broadcastHint.setText(getString(R.string.please_broadcast_with_hot,
                WatchWallet.METAMASK.getWalletName(mActivity)));
        ViewModelProviders.of(mActivity).get(CoinListViewModel.class)
                .loadTx(bundle.getString(KEY_TX_ID)).observe(this, txEntity -> {
            this.txEntity = txEntity;
            if (this.txEntity != null) {
                updateUI();
            }
        });
        viewModel = ViewModelProviders.of(this).get(EthTxConfirmViewModel.class);
    }

    private void updateUI() {
        int chainId = 1;
        JSONObject signed = null;
        JSONObject abi = null;
        try {
            signed = new JSONObject(txEntity.getSignedHex());
            chainId = signed.optInt("chainId",1);
            abi = signed.getJSONObject("abi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mBinding.ethTx.network.setText(viewModel.getNetwork(chainId));
        showQrCode(signed);
        updateAbiView(abi);
        mBinding.ethTx.setTx(txEntity);
        processAndUpdateTo();
    }

    private void showQrCode(JSONObject signed) {
        if (signed != null) {
            signed.remove("abi");
            signed.remove("chainId");
            mBinding.qrcode.qrcode.setData(Hex.toHexString(signed.toString().getBytes(StandardCharsets.UTF_8)));
        }
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
                        SpannableStringBuilder spannableString = highLight(item.value);
                        binding.value.setText(spannableString);
                    }
                    mBinding.ethTx.container.addView(binding.getRoot());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mBinding.ethTx.data.setVisibility(View.GONE);
        }
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

    @Override
    protected void initData(Bundle savedInstanceState) {

    }
}
