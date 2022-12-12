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

import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ElementsdGetAddressInfoResponse extends JsonRpcResponse<ElementsdGetAddressInfoResponse.Result> {
    @Getter
    public static class Result {
        private String address;
        private String scriptPubKey;

        @Json(name = "ismine")
        private boolean isMine;
        private boolean solvable;
        private String desc;

        @Json(name = "iswatchonly")
        private boolean isWatchOnly;
        @Json(name = "isscript")
        private boolean isScript;

        @Json(name = "iswitness")
        private boolean isWitness;
        @Json(name = "witness_version")
        private int witnessVersion;
        @Json(name = "witness_program")
        private String witnessProgram;

        @Json(name = "pubkey")
        private String pubKey;
        @Json(name = "confidential")
        private String confidential;
        @Json(name = "confidential_key")
        private String confidentialKey;
        @Json(name = "unconfidential")
        private String unconfidential;

        @Json(name = "ischange")
        private boolean isChange;
        private long timestamp;

        @Json(name = "hdkeypath")
        private String hdKeyPath;
        @Json(name = "hdseedid")
        private String hdSeedId;
        @Json(name = "hdmasterfingerprint")
        private String hdMasterFingerprint;

        private List<String> labels;
    }
}
