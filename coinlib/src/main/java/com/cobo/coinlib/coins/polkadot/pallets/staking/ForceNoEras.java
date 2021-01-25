package com.cobo.coinlib.coins.polkadot.pallets.staking;

import com.cobo.coinlib.coins.polkadot.ScaleCodecReader;
import com.cobo.coinlib.coins.polkadot.UOS.Network;
import com.cobo.coinlib.coins.polkadot.pallets.EmptyParameter;
import com.cobo.coinlib.coins.polkadot.pallets.Pallet;

public class ForceNoEras extends Pallet<EmptyParameter> {
    public ForceNoEras(Network network, int code) {
        super("staking.forceNoEras", network, code);
    }

    @Override
    public EmptyParameter read(ScaleCodecReader scr) {
        return new EmptyParameter(name, network, code);
    }
}