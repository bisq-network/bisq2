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

package bisq.offer.mu_sig.draft.dependencies;

import bisq.common.market.Market;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultCreateOfferDraftCookieStore implements CreateOfferDraftCookieStore {
    private final SettingsService settingsService;

    public DefaultCreateOfferDraftCookieStore(SettingsService settingsService) {
        this.settingsService = checkNotNull(settingsService, "settingsService must not be null");
    }

    @Override
    public void persistDirection(Direction direction) {
        checkNotNull(direction, "direction must not be null");
        settingsService.setCookie(CookieKey.MU_SIG_CREATE_OFFER_USE_BUY_DIRECTION, direction.isBuy());
    }

    @Override
    public Direction getDirection() {
        boolean useBuyDirection = settingsService.getCookie()
                .asBoolean(CookieKey.MU_SIG_CREATE_OFFER_USE_BUY_DIRECTION)
                .orElse(false);
        return useBuyDirection ? Direction.BUY : Direction.SELL;
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


    @Override
    public boolean getUseRangeAmount() {
        return settingsService.getCookie()
                .asBoolean(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED)
                .orElse(false);
    }

    @Override
    public void persistUseRangeAmount(boolean useRangeAmount) {
        settingsService.setCookie(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED, useRangeAmount);
    }
}
