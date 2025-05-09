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

import bisq.common.currency.TradeCurrency;
import bisq.common.currency.stable.StableCoinCurrency;
import bisq.common.currency.stable.StableCoinCurrencyRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public enum StablecoinPaymentRail implements NationalCurrencyPaymentRail {
    USDT_ERC20(StableCoinCurrencyRepository.USDT_ERC20),
    USDT_TRC20(StableCoinCurrencyRepository.USDT_TRC20),
    USDT_BEP20(StableCoinCurrencyRepository.USDT_BEP20),
    USDC_ERC20(StableCoinCurrencyRepository.USDC_ERC20),
    USDC_SPL(StableCoinCurrencyRepository.USDC_SPL),
    DAI_ERC20(StableCoinCurrencyRepository.DAI_ERC20),
    FDUSD_BEP20(StableCoinCurrencyRepository.FDUSD_BEP20),
    FDUSD_ERC20(StableCoinCurrencyRepository.FDUSD_ERC20),
    TUSD_ERC20(StableCoinCurrencyRepository.TUSD_ERC20),
    USDP_ERC20(StableCoinCurrencyRepository.USDP_ERC20),
    GUSD_ERC20(StableCoinCurrencyRepository.GUSD_ERC20);

    @Getter
    private final StableCoinCurrency stableCoinCurrency;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<TradeCurrency> tradeCurrencies;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<String> currencyCodes;

    StablecoinPaymentRail(StableCoinCurrency stableCoinCurrency) {
        this.stableCoinCurrency = stableCoinCurrency;
        tradeCurrencies = Collections.singletonList(stableCoinCurrency);
        currencyCodes = Collections.singletonList(stableCoinCurrency.getPegCurrencyCode());
    }

    public boolean supportsCurrency(String currencyCode) {
        return currencyCodes.contains(currencyCode);
    }
}


