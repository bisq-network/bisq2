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

package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.PaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Computes effective trade-amount constraints for the current draft context.
 * <p>
 * Design: converts market, pricing, direction, and payment-rail inputs into a single immutable
 * {@link TradeAmountConstraints} result so callers do not duplicate protocol limit logic.
 */
class TradeAmountConstraintsService {
    private final CreateOfferDraftMarketData marketData;

    TradeAmountConstraintsService(CreateOfferDraftMarketData marketData) {
        this.marketData = checkNotNull(marketData, "marketData must not be null");
    }

    /* --------------------------------------------------------------------- */
    // Constraint computation
    /* --------------------------------------------------------------------- */

    TradeAmountConstraints compute(Market market,
                                          Direction direction,
                                          PriceQuote offerPriceQuote,
                                          PriceQuote marketPriceQuote,
                                          PaymentRail paymentRail) {
        checkNotNull(market, "market must not be null");
        checkNotNull(direction, "direction must not be null");
        checkNotNull(offerPriceQuote, "offerPriceQuote must not be null");
        checkNotNull(marketPriceQuote, "marketPriceQuote must not be null");

        PriceQuote btcUsdPriceQuote = marketData.getBtcUsdPriceQuote();

        Fiat maxTradeLimitInUsd = TradeAmountLimits.getMaxTradeLimitInUsd(paymentRail);
        TradeAmountRange tradeAmountLimits = TradeAmountLimits.toTradeAmountLimits(market,
                offerPriceQuote,
                btcUsdPriceQuote,
                marketPriceQuote,
                TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD,
                maxTradeLimitInUsd);

        Optional<TradeAmount> userSpecificTradeAmountLimit = TradeAmountLimits.toUserSpecificTradeAmountLimit(direction,
                market,
                offerPriceQuote,
                btcUsdPriceQuote,
                marketPriceQuote,
                TradeAmountLimits.getUserSpecificLimitInUsdAmount());
        return new TradeAmountConstraints(tradeAmountLimits, userSpecificTradeAmountLimit);
    }
}
