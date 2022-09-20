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

import bisq.wallets.bitcoind.rpc.calls.requests.BitcoindImportMultiRequest;
import bisq.wallets.bitcoind.rpc.responses.BitcoinImportMultiEntryResponse;
import bisq.wallets.core.rpc.call.WalletRpcCall;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class BitcoindImportMultiRpcCall
        extends WalletRpcCall<BitcoindImportMultiRpcCall.Request, BitcoinImportMultiEntryResponse[]> {
    @Builder
    @Getter
    public static class Request {
        private List<BitcoindImportMultiRequest> requests;
        private final Map<String, Boolean> options = Map.of("rescan", false);
    }

    public BitcoindImportMultiRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "importmulti";
    }

    @Override
    public boolean isResponseValid(BitcoinImportMultiEntryResponse[] response) {
        return true;
    }

    @Override
    public Class<BitcoinImportMultiEntryResponse[]> getRpcResponseClass() {
        return BitcoinImportMultiEntryResponse[].class;
    }
}
