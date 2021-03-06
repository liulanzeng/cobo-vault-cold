/*
 * Copyright (c) 2020 Cobo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * in the file COPYING.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cobo.cold.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.cobo.coinlib.utils.Coins;
import com.cobo.cold.R;
import com.cobo.cold.databinding.CommonModalBinding;
import com.cobo.cold.databinding.SyncFragmentBinding;
import com.cobo.cold.ui.MainActivity;
import com.cobo.cold.ui.modal.ModalDialog;
import com.cobo.cold.viewmodel.SyncViewModel;
import com.cobo.cold.viewmodel.WatchWallet;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

import static com.cobo.cold.ui.fragment.setup.SyncWatchWalletGuide.getSyncWatchWalletGuide;
import static com.cobo.cold.ui.fragment.setup.SyncWatchWalletGuide.getSyncWatchWalletGuideTitle;

public class SyncFragment extends BaseFragment<SyncFragmentBinding> {

    public static final String TAG = "SyncFragment";
    private SyncViewModel viewModel;
    private WatchWallet watchWallet;
    private String coinCode;
    private boolean fromSyncGuide = false;

    @Override
    protected int setView() {
        return R.layout.sync_fragment;
    }

    @Override
    protected void init(View view) {
        watchWallet = WatchWallet.getWatchWallet(mActivity);
        Bundle data = getArguments();
        if (data != null) {
            coinCode = data.getString("coinCode");
            fromSyncGuide = getArguments().getBoolean("fromSyncGuide");
        }
        if (!fromSyncGuide) {
            mBinding.complete.setVisibility(View.GONE);
        }
        mBinding.toolbar.setNavigationOnClickListener(v -> navigateUp());
        mBinding.toolbar.setTitle("");
        mBinding.complete.setOnClickListener(v -> {
            if (mActivity instanceof MainActivity) {
                if (watchWallet == WatchWallet.POLKADOT_JS && coinCode.equals(Coins.DOT.coinCode())) {
                    Bundle bundle = getArguments();
                    bundle.putString("coinCode", Coins.KSM.coinCode());
                    navigate(R.id.action_to_syncWatchWalletGuide, bundle);
                } else {
                    startActivity(new Intent(mActivity, MainActivity.class));
                    mActivity.finish();
                }
            } else {
                if (watchWallet == WatchWallet.POLKADOT_JS && coinCode.equals(Coins.DOT.coinCode())) {
                    Bundle bundle = getArguments();
                    bundle.putString("coinCode", Coins.KSM.coinCode());
                    navigate(R.id.action_to_syncWatchWalletGuide, bundle);
                } else {
                    navigate(R.id.action_to_setupCompleteFragment);
                }
            }
        });
        setupUIWithWatchWallet();
        viewModel = ViewModelProviders.of(mActivity).get(SyncViewModel.class);
        generateSyncData();
    }

    private void setupUIWithWatchWallet() {
        mBinding.info.setOnClickListener(v -> showHint());
        switch (watchWallet) {
            case COBO:
                mBinding.hint.setText(R.string.sync_with_cobo_vault);
                break;
            case POLKADOT_JS:
                mBinding.hint.setText(R.string.sync_with_polkadot_js);
                mBinding.chain.setVisibility(View.VISIBLE);
                if (coinCode.equals(Coins.DOT.coinCode())) {
                    mBinding.chain.setText(Coins.DOT.coinName());
                } else if(coinCode.equals(Coins.KSM.coinCode())) {
                    mBinding.chain.setText(Coins.KSM.coinName());
                }
                break;
            case XRP_TOOLKIT:
                mBinding.hint.setText(R.string.sync_with_xrp_toolkit);
                mBinding.address.setVisibility(View.VISIBLE);
                break;
            case METAMASK:
                mBinding.hint.setText(R.string.sync_with_metamask);
                break;
        }
    }

    private void showHint() {
        ModalDialog modalDialog = ModalDialog.newInstance();
        CommonModalBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(mActivity), R.layout.common_modal,
                null, false);
        binding.title.setText(getString(getSyncWatchWalletGuideTitle(watchWallet), coinCode));
        binding.subTitle.setText(getString(getSyncWatchWalletGuide(watchWallet),
                Coins.coinNameFromCoinCode(coinCode), coinCode));
        binding.subTitle.setGravity(Gravity.START);
        binding.close.setVisibility(View.GONE);
        binding.confirm.setText(R.string.know);
        binding.confirm.setOnClickListener(vv -> modalDialog.dismiss());
        modalDialog.setBinding(binding);
        modalDialog.show(mActivity.getSupportFragmentManager(), "");
    }

    private void generateSyncData() {
        switch (watchWallet) {
            case COBO:
                viewModel.generateSyncCobo().observe(this, sync -> {
                    if (!TextUtils.isEmpty(sync)) {
                        mBinding.dynamicQrcodeLayout.qrcode.setData(sync);
                    }
                });
                break;
            case XRP_TOOLKIT:
                int index = getArguments().getInt("index");
                viewModel.generateSyncXumm(index).observe(this, xrpSyncData -> {
                    if (xrpSyncData != null) {
                        mBinding.dynamicQrcodeLayout.qrcode.setData(generateXrpToolsSyncData(xrpSyncData));
                        mBinding.addressName.setText(xrpSyncData.addressEntity.getName());
                        mBinding.addressInfo.setText(String.format("%s\n(%s)",
                                xrpSyncData.addressEntity.getAddressString(),
                                xrpSyncData.addressEntity.getPath()));
                    }
                });
                break;
            case POLKADOT_JS:
                viewModel.generateSyncPolkadotjs(coinCode).observe(this, s -> {
                    if (!TextUtils.isEmpty(s)) {
                        mBinding.dynamicQrcodeLayout.qrcode.disableMultipart();
                        mBinding.dynamicQrcodeLayout.qrcode.setData(s);
                    }
                });
                break;
            case METAMASK:
                viewModel.generateSyncMetamask().observe(this, s -> {
                    if (!TextUtils.isEmpty(s)) {
                        mBinding.dynamicQrcodeLayout.qrcode.disableMultipart();
                        mBinding.dynamicQrcodeLayout.qrcode.setData(s);
                    }
                });
        }
    }

    private String generateXrpToolsSyncData(SyncViewModel.XrpSyncData xrpSyncData) {
        JSONObject object = new JSONObject();
        try {
            object.put("address", xrpSyncData.addressEntity.getAddressString())
                    .put("pubkey", xrpSyncData.pubkey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Hex.toHexString(object.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }
}
