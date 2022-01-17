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
    public enum Method implements Settlement.Method {
        MAINNET,            // If coin is transferred via native mainnet chain. E.g. Bitcoin network
        L2,                 // Layer 2, e.g. Lightning
        SIDE_CHAIN,         // Side chain, e.g. Liquid, RSK
        WRAPPED,            // Wrapped coin in a token (e.g WBTC as ERC20 on ETH)
        MULTI_HOSTED,       // Multiple chains can be used. E.g. USDT using BTC/Omni, ETH/ERC20,...
        OTHER               // Anything else
    }

    public CryptoSettlement(Method type) {
        super(type);
    }

    public CryptoSettlement(Method type, String name) {
        super(type, name);
    }

    public CryptoSettlement(String name) {
        super(name);
    }

    @Override
    protected Method getDefaultType() {
        return Method.OTHER;
    }
}
