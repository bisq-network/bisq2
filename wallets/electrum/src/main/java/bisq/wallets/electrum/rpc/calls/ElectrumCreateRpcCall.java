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

import bisq.wallets.core.rpc.call.DaemonRpcCall;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class ElectrumCreateRpcCall extends DaemonRpcCall<ElectrumCreateRpcCall.Request, ElectrumCreateResponse> {

    private static final String SUCCESS_MSG = "Please keep your seed in a safe place;" +
            " if you lose it, you will not be able to restore your wallet.";

    @Getter
    public static class Request {
        private final String passphrase = "";
        @JsonProperty("encrypt_file")
        private final String encryptFile = "true";
        @JsonProperty("seed_type")
        private final String seedType = "segwit";

        private final String password;

        public Request(String password) {
            this.password = password;
        }
    }

    public ElectrumCreateRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "create";
    }

    @Override
    public boolean isResponseValid(ElectrumCreateResponse response) {
        return response.getMsg().equals(SUCCESS_MSG);
    }

    @Override
    public Class<ElectrumCreateResponse> getRpcResponseClass() {
        return ElectrumCreateResponse.class;
    }
}
