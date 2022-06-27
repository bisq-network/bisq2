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

import bisq.wallets.bitcoind.rpc.responses.BitcoindWarningResponse;
import bisq.wallets.core.rpc.call.DaemonRpcCall;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class BitcoindUnloadWalletRpcCall extends DaemonRpcCall<BitcoindUnloadWalletRpcCall.Request, BitcoindWarningResponse> {
 
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request {
        @JsonProperty("wallet_name")
        private final String walletName;

        public Request(@JsonProperty("wallet_name") String walletName) {
            this.walletName = walletName;
        }
    }

    public BitcoindUnloadWalletRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "unloadwallet";
    }

    @Override
    public boolean isResponseValid(BitcoindWarningResponse response) {
        return !response.hasWarning();
    }

    @Override
    public Class<BitcoindWarningResponse> getRpcResponseClass() {
        return BitcoindWarningResponse.class;
    }
}
