package com.cobo.coinlib.coins.polkadot.pallets.staking;

import com.cobo.coinlib.coins.polkadot.UOS.Network;
import com.cobo.coinlib.coins.polkadot.pallets.Parameter;
import com.cobo.coinlib.coins.polkadot.scale.ScaleCodecWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class IncreaseValidatorCountParameter extends Parameter {
    private final long additional;

    public IncreaseValidatorCountParameter(String name, Network network, int code, long additional) {
        super(name, network, code);
        this.additional = additional;
    }

    @Override
    protected JSONObject addCallParameter() throws JSONException {
        return new JSONObject().put("additional", additional);
    }

    @Override
    public void writeTo(ScaleCodecWriter scw) throws IOException {
        super.writeTo(scw);
        scw.writeLIntCompact(additional);
    }
}