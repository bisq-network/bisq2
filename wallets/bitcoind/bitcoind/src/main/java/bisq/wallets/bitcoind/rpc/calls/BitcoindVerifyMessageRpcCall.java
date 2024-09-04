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
import bisq.wallets.json_rpc.reponses.JsonRpcBooleanResponse;
import lombok.Builder;
import lombok.Getter;

public class BitcoindVerifyMessageRpcCall extends DaemonRpcCall<BitcoindVerifyMessageRpcCall.Request, JsonRpcBooleanResponse> {
    @Builder
    @Getter
    public static class Request {
        private final String address;
        private final String signature;
        private final String message;
    }

    public BitcoindVerifyMessageRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "verifymessage";
    }

    @Override
    public boolean isResponseValid(JsonRpcBooleanResponse response) {
        return true;
    }

    @Override
    public Class<JsonRpcBooleanResponse> getRpcResponseClass() {
        return JsonRpcBooleanResponse.class;
    }
}
