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

import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultCreateOfferDraftMarketData implements CreateOfferDraftMarketData {
    private final MarketPriceService marketPriceService;

    public DefaultCreateOfferDraftMarketData(MarketPriceService marketPriceService) {
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
    }

    @Override
    public PriceQuote getMarketPriceQuote(Market market) {
        checkNotNull(market, "market must not be null");
        return marketPriceService.findMarketPriceQuote(market)
                .orElseThrow(() -> new IllegalStateException("Market price not available for " + market));
    }

    @Override
    public PriceQuote getBtcUsdPriceQuote() {
        return getMarketPriceQuote(MarketRepository.getUSDBitcoinMarket());
    }

    @Override
    public TradeAmount getTradeAmountFromUsd(Market market, Fiat usdAmount) {
        checkNotNull(market, "market must not be null");
        checkNotNull(usdAmount, "usdAmount must not be null");
        return MarketBasedAmountConversion.tradeAmountFromUsdAmount(marketPriceService, market, usdAmount);
    }
}
