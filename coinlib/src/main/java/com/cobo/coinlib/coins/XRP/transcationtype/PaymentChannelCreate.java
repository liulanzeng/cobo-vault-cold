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

package com.cobo.coinlib.coins.XRP.transcationtype;

import com.cobo.coinlib.coins.XRP.Schemas;
import com.cobo.coinlib.coins.XRP.XrpTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class PaymentChannelCreate extends XrpTransaction {

    public PaymentChannelCreate() { super(Schemas.PaymentChannelCreate); }

    @Override
    public JSONObject flatTransactionDetail(JSONObject tx) {
        JSONObject result = new JSONObject();
        try {
            result.put("TransactionType", tx.getString("TransactionType"));
            result.put("Account", tx.getString("Account"));
            result.put("Amount", tx.getString("Amount") + " drops");
            result.put("Destination", tx.getString("Destination"));
            if(tx.has("DestinationTag")){
                result.put("DestinationTag", tx.getInt("DestinationTag"));
            }
            result.put("SettleDelay", tx.getInt("SettleDelay"));
            result.put("PublicKey", tx.getString("PublicKey"));
            if(tx.has("CancelAfter")) {
                result.put("CancelAfter", tx.getInt("CancelAfter"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  result;
    }
}
