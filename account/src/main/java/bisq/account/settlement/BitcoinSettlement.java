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

package bisq.account.settlement;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class BitcoinSettlement extends Settlement<BitcoinSettlement.Method>  {
    public static final BitcoinSettlement BTC_MAINCHAIN = new BitcoinSettlement(BitcoinSettlement.Method.BTC_MAINCHAIN);

    public enum Method implements Settlement.Method {
        BTC_MAINCHAIN,
        LN,
        LBTC,
        WBTC,
        OTHER
    }

    public BitcoinSettlement(Method method) {
        super(method);
    }

    public BitcoinSettlement(Method method, String name) {
        super(method, name);
    }

    public BitcoinSettlement(Method method, String name, Type type) {
        super(method, name, type);
    }

    public BitcoinSettlement(String name) {
        super(name);
    }

    @Override
    protected Method getDefaultMethod() {
        return Method.OTHER;
    }
}
