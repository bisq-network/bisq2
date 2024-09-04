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
import bisq.wallets.bitcoind.rpc.responses.BitcoindImportDescriptorResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;
import lombok.Getter;
import lombok.Setter;

public class BitcoindImportDescriptorsRpcCall
        extends DaemonRpcCall<BitcoindImportDescriptorsRpcCall.Request, BitcoindImportDescriptorResponse> {

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
    public boolean isResponseValid(BitcoindImportDescriptorResponse response) {
        return response.getResult().stream().allMatch(BitcoindImportDescriptorResponse.Entry::isSuccess);
    }

    @Override
    public Class<BitcoindImportDescriptorResponse> getRpcResponseClass() {
        return BitcoindImportDescriptorResponse.class;
    }
}
