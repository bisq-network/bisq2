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

package bisq.wallets.bitcoind.rpc.psbt;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class BitcoindPsbtOutput {

    private final Map<String, Double> amountByAddress = new HashMap<>();

    @Setter
    private String data = "00";

    public void addOutput(String address, double amount) {
        amountByAddress.put(address, amount);
    }

    public Object[] toPsbtOutputObject() {
        var dataMap = new HashMap<>();
        dataMap.put("data", data);
        return new Object[]{amountByAddress, dataMap};
    }
}
