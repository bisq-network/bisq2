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

package bisq.wallets.elementsd.rpc.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ElementsdGetAddressInfoResponse {
    private String address;
    private String scriptPubKey;

    @JsonProperty("ismine")
    private boolean isMine;
    private boolean solvable;
    private String desc;

    @JsonProperty("iswatchonly")
    private boolean isWatchOnly;
    @JsonProperty("isscript")
    private boolean isScript;

    @JsonProperty("iswitness")
    private boolean isWitness;
    @JsonProperty("witness_version")
    private int witnessVersion;
    @JsonProperty("witness_program")
    private String witnessProgram;

    @JsonProperty("pubkey")
    private String pubKey;
    @JsonProperty("confidential")
    private String confidential;
    @JsonProperty("confidential_key")
    private String confidentialKey;
    @JsonProperty("unconfidential")
    private String unconfidential;

    @JsonProperty("ischange")
    private boolean isChange;
    private long timestamp;

    @JsonProperty("hdkeypath")
    private String hdKeyPath;
    @JsonProperty("hdseedid")
    private String hdSeedId;
    @JsonProperty("hdmasterfingerprint")
    private String hdMasterFingerprint;

    private List<String> labels;

}
