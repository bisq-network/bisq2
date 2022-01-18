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

package bisq.account;

public class CryptoSettlement extends Settlement<CryptoSettlement.Method> {
    public static final CryptoSettlement MAINNET = new CryptoSettlement(Method.MAINNET);
    public static final CryptoSettlement XMR_AUTO_CONF = new CryptoSettlement(Method.MAINNET, "XMR auto-confirm", Settlement.Type.AUTOMATIC);
    public static final CryptoSettlement XMR_MANUAL = new CryptoSettlement(Method.MAINNET, "XMR manual-confirm", Type.MANUAL);
    public static final CryptoSettlement L2 = new CryptoSettlement(Method.L2);
    public static final CryptoSettlement SIDE_CHAIN = new CryptoSettlement(Method.SIDE_CHAIN);
    public static final CryptoSettlement WRAPPED = new CryptoSettlement(Method.WRAPPED);
    public static final CryptoSettlement MULTI_CHAIN = new CryptoSettlement(Method.MULTI_CHAIN);
    public static final CryptoSettlement OTHER = new CryptoSettlement(Method.OTHER);

    public enum Method implements SettlementMethod {
        MAINNET,            // If coin is transferred via native mainnet chain. E.g. Bitcoin network
        L2,                 // Layer 2, e.g. Lightning
        SIDE_CHAIN,         // Side chain, e.g. Liquid, RSK
        WRAPPED,            // Wrapped coin in a token (e.g WBTC as ERC20 on ETH)
        MULTI_CHAIN,        // Multiple chains can be used. E.g. USDT using BTC/Omni, ETH/ERC20,...
        OTHER               // Anything else
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
