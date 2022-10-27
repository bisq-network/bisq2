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

import bisq.wallets.json_rpc.DaemonRpcCall;
import bisq.wallets.electrum.rpc.responses.ElectrumDeserializeResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class ElectrumDeserializeRpcCall
        extends DaemonRpcCall<ElectrumDeserializeRpcCall.Request, ElectrumDeserializeResponse> {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request {
        private final String tx;

        public Request(String tx) {
            this.tx = tx;
        }
    }

    public ElectrumDeserializeRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "deserialize";
    }

    @Override
    public boolean isResponseValid(ElectrumDeserializeResponse response) {
        return true;
    }

    @Override
    public Class<ElectrumDeserializeResponse> getRpcResponseClass() {
        return ElectrumDeserializeResponse.class;
    }
}
