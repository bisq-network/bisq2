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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ElectrumListUnspentResponseEntry {
    private String address;
    @JsonProperty("bip32_paths")
    private Map<String, String> bip32Paths;

    private boolean coinbase;
    private int height;
    @JsonProperty("nsequence")
    private long nSequence;
    @JsonProperty("part_sigs")
    private Map<String, String> partSigs;

    @JsonProperty("prevout_hash")
    private String prevOutHash;
    @JsonProperty("prevout_n")
    private int prevOutN;

    @JsonProperty("redeem_script")
    private String redeemScript;
    @JsonProperty("sighash")
    private String sigHash;
    @JsonProperty("unknown_psbt_fields")
    private Map<String, String> unknownPsbtFields;

    private String utxo;
    private String value;

    @JsonProperty("witness_script")
    private String witnessScript;
    @JsonProperty("witness_utxo")
    private String witnessUtxo;
}
