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
import bisq.wallets.electrum.rpc.responses.ElectrumPasswordResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class ElectrumPasswordRpcCall extends DaemonRpcCall<ElectrumPasswordRpcCall.Request, ElectrumPasswordResponse> {
    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request {
        private final String password;
        @JsonProperty("new_password")
        private final String newPassword;

        public Request(String password, @JsonProperty("new_password") String newPassword) {
            this.password = password;
            this.newPassword = newPassword;
        }

        public String getPassword() {
            return password;
        }
    }

    public ElectrumPasswordRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "password";
    }

    @Override
    public boolean isResponseValid(ElectrumPasswordResponse response) {
        return response.isPassword();
    }

    @Override
    public Class<ElectrumPasswordResponse> getRpcResponseClass() {
        return ElectrumPasswordResponse.class;
    }
}
