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

import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;

public class BitcoindGetAddressInfoResponse extends JsonRpcResponse<BitcoindGetAddressInfoResponse.Result> {
    @Getter
    public static class Result {
        private String address;
        private String scriptPubKey;
        @Json(name = "ismine")
        private boolean isMine;
        @Json(name = "iswatchonly")
        private boolean isWatchOnly;
        private boolean solvable;

        private String desc;
        @Json(name = "parent_desc")
        private String parentDesc;

        @Json(name = "isscript")
        private boolean isScript;
        @Json(name = "ischange")
        private boolean isChange;
        @Json(name = "iswitness")
        private boolean isWitness;
        @Json(name = "witness_version")
        private int witnessVersion;
        @Json(name = "witness_program")
        private String witnessProgram;
        private String script;
        private String hex;
        @Json(name = "pubkeys")
        private String[] pubKeys;
        @Json(name = "sigsrequired")
        private int sigsRequired;
        @Json(name = "pubkey")
        private String pubKey;
        private Object embedded;
        @Json(name = "iscompressed")
        private boolean isCompressed;
        private int timestamp;
        @Json(name = "hdkeypath")
        private String hdKeyPath;
        @Json(name = "hdseedid")
        private String hdSeedId;
        @Json(name = "hdmasterfingerprint")
        private String hdMasterFingerprint;
        private String[] labels;
    }
}
