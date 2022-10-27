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

import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElectrumGetInfoResponse extends JsonRpcResponse<ElectrumGetInfoResponse.Result> {
    @Getter
    @Setter
    public static class Result {
        @Json(name = "auto_connect")
        private boolean isAutoConnectEnabled;
        @Json(name = "blockchain_height")
        private int blockchainHeight;
        @Json(name = "connected")
        private boolean isConnected;

        @Json(name = "default_wallet")
        private String defaultWallet;
        @Json(name = "fee_per_kb")
        private int feePerKb;

        private String path;
        private String server;
        @Json(name = "server_height")
        private int serverHeight;
        @Json(name = "spv_nodes")
        private int spvNodes;
        private String version;
    }
}
