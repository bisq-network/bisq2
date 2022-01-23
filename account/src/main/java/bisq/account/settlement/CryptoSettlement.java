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
public class CryptoSettlement extends Settlement<CryptoSettlement.Method> {
    //todo maybe put Settlement.Type into SettlementOption?
    public static final CryptoSettlement NATIVE_CHAIN = new CryptoSettlement(Method.NATIVE_CHAIN);
    public static final CryptoSettlement XMR_AUTO_CONF = new CryptoSettlement(Method.NATIVE_CHAIN, "XMR auto-confirm", Type.AUTOMATIC);
    public static final CryptoSettlement XMR_MANUAL = new CryptoSettlement(Method.NATIVE_CHAIN, "XMR manual-confirm", Type.MANUAL);
    public static final CryptoSettlement ERC20 = new CryptoSettlement(Method.ERC20);
    public static final CryptoSettlement OTHER = new CryptoSettlement(Method.OTHER);

    public enum Method implements Settlement.Method {
        NATIVE_CHAIN,
        ERC20,
        OTHER
    }

    public CryptoSettlement(Method method) {
        super(method);
    }

    public CryptoSettlement(Method method, String name) {
        super(method, name);
    }

    public CryptoSettlement(Method method, String name, Settlement.Type type) {
        super(method, name, type);
    }

    public CryptoSettlement(String name) {
        super(name);
    }

    @Override
    protected Method getDefaultMethod() {
        return Method.OTHER;
    }
}
