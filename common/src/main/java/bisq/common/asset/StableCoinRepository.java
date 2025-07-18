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

package bisq.common.asset;

import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StableCoinRepository {
    public static final StableCoin USDT_ERC20 = new StableCoin("USDT",
            "Tether USD",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.TETHER);

    public static final StableCoin USDT_TRC20 = new StableCoin("USDT",
            "Tether USD",
            "USD",
            StableCoin.Network.TRON,
            StableCoin.TokenStandard.TRC20,
            StableCoin.Issuer.TETHER);

    public static final StableCoin USDT_BEP20 = new StableCoin("USDT",
            "Tether USD",
            "USD",
            StableCoin.Network.BNB_SMART_CHAIN,
            StableCoin.TokenStandard.BEP20,
            StableCoin.Issuer.TETHER);

    public static final StableCoin USDC_ERC20 = new StableCoin("USDC",
            "USD Coin",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.CIRCLE);

    public static final StableCoin USDC_SPL = new StableCoin("USDC",
            "USD Coin",
            "USD",
            StableCoin.Network.SOLANA,
            StableCoin.TokenStandard.SPL,
            StableCoin.Issuer.CIRCLE);

    public static final StableCoin DAI_ERC20 = new StableCoin("DAI",
            "Dai",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.MAKERDAO);

    public static final StableCoin FDUSD_BEP20 = new StableCoin("FDUSD",
            "First Digital USD",
            "USD",
            StableCoin.Network.BNB_SMART_CHAIN,
            StableCoin.TokenStandard.BEP20,
            StableCoin.Issuer.FIRST_DIGITAL);

    public static final StableCoin FDUSD_ERC20 = new StableCoin("FDUSD",
            "First Digital USD",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.FIRST_DIGITAL);

    public static final StableCoin TUSD_ERC20 = new StableCoin("TUSD",
            "TrueUSD",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.TECHTERYX);

    public static final StableCoin USDP_ERC20 = new StableCoin("USDP",
            "Pax Dollar",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.PAXOS);

    public static final StableCoin GUSD_ERC20 = new StableCoin("GUSD",
            "Gemini Dollar",
            "USD",
            StableCoin.Network.ETHEREUM,
            StableCoin.TokenStandard.ERC20,
            StableCoin.Issuer.GEMINI);

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

    public static Optional<StableCoin> find(String code) {
        return STABLE_COIN_CURRENCY_SET_BY_CODE.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getCode().equals(code))
                .findAny();
    }

    public static List<StableCoin> getMajorStableCoins() {
        return List.of(USDT_ERC20, USDT_TRC20, USDC_ERC20, DAI_ERC20);
    }

    public static Set<StableCoin> allWithPegCurrencyCode(String pegCurrencyCode) {
        return STABLE_COIN_CURRENCY_SET_BY_CODE.values().stream()
                .flatMap(Collection::stream)
                .filter(coin -> coin.getPegCurrencyCode().equals(pegCurrencyCode))
                .collect(Collectors.toSet());
    }

    public static Set<StableCoin> allWithCode(String code) {
        return STABLE_COIN_CURRENCY_SET_BY_CODE.values().stream()
                .flatMap(Collection::stream)
                .filter(coin -> coin.getCode().equals(code))
                .collect(Collectors.toSet());
    }
}
