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

package bisq.account.payment_method;

import bisq.common.asset.Asset;
import bisq.common.asset.StableCoin;
import bisq.common.asset.StableCoinRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public enum StableCoinPaymentRail implements NationalCurrencyPaymentRail {
    USDT_ERC20(StableCoinRepository.USDT_ERC20),
    USDT_TRC20(StableCoinRepository.USDT_TRC20),
    USDT_BEP20(StableCoinRepository.USDT_BEP20),
    USDC_ERC20(StableCoinRepository.USDC_ERC20),
    USDC_SPL(StableCoinRepository.USDC_SPL),
    DAI_ERC20(StableCoinRepository.DAI_ERC20),
    FDUSD_BEP20(StableCoinRepository.FDUSD_BEP20),
    FDUSD_ERC20(StableCoinRepository.FDUSD_ERC20),
    TUSD_ERC20(StableCoinRepository.TUSD_ERC20),
    USDP_ERC20(StableCoinRepository.USDP_ERC20),
    GUSD_ERC20(StableCoinRepository.GUSD_ERC20);

    @Getter
    private final StableCoin stableCoin;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<Asset> tradeCurrencies;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<String> currencyCodes;

    StableCoinPaymentRail(StableCoin stableCoin) {
        this.stableCoin = stableCoin;
        tradeCurrencies = Collections.singletonList(stableCoin);
        currencyCodes = Collections.singletonList(stableCoin.getPegCurrencyCode());
    }

    public boolean supportsCurrency(String currencyCode) {
        return currencyCodes.contains(currencyCode);
    }
}


