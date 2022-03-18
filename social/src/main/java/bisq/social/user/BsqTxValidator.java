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

package bisq.social.user;

import bisq.common.data.Pair;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BsqTxValidator {

    public static boolean initialSanityChecks(String txId, String jsonTxt) {
        if (jsonTxt == null || jsonTxt.length() == 0) {
            return false;
        }
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        // there should always be "id" string element at the top level
        if (json.get("id") == null) {
            return false;
        }
        // txid should match what we requested
        if (!txId.equals(json.get("id").getAsString())) {
            return false;
        }
        return true;
    }

    public static boolean isBsqTx(String url, String txId, String jsonTxt) {
        return url.matches(".*bisq.*") && initialSanityChecks(txId, jsonTxt);
    }

    public static boolean isProofOfBurn(String jsonTxt) {
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        JsonElement txType = json.get("txType");
        if (txType == null) {
            return false;
        }
        return txType.getAsString().equalsIgnoreCase("PROOF_OF_BURN");
    }

    public static boolean isLockup(String jsonTxt) {
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        JsonElement txType = json.get("txType");
        if (txType == null) {
            return false;
        }
        return txType.getAsString().equalsIgnoreCase("LOCKUP");
    }

    public static long getBurntAmount(String jsonTxt) {
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        JsonElement burntFee = json.get("burntFee");
        if (burntFee == null) {
            return 0;   // no json element, assume zero burnt amount
        }
        return burntFee.getAsLong();
    }

    public static Optional<String> getOpReturnData(String jsonTxt) {
        try {
            Pair<JsonArray, JsonArray> vinAndVout = getVinAndVout(jsonTxt);
            JsonArray voutArray = vinAndVout.second();
            for (JsonElement x : voutArray) {
                JsonObject y = x.getAsJsonObject();
                if (y.get("txOutputType").getAsString().matches(".*OP_RETURN.*")) {
                    return Optional.of(y.get("opReturn").getAsString());
                }
            }
        } catch (JsonSyntaxException e) {
            log.error("json error:", e);
        }
        return Optional.empty();
    }

    private static Pair<JsonArray, JsonArray> getVinAndVout(String jsonTxt) throws JsonSyntaxException {
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        if (json.get("inputs") == null || json.get("outputs") == null) {
            throw new JsonSyntaxException("missing vin/vout");
        }
        JsonArray jsonVin = json.get("inputs").getAsJsonArray();
        JsonArray jsonVout = json.get("outputs").getAsJsonArray();
        if (jsonVin == null || jsonVout == null || jsonVin.size() < 1 || jsonVout.size() < 1) {
            throw new JsonSyntaxException("not enough vins/vouts");
        }
        return new Pair<>(jsonVin, jsonVout);
    }
}
