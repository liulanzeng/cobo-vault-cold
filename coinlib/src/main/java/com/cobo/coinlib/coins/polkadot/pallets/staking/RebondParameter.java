package com.cobo.coinlib.coins.polkadot.pallets.staking;

import com.cobo.coinlib.coins.polkadot.ScaleCodecReader;
import com.cobo.coinlib.coins.polkadot.UOS.Network;
import com.cobo.coinlib.coins.polkadot.pallets.Parameter;
import com.cobo.coinlib.coins.polkadot.pallets.Utils;
import com.cobo.coinlib.coins.polkadot.scale.ScaleCodecWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;

public class RebondParameter  extends Parameter {
    private BigInteger value;
    public RebondParameter(String name, Network network, int code, ScaleCodecReader scr) {
        super(name, network, code, scr);
    }

    @Override
    protected void read(ScaleCodecReader scr) {
        value = scr.readCompact();
    }

    @Override
    protected JSONObject addCallParameter() throws JSONException {
        return new JSONObject().put("Value", Utils.getReadableBalanceString(network, value));
    }

    @Override
    public void write(ScaleCodecWriter scw) throws IOException {
        scw.writeBIntCompact(value);
    }
}
