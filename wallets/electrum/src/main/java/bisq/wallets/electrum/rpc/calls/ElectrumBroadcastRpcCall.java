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

public class ElectrumBroadcastRpcCall extends DaemonRpcCall<ElectrumBroadcastRpcCall.Request, String> {
    public record Request(String tx) {
    }

    public ElectrumBroadcastRpcCall(Request request) {
        super(request);
    }

    @Override
    public String getRpcMethodName() {
        return "broadcast";
    }

    @Override
    public boolean isResponseValid(String response) {
        return !response.isEmpty();
    }

    @Override
    public Class<String> getRpcResponseClass() {
        return String.class;
    }
}
