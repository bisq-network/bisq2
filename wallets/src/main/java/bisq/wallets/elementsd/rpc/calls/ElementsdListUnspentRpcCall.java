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

package bisq.wallets.elementsd.rpc.calls;

import bisq.wallets.elementsd.rpc.responses.ElementsdListUnspentResponseEntry;
import bisq.wallets.rpc.call.WalletRpcCall;

public class ElementsdListUnspentRpcCall extends WalletRpcCall<Void, ElementsdListUnspentResponseEntry[]> {
    public ElementsdListUnspentRpcCall() {
        super(null);
    }

    @Override
    public String getRpcMethodName() {
        return "listunspent";
    }

    @Override
    public boolean isResponseValid(ElementsdListUnspentResponseEntry[] response) {
        return true;
    }

    @Override
    public Class<ElementsdListUnspentResponseEntry[]> getRpcResponseClass() {
        return ElementsdListUnspentResponseEntry[].class;
    }
}
