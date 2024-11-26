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

package bisq.offer.price.spec;

import bisq.i18n.Res;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;

public class PriceSpecFormatter {
    public static String getFormattedPriceSpec(PriceSpec priceSpec) {
        return getFormattedPriceSpec(priceSpec, false);
    }

    public static String getFormattedPriceSpec(PriceSpec priceSpec, boolean abbreviated) {
        String priceInfo;
        if (priceSpec instanceof FixPriceSpec fixPriceSpec) {
            String price = PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
            priceInfo = Res.get("bisqEasy.tradeWizard.review.chatMessage.fixPrice", price);
        } else if (priceSpec instanceof FloatPriceSpec floatPriceSpec) {
            String percent = PercentageFormatter.formatToPercentWithSymbol(Math.abs(floatPriceSpec.getPercentage()));
            priceInfo = Res.get(floatPriceSpec.getPercentage() >= 0
                    ? abbreviated
                        ? "bisqEasy.tradeWizard.review.chatMessage.floatPrice.plus"
                        : "bisqEasy.tradeWizard.review.chatMessage.floatPrice.above"
                    : abbreviated
                        ? "bisqEasy.tradeWizard.review.chatMessage.floatPrice.minus"
                        : "bisqEasy.tradeWizard.review.chatMessage.floatPrice.below"
                    , percent);
        } else {
            priceInfo = Res.get("bisqEasy.tradeWizard.review.chatMessage.marketPrice");
        }
        return priceInfo;
    }
}
