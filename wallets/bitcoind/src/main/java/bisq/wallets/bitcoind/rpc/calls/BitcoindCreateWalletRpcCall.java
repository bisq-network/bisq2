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

package bisq.wallets.bitcoind.rpc.calls;

import bisq.wallets.bitcoind.rpc.responses.BitcoindCreateOrLoadWalletResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;
import com.squareup.moshi.Json;
import lombok.Builder;
import lombok.Getter;

public class BitcoindCreateWalletRpcCall
        extends DaemonRpcCall<BitcoindCreateWalletRpcCall.Request, BitcoindCreateOrLoadWalletResponse> {

    @Builder
    @Getter
    public static class Request {
        @Json(name = "wallet_name")
        private String walletName;

        @Json(name = "disable_private_keys")
        private Boolean disablePrivateKeys;
        private Boolean blank;

        private String passphrase;
        @Json(name = "avoid_reuse")
        private Boolean avoidReuse;
        private boolean descriptors;
    }

    public BitcoindCreateWalletRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "createwallet";
    }

    @Override
    public boolean isResponseValid(BitcoindCreateOrLoadWalletResponse response) {
        return response.getResult() != null;
    }

    @Override
    public Class<BitcoindCreateOrLoadWalletResponse> getRpcResponseClass() {
        return BitcoindCreateOrLoadWalletResponse.class;
    }
}
