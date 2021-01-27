package com.cobo.coinlib.coins.polkadot.pallets.utility;

import com.cobo.coinlib.coins.polkadot.ScaleCodecReader;
import com.cobo.coinlib.coins.polkadot.UOS.Network;
import com.cobo.coinlib.coins.polkadot.pallets.Pallet;

public class BatchBase extends Pallet<BatchParameter> {

    public BatchBase(String name, Network network, int code) {
        super(name, network, code);
    }

    @Override
    public BatchParameter read(ScaleCodecReader scr) {
        return new BatchParameter(network, name, code, scr);
    }
}
