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

package bisq.offer.mu_sig.use_case.dependencies;

import bisq.common.market.Market;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultTakeOfferDraftCookieStore implements TakeOfferDraftCookieStore {
    private final SettingsService settingsService;

    public DefaultTakeOfferDraftCookieStore(SettingsService settingsService) {
        this.settingsService = checkNotNull(settingsService, "settingsService must not be null");
    }

    @Override
    public void persistUseBaseCurrencyForAmountInput(Market market, boolean useBaseCurrencyForAmountInput) {
        checkNotNull(market, "market must not be null");
        if (market.isBtcFiatMarket()) {
            settingsService.setCookie(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC, useBaseCurrencyForAmountInput);
        } else {
            settingsService.setCookie(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC, useBaseCurrencyForAmountInput);
        }
    }

    @Override
    public boolean getUseBaseCurrencyForAmountInput(Market market) {
        checkNotNull(market, "market must not be null");
        if (market.isBtcFiatMarket()) {
            return settingsService.getCookie()
                    .asBoolean(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(false);
        }
        return settingsService.getCookie()
                .asBoolean(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                .orElse(true);
    }

    private String geMarketSubKey(Market market) {
        return market.getMarketCodes();
    }
}
