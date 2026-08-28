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

package bisq.desktop.common.utils;

import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeRestrictedException;

public final class TradeExceptionHandler {
    private TradeExceptionHandler() {
    }

    public static boolean run(Runnable tradeAction) {
        try {
            tradeAction.run();
            return true;
        } catch (TradeRestrictedException e) {
            new Popup().warning(localizedMessage(e)).show();
            return false;
        }
    }

    /**
     * The exception carries a stable English message for API clients; the desktop popup maps the
     * structured restriction back to the user's language instead of showing that raw text.
     */
    public static String localizedMessage(TradeRestrictedException e) {
        return switch (e.getRestriction()) {
            case HALT_TRADING -> Res.get("trade.error.tradingHalted");
            case MIN_VERSION_REQUIRED -> Res.get("trade.error.minVersionRequired", e.findMinRequiredVersion().orElse(""));
        };
    }
}
