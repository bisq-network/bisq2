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

package bisq.wallets.electrum.rpc.responses;

import bisq.wallets.core.model.Utxo;
import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ElectrumListUnspentResponse extends JsonRpcResponse<List<ElectrumListUnspentResponse.Result>> {
    @Getter
    public static class Result implements Utxo {
        private String address;
        @Json(name = "bip32_paths")
        private Map<String, String> bip32Paths;

        private boolean coinbase;
        private int height;
        @Json(name = "nsequence")
        private long nSequence;
        @Json(name = "part_sigs")
        private Map<String, String> partSigs;

        @Json(name = "prevout_hash")
        private String prevOutHash;
        @Json(name = "prevout_n")
        private int prevOutN;

        @Json(name = "redeem_script")
        private String redeemScript;
        @Json(name = "sighash")
        private String sigHash;
        @Json(name = "unknown_psbt_fields")
        private Map<String, String> unknownPsbtFields;

        private String utxo;
        private String value;

        @Json(name = "witness_script")
        private String witnessScript;
        @Json(name = "witness_utxo")
        private String witnessUtxo;

        @Override
        public String getTxId() {
            return prevOutHash;
        }

        @Override
        public double getAmount() {
            return Double.parseDouble(value);
        }
    }
}
