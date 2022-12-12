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

import bisq.wallets.bitcoind.rpc.responses.BitcoindListTransactionsResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;
import lombok.Builder;
import lombok.Getter;

public class BitcoindListTransactionsRpcCall
        extends DaemonRpcCall<BitcoindListTransactionsRpcCall.Request, BitcoindListTransactionsResponse> {
    @Builder
    @Getter
    public static class Request {
        private final String label = "*";
        private final int count;
    }

    public BitcoindListTransactionsRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "listtransactions";
    }

    @Override
    public boolean isResponseValid(BitcoindListTransactionsResponse response) {
        return true;
    }

    @Override
    public Class<BitcoindListTransactionsResponse> getRpcResponseClass() {
        return BitcoindListTransactionsResponse.class;
    }
}
