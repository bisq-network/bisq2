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

package bisq.presentation.offer;

import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.offer.MarketPrice;
import bisq.offer.SwapOffer;
import bisq.offer.options.TransferOption;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Enriched offer object which carries the dynamic data as well as formatted strings for presentation.
 */
@Getter
@Slf4j
public class OfferEntity implements Comparable<OfferEntity> {
    protected final SwapOffer offer;
    private Quote quote;
    private Monetary quoteAmount;
    protected final String formattedBaseAmountWithMinAmount;


    protected final String formattedTransferOptions;
    protected String formattedQuote;
    protected String formattedMarketPriceOffset;
    protected String formattedQuoteAmount;
    protected String formattedQuoteAmountWithMinAmount;
    protected String baseAmountCode;
    protected String quoteAmountCode;

    protected final BehaviorSubject<Map<String, MarketPrice>> marketPriceSubject;
    protected Disposable marketPriceDisposable;
    private Double marketPriceOffset;

    public OfferEntity(SwapOffer offer, BehaviorSubject<Map<String, MarketPrice>> marketPriceSubject) {
        this.offer = offer;
        this.marketPriceSubject = marketPriceSubject;

        formattedBaseAmountWithMinAmount = AmountFormatter.formatAmountWithMinAmount(offer.getBaseLeg().monetary(),
                offer.findMinBaseAmount());
        baseAmountCode = offer.getBaseCode();

        formattedTransferOptions = offer.getOfferOptions().stream()
                .filter(offerOption -> offerOption instanceof TransferOption)
                .map(offerOption -> (TransferOption) offerOption)
                .map(OfferFormatter::formatTransferOptions)
                .findAny().orElse("");

        marketPriceDisposable = marketPriceSubject.subscribe(this::updatedPriceAndAmount, Throwable::printStackTrace);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void activate() {
        marketPriceDisposable = marketPriceSubject.subscribe(this::updatedPriceAndAmount);
    }

    public void deactivate() {
        marketPriceDisposable.dispose();
    }

    public String getFormattedAskAmountWithMinAmount() {
        return offer.isUseAskLegForBaseCurrency() ? formattedBaseAmountWithMinAmount : formattedQuoteAmountWithMinAmount;
    }


    public String getFormattedBidAmountWithMinAmount() {
        return offer.isUseAskLegForBaseCurrency() ? formattedQuoteAmountWithMinAmount : formattedBaseAmountWithMinAmount;
    }


    public int compareBaseAmount(OfferEntity other) {
        return Long.compare(offer.getBaseLeg().amount(), other.getOffer().getBaseLeg().amount());
    }

    public int compareAskAmount(OfferEntity other) {
        return Long.compare(offer.getAskLeg().amount(), other.getOffer().getAskLeg().amount());
    }

    public int compareBidAmount(OfferEntity other) {
        return Long.compare(offer.getBidLeg().amount(), other.getOffer().getBidLeg().amount());
    }

    public int compareQuoteAmount(OfferEntity other) {
        return quoteAmount.compareTo(other.quoteAmount);
    }

    public int compareQuote(OfferEntity other) {
        return quote.compareTo(other.quote);
    }

    public int compareMarketPriceOffset(OfferEntity other) {
        return marketPriceOffset.compareTo(other.marketPriceOffset);
    }

    @Override
    public int compareTo(OfferEntity other) {
        return compareQuote(other);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void updatedPriceAndAmount(Map<String, MarketPrice> marketPriceMap) {
        MarketPrice marketPrice = marketPriceMap.get(offer.getQuoteLeg().code());
        if (marketPrice == null) {
            return;
        }

        Quote marketQuote = marketPrice.quote();
        String type;
        if (offer.findMarketPriceOffset().isPresent()) {
            marketPriceOffset = offer.findMarketPriceOffset().get();
            quote = Quote.fromMarketPriceOffset(marketQuote, marketPriceOffset);
            quoteAmount = Quote.toQuoteMonetary(offer.getBaseLeg().monetary(), quote);
            type = "Var";
        } else {
            quote = offer.getQuote();
            quoteAmount = offer.getQuoteLeg().monetary();
            marketPriceOffset = Quote.offsetOf(marketQuote, quote);
            type = "Fix";
        }
        formattedQuote = QuoteFormatter.format(quote);
        formattedMarketPriceOffset = QuoteFormatter.formatMarketPriceOffset(marketPriceOffset) + " [" + type + "]";

        formattedQuoteAmount = AmountFormatter.formatAmount(quoteAmount);

        formattedQuoteAmountWithMinAmount = AmountFormatter.formatAmountWithMinAmount(quoteAmount,
                offer.findMinQuoteAmount(quoteAmount.getValue()));
    }

    @Override
    public String toString() {
        return "OfferEntity{" +
                "\r\n     offer=" + offer +
                ",\r\n     quote=" + quote +
                ",\r\n     quoteAmount=" + quoteAmount +
                ",\r\n     formattedBaseAmountWithMinAmount='" + formattedBaseAmountWithMinAmount + '\'' +
                ",\r\n     formattedTransferOptions='" + formattedTransferOptions + '\'' +
                ",\r\n     formattedQuote='" + formattedQuote + '\'' +
                ",\r\n     formattedQuoteAmount='" + formattedQuoteAmount + '\'' +
                ",\r\n     formattedQuoteAmountWithMinAmount='" + formattedQuoteAmountWithMinAmount + '\'' +
                "\r\n}";
    }
}
