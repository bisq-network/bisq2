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

package bisq.common.currency.stable;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

public class StableCoinCurrencyRepository {
    public static final StableCoinCurrency USDT_ERC20 = new StableCoinCurrency("USDT",
            "Tether USD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.TETHER);

    public static final StableCoinCurrency USDT_TRC20 = new StableCoinCurrency("USDT",
            "Tether USD (Tron TRC-20)",
            "USD",
            StableCoinChain.TRON,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.TETHER);

    public static final StableCoinCurrency USDT_BEP20 = new StableCoinCurrency("USDT",
            "Tether USD (BNB Smart Chain BEP-20)",
            "USD",
            StableCoinChain.BNB_SMART_CHAIN,
            StableCoinTokenStandard.BEP20,
            StableCoinIssuer.TETHER);

    public static final StableCoinCurrency USDC_ERC20 = new StableCoinCurrency("USDC",
            "USD Coin (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.CIRCLE);

    public static final StableCoinCurrency USDC_SPL = new StableCoinCurrency("USDC",
            "USD Coin (Solana SPL)",
            "USD",
            StableCoinChain.SOLANA,
            StableCoinTokenStandard.SPL,
            StableCoinIssuer.CIRCLE);

    public static final StableCoinCurrency DAI_ERC20 = new StableCoinCurrency("DAI",
            "Dai (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.MAKERDAO);

    public static final StableCoinCurrency FDUSD_BEP20 = new StableCoinCurrency("FDUSD",
            "First Digital USD (BNB Smart Chain BEP-20)",
            "USD",
            StableCoinChain.BNB_SMART_CHAIN,
            StableCoinTokenStandard.BEP20,
            StableCoinIssuer.FIRST_DIGITAL);

    public static final StableCoinCurrency FDUSD_ERC20 = new StableCoinCurrency("FDUSD",
            "First Digital USD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.FIRST_DIGITAL);

    public static final StableCoinCurrency TUSD_ERC20 = new StableCoinCurrency("TUSD",
            "TrueUSD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.TECHTERYX);

    public static final StableCoinCurrency USDP_ERC20 = new StableCoinCurrency("USDP",
            "Pax Dollar (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.PAXOS);

    public static final StableCoinCurrency GUSD_ERC20 = new StableCoinCurrency("GUSD",
            "Gemini Dollar (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.GEMINI);

    @Getter
    private static final Map<String, Set<StableCoinCurrency>> STABLE_COIN_CURRENCY_SET_BY_CODE = Map.of(
            "USDT", Set.of(USDT_ERC20, USDT_TRC20, USDT_BEP20),
            "USDC", Set.of(USDC_ERC20, USDC_SPL),
            "DAI", Set.of(DAI_ERC20),
            "FDUSD", Set.of(FDUSD_ERC20, FDUSD_BEP20),
            "TUSD", Set.of(TUSD_ERC20),
            "USDP", Set.of(USDP_ERC20),
            "GUSD", Set.of(GUSD_ERC20)
    );
}
