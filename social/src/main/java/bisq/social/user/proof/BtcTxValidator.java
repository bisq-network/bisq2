/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.social.user.proof;

import bisq.common.data.Pair;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BtcTxValidator {

    public static boolean initialSanityChecks(String txId, String jsonTxt) {
        if (jsonTxt == null || jsonTxt.length() == 0) {
            return false;
        }
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        // there should always be "txid" string element at the top level
        if (json.get("txid") == null) {
            return false;
        }
        // txid should match what we requested
        if (!txId.equals(json.get("txid").getAsString())) {
            return false;
        }
        return true;
    }

    public static String getFirstInputPubKey(String jsonTxt) {
        try {
            Pair<JsonArray, JsonArray> vinAndVout = getVinAndVout(jsonTxt);
            JsonArray vinArray = vinAndVout.getFirst();
            for (JsonElement x : vinArray) {
                JsonObject vin = x.getAsJsonObject();
                // pubKey in witness or scriptsig (legacy or segwit txs)
                JsonArray witnesses = vin.getAsJsonArray("witness");
                int PUBLIC_KEY_LENGTH = 33;
                if (witnesses != null) {
                    String witnessPubKey = witnesses.get(1).getAsString();
                    if (witnessPubKey.length() >= PUBLIC_KEY_LENGTH * 2) {
                        return witnessPubKey;
                    }
                }
                JsonElement scriptsig = vin.get("scriptsig");
                if (scriptsig != null) {
                    String scriptsigAsHex = scriptsig.getAsString();
                    if (scriptsigAsHex.length() >= PUBLIC_KEY_LENGTH * 2) {
                        return scriptsigAsHex.substring(scriptsigAsHex.length() - PUBLIC_KEY_LENGTH * 2);
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            log.error("json error:", e);
        }
        throw new JsonSyntaxException("could not find pubKey");
    }

    private static Pair<JsonArray, JsonArray> getVinAndVout(String jsonTxt) throws JsonSyntaxException {
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        if (json.get("vin") == null || json.get("vout") == null) {
            throw new JsonSyntaxException("missing vin/vout");
        }
        JsonArray jsonVin = json.get("vin").getAsJsonArray();
        JsonArray jsonVout = json.get("vout").getAsJsonArray();
        if (jsonVin == null || jsonVout == null || jsonVin.size() < 1 || jsonVout.size() < 1) {
            throw new JsonSyntaxException("not enough vins/vouts");
        }
        return new Pair<>(jsonVin, jsonVout);
    }
}
