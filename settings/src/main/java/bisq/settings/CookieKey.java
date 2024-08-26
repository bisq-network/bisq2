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

package bisq.settings;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum CookieKey {
    STAGE_X,
    STAGE_Y,
    STAGE_W,
    STAGE_H,
    NAVIGATION_TARGET,
    FILE_CHOOSER_DIR,
    CREATE_OFFER_METHODS(true),
    CREATE_OFFER_USE_FIX_PRICE(true),
    BONDED_ROLES_EXPANDED,
    CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED,
    IGNORE_VERSION(true),
    NOTIFY_FOR_PRE_RELEASE,
    BISQ_EASY_VIDEO_OPENED,
    MENU_HORIZONTAL_EXPANDED,
    SHOW_NETWORK_BOOTSTRAP_DETAILS,
    PERMIT_OPENING_BROWSER,
    USE_TRANSIENT_NOTIFICATIONS,
    MARKETS_FILTER,
    MARKET_SORT_TYPE,
    SELECTED_MARKET_CODES,
    CREATE_OFFER_BITCOIN_METHODS,
    TAKE_OFFER_SELECTED_BITCOIN_METHOD,
    TAKE_OFFER_SELECTED_FIAT_METHOD(true),
    BISQ_EASY_OFFER_LIST_PAYMENT_FILTERS(true),
    BISQ_EASY_OFFER_LIST_CUSTOM_PAYMENT_FILTER(true);

    @Getter
    private final boolean useSubKey;

    CookieKey(boolean useSubKey) {
        this.useSubKey = useSubKey;
    }

    CookieKey() {
        this(false);
    }
}
