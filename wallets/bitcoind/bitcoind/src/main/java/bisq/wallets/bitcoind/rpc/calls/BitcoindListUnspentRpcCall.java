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

import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponse;
import bisq.wallets.json_rpc.DaemonRpcCall;

public class BitcoindListUnspentRpcCall extends DaemonRpcCall<Void, BitcoindListUnspentResponse> {
    public BitcoindListUnspentRpcCall() {
        super(null);
    }

    @Override
    public String getRpcMethodName() {
        return "listunspent";
    }

    @Override
    public boolean isResponseValid(BitcoindListUnspentResponse response) {
        return true;
    }

    @Override
    public Class<BitcoindListUnspentResponse> getRpcResponseClass() {
        return BitcoindListUnspentResponse.class;
    }
}
