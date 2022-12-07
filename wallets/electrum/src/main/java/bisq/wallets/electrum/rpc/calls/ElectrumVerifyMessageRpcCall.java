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

package bisq.wallets.electrum.rpc.calls;

import bisq.wallets.json_rpc.reponses.JsonRpcBooleanResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class ElectrumVerifyMessageRpcCall extends DaemonRpcCall<ElectrumVerifyMessageRpcCall.Request, JsonRpcBooleanResponse> {
    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request {
        private final String address;
        private final String signature;
        private final String message;

        public Request(String address, String signature, String message) {
            this.address = address;
            this.signature = signature;
            this.message = message;
        }
    }

    public ElectrumVerifyMessageRpcCall(Request request) {
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
