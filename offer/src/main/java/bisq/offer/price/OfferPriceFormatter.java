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

package bisq.offer.price;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.monetary.PriceQuote;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.presentation.formatters.PriceFormatter;

import java.util.function.Function;

public class OfferPriceFormatter {
    public static String formatQuote(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuote(marketPriceService, offer, true);
    }

    public static String formatQuote(MarketPriceService marketPriceService, Offer<?, ?> offer, boolean showCode) {
        return PriceUtil.findQuote(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("data.na"));
    }

    private static Function<PriceQuote, String> getFormatFunction(boolean showCode) {
        return showCode ? PriceFormatter::formatWithCode : PriceFormatter::format;
    }
}