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

package bisq.wallets.bitcoind.rpc.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitcoindGetAddressInfoResponse {
    private String address;
    private String scriptPubKey;
    @JsonProperty("ismine")
    private boolean isMine;
    @JsonProperty("iswatchonly")
    private boolean isWatchOnly;
    private boolean solvable;

    private String desc;
    @JsonProperty("parent_desc")
    private String parentDesc;

    @JsonProperty("isscript")
    private boolean isScript;
    @JsonProperty("ischange")
    private boolean isChange;
    @JsonProperty("iswitness")
    private boolean isWitness;
    @JsonProperty("witness_version")
    private int witnessVersion;
    @JsonProperty("witness_program")
    private String witnessProgram;
    private String script;
    private String hex;
    @JsonProperty("pubkeys")
    private String[] pubKeys;
    @JsonProperty("sigsrequired")
    private int sigsRequired;
    @JsonProperty("pubkey")
    private String pubKey;
    private Object embedded;
    @JsonProperty("iscompressed")
    private boolean isCompressed;
    private int timestamp;
    @JsonProperty("hdkeypath")
    private String hdKeyPath;
    @JsonProperty("hdseedid")
    private String hdSeedId;
    @JsonProperty("hdmasterfingerprint")
    private String hdMasterFingerprint;
    private String[] labels;
}
