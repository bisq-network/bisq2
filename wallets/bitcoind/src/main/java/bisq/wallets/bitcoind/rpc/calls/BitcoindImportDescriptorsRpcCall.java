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

import bisq.wallets.bitcoind.rpc.calls.requests.BitcoindImportDescriptorRequestEntry;
import bisq.wallets.bitcoind.rpc.responses.BitcoindImportDescriptorResponseEntry;
import bisq.wallets.core.rpc.call.WalletRpcCall;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class BitcoindImportDescriptorsRpcCall
        extends WalletRpcCall<BitcoindImportDescriptorsRpcCall.Request, BitcoindImportDescriptorResponseEntry[]> {

    @Getter
    @Setter
    public static class Request {
        private final BitcoindImportDescriptorRequestEntry[] requests;

        public Request(BitcoindImportDescriptorRequestEntry[] requests) {
            this.requests = requests;
        }
    }

    public BitcoindImportDescriptorsRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "importdescriptors";
    }

    @Override
    public boolean isResponseValid(BitcoindImportDescriptorResponseEntry[] response) {
        return Arrays.stream(response).allMatch(BitcoindImportDescriptorResponseEntry::isSuccess);
    }

    @Override
    public Class<BitcoindImportDescriptorResponseEntry[]> getRpcResponseClass() {
        return BitcoindImportDescriptorResponseEntry[].class;
    }
}
