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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
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

    public static String getFormattedPrice(PriceSpec priceSpec, MarketPriceService marketPriceService) {
        String priceInfo = Res.get("data.na");
        if (priceSpec instanceof FixPriceSpec fixPriceSpec) {
            return PriceFormatter.format(fixPriceSpec.getPriceQuote());
        }

        Market selectedMarket = marketPriceService.getSelectedMarket().get();
        if (selectedMarket == null) {
            log.warn("No market price selected");
            return Res.get("data.na");
        }

        //  marketPrice.getMarket().getMarketCodes();
        Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(selectedMarket);
        if (marketPrice.isEmpty()) {
            log.warn("No market price available for selected market {}", selectedMarket);
            return Res.get("data.na");
        }

        if (priceSpec instanceof FloatPriceSpec floatPriceSpec) {
            long quotePriceValue = marketPrice.map(p -> p.getPriceQuote().getValue()).orElse(0L);
            double percentage = floatPriceSpec.getPercentage();
            String currentPrice = marketPrice.map(MarketPrice::getPriceQuote)
                    .map(priceQuote -> PriceUtil.fromMarketPriceMarkup(priceQuote, percentage))
                    .map(priceQuote -> PriceFormatter.format(priceQuote, true))
                    .orElse(Res.get("data.na"));

            String percent = PercentageFormatter.formatToPercentWithSymbol(Math.abs(percentage));
            return currentPrice + " (" + Res.get(percentage >= 0
                    ? "bisqEasy.tradeWizard.review.chatMessage.floatPrice.plus"
                    : "bisqEasy.tradeWizard.review.chatMessage.floatPrice.minus", percent) + ")";
        }
        return PriceFormatter.format(marketPrice.get().getPriceQuote(), true);

    }

    public static String getFormattedPriceSpecWithOfferPrice(PriceSpec priceSpec, String offerPrice) {
        if (priceSpec instanceof FixPriceSpec fixPriceSpec) {
            String price = PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
            return Res.get("priceSpecFormatter.fixPrice", price);
        }
        if (priceSpec instanceof FloatPriceSpec floatPriceSpec) {
            String percent = PercentageFormatter.formatToPercentWithSymbol(Math.abs(floatPriceSpec.getPercentage()));
            return Res.get(floatPriceSpec.getPercentage() >= 0
                            ? "priceSpecFormatter.floatPrice.above"
                            : "priceSpecFormatter.floatPrice.below",
                    percent,
                    offerPrice);
        }
        // market price
        return Res.get("priceSpecFormatter.marketPrice", offerPrice);
    }
}
