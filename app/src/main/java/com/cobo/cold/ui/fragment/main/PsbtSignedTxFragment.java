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

package com.cobo.cold.ui.fragment.main;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.cobo.cold.R;
import com.cobo.cold.databinding.CommonModalBinding;
import com.cobo.cold.db.entity.TxEntity;
import com.cobo.cold.ui.fragment.main.electrum.SignedTxFragment;
import com.cobo.cold.ui.modal.ModalDialog;
import com.cobo.cold.ui.views.qrcode.DynamicQrCodeView;
import com.cobo.cold.viewmodel.WatchWallet;

import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static com.cobo.cold.ui.fragment.main.PsbtTxConfirmFragment.showExportPsbtDialog;

public class PsbtSignedTxFragment extends SignedTxFragment {
    @Override
    protected void displaySignResult(TxEntity txEntity) {
        if (watchWallet == WatchWallet.WASABI) {
            mBinding.txDetail.qr.setVisibility(View.GONE);
            mBinding.txDetail.broadcastGuide.setVisibility(View.GONE);
        } else if (watchWallet == WatchWallet.BLUE) {
            mBinding.txDetail.info.setOnClickListener(v -> showBlueWalletInfo());
            mBinding.txDetail.scanHint.setText(R.string.use_blue_to_broadcast);
            mBinding.txDetail.exportToSdcardHint.setVisibility(View.GONE);
            mBinding.txDetail.qrcodeLayout.qrcode.setVisibility(View.GONE);
            mBinding.txDetail.dynamicQrcodeLayout.qrcode.setVisibility(View.VISIBLE);
            mBinding.txDetail.broadcastGuide.setVisibility(View.GONE);
            mBinding.txDetail.export.setVisibility(View.GONE);
            mBinding.txDetail.dynamicQrcodeLayout.qrcode.setEncodingScheme(DynamicQrCodeView.EncodingScheme.Bc32);
            mBinding.txDetail.dynamicQrcodeLayout.qrcode.setData(Hex.toHexString(Base64.decode(txEntity.getSignedHex())));
        }
    }

    private void showBlueWalletInfo() {
        ModalDialog modalDialog = ModalDialog.newInstance();
        CommonModalBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(mActivity), R.layout.common_modal,
                null, false);
        binding.title.setText(R.string.blue_wallet_broadcast_guide);
        binding.subTitle.setText(R.string.blue_wallet_broadcast_guide1);
        binding.subTitle.setGravity(Gravity.START);
        binding.close.setVisibility(View.GONE);
        binding.confirm.setText(R.string.know);
        binding.confirm.setOnClickListener(vv -> modalDialog.dismiss());
        modalDialog.setBinding(binding);
        modalDialog.show(mActivity.getSupportFragmentManager(), "");
    }

    @Override
    protected void showExportDialog() {
        showExportPsbtDialog(mActivity, txEntity.getTxId(),
                txEntity.getSignedHex(), () -> popBackStack(R.id.assetFragment, false));
    }
}
