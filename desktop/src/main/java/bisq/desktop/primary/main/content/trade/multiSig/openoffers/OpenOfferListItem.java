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

package bisq.desktop.primary.main.content.trade.multiSig.openoffers;

import bisq.common.currency.TradeCurrency;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.offer.OpenOffer;
import bisq.offer.spec.SettlementSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OpenOfferListItem implements TableItem {
    @EqualsAndHashCode.Include
    private final String id;
    private final OpenOffer openOffer;
    private final Offer offer;
    private final String market;
    private final MarketPriceService marketPriceService;
    private final String price;
    private final String baseAmount;
    private final String quoteAmount;
    private final String settlement;
    private final String options;

    OpenOfferListItem(OpenOffer openOffer, MarketPriceService marketPriceService) {
        this.openOffer = openOffer;
        this.offer = openOffer.getOffer();
        this.marketPriceService = marketPriceService;

        id = offer.getId();
        market = offer.getMarket().toString();
        baseAmount = AmountFormatter.formatAmount(offer.getBaseAmountAsMonetary());
        quoteAmount = AmountFormatter.formatAmount(offer.getQuoteAmountAsMonetary(marketPriceService));
        price = QuoteFormatter.format(offer.getQuote(marketPriceService));

        String baseSideSettlement = offer.getBaseSideSettlementSpecs().stream()
                .map(SettlementSpec::getSettlementMethodName)
                .map(settlementMethodName -> Res.get(settlementMethodName))
                .collect(Collectors.joining("\n"));
        String quoteSideSettlement = offer.getQuoteSideSettlementSpecs().stream()
                .map(SettlementSpec::getSettlementMethodName)
                .map(settlementMethodName -> Res.get(settlementMethodName))
                .collect(Collectors.joining("\n"));

        String baseCurrencyCode = offer.getMarket().getBaseCurrencyCode();
        String quoteCurrencyCode = offer.getMarket().getQuoteCurrencyCode();

        boolean isBaseCurrencyFiat = TradeCurrency.isFiat(baseCurrencyCode);
        boolean isQuoteCurrencyFiat = TradeCurrency.isFiat(quoteCurrencyCode);

        boolean isBaseSideFiatOrMultiple = isBaseCurrencyFiat || offer.getBaseSideSettlementSpecs().size() > 1;
        boolean isQuoteSideFiatOrMultiple = isQuoteCurrencyFiat || offer.getQuoteSideSettlementSpecs().size() > 1;
        if (isBaseSideFiatOrMultiple && !isQuoteSideFiatOrMultiple) {
            settlement = baseSideSettlement;
        } else if (isQuoteSideFiatOrMultiple && !isBaseSideFiatOrMultiple) {
            settlement = quoteSideSettlement;
        } else if (isBaseSideFiatOrMultiple) {
            // both
            settlement = Res.get("offerbook.table.settlement.multi",
                    baseCurrencyCode, baseSideSettlement, quoteCurrencyCode, quoteSideSettlement);
        } else {
            // none (both are using non fiat mandatory settlement method 
            settlement = "";
        }

        options = ""; //todo
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
