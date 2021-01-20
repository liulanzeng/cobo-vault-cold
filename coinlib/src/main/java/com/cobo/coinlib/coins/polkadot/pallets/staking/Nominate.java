package com.cobo.coinlib.coins.polkadot.pallets.staking;

import com.cobo.coinlib.coins.polkadot.UOS.Network;
import com.cobo.coinlib.coins.polkadot.pallets.Pallet;
import com.cobo.coinlib.coins.polkadot.ScaleCodecReader;

import java.util.Arrays;
import java.util.List;

public class Nominate extends Pallet<NominateParameter> {
    public Nominate(Network network, int code){
        super("staking.nominate", network, code);
    }

    @Override
    public NominateParameter read(ScaleCodecReader scr) {
        List<byte[]> publicKeys = Arrays.asList();
        int length = scr.readUByte();
        for (int i = 0; i< length; i++) {
            publicKeys.add(scr.readByteArray(32));
        }
        return new NominateParameter(network, name, code, length, publicKeys);
    }
}