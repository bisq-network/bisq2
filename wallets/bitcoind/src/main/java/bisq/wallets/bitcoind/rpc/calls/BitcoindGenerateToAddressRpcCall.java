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

import bisq.wallets.json_rpc.DaemonRpcCall;
import bisq.wallets.json_rpc.reponses.JsonRpcStringListResponse;
import lombok.Builder;
import lombok.Getter;

public class BitcoindGenerateToAddressRpcCall extends DaemonRpcCall<BitcoindGenerateToAddressRpcCall.Request, JsonRpcStringListResponse> {

    @Builder
    @Getter
    public static class Request {
        private final int nblocks;
        private final String address;
    }

    public BitcoindGenerateToAddressRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "generatetoaddress";
    }

    @Override
    public boolean isResponseValid(JsonRpcStringListResponse response) {
        return true;
    }

    @Override
    public Class<JsonRpcStringListResponse> getRpcResponseClass() {
        return JsonRpcStringListResponse.class;
    }
}
