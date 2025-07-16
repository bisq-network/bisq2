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

package bisq.common.asset.stable;

import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StableCoinRepository {
    public static final StableCoin USDT_ERC20 = new StableCoin("USDT",
            "Tether USD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.TETHER);

    public static final StableCoin USDT_TRC20 = new StableCoin("USDT",
            "Tether USD (Tron TRC-20)",
            "USD",
            StableCoinChain.TRON,
            StableCoinTokenStandard.TRC20,
            StableCoinIssuer.TETHER);

    public static final StableCoin USDT_BEP20 = new StableCoin("USDT",
            "Tether USD (BNB Smart Chain BEP-20)",
            "USD",
            StableCoinChain.BNB_SMART_CHAIN,
            StableCoinTokenStandard.BEP20,
            StableCoinIssuer.TETHER);

    public static final StableCoin USDC_ERC20 = new StableCoin("USDC",
            "USD Coin (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.CIRCLE);

    public static final StableCoin USDC_SPL = new StableCoin("USDC",
            "USD Coin (Solana SPL)",
            "USD",
            StableCoinChain.SOLANA,
            StableCoinTokenStandard.SPL,
            StableCoinIssuer.CIRCLE);

    public static final StableCoin DAI_ERC20 = new StableCoin("DAI",
            "Dai (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.MAKERDAO);

    public static final StableCoin FDUSD_BEP20 = new StableCoin("FDUSD",
            "First Digital USD (BNB Smart Chain BEP-20)",
            "USD",
            StableCoinChain.BNB_SMART_CHAIN,
            StableCoinTokenStandard.BEP20,
            StableCoinIssuer.FIRST_DIGITAL);

    public static final StableCoin FDUSD_ERC20 = new StableCoin("FDUSD",
            "First Digital USD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.FIRST_DIGITAL);

    public static final StableCoin TUSD_ERC20 = new StableCoin("TUSD",
            "TrueUSD (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.TECHTERYX);

    public static final StableCoin USDP_ERC20 = new StableCoin("USDP",
            "Pax Dollar (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.PAXOS);

    public static final StableCoin GUSD_ERC20 = new StableCoin("GUSD",
            "Gemini Dollar (Ethereum ERC-20)",
            "USD",
            StableCoinChain.ETHEREUM,
            StableCoinTokenStandard.ERC20,
            StableCoinIssuer.GEMINI);

    @Getter
    private static final Map<String, Set<StableCoin>> STABLE_COIN_CURRENCY_SET_BY_CODE = Map.of(
            "USDT", Set.of(USDT_ERC20, USDT_TRC20, USDT_BEP20),
            "USDC", Set.of(USDC_ERC20, USDC_SPL),
            "DAI", Set.of(DAI_ERC20),
            "FDUSD", Set.of(FDUSD_ERC20, FDUSD_BEP20),
            "TUSD", Set.of(TUSD_ERC20),
            "USDP", Set.of(USDP_ERC20),
            "GUSD", Set.of(GUSD_ERC20)
    );

    public static Set<StableCoin> allWithPegCurrencyCode(String pegCurrencyCode) {
        return STABLE_COIN_CURRENCY_SET_BY_CODE.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getPegCurrencyCode().equals(pegCurrencyCode))
                .collect(Collectors.toSet());
    }

    public static Set<StableCoin> allWithCode(String code) {
        return STABLE_COIN_CURRENCY_SET_BY_CODE.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getCode().equals(code))
                .collect(Collectors.toSet());
    }
}
