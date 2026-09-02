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

package bisq.offer.mu_sig.use_case.take_offer.price;

import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;

public class TakeOfferPriceModel implements TakeOfferPriceReadOnlyModel {
    protected final Observable<PriceQuote> priceQuote = new Observable<>();
    protected final Observable<Double> priceDeviation = new Observable<>();
    protected final Observable<PriceQuote> marketPriceQuote = new Observable<>();

    public TakeOfferPriceModel() {
    }

    /* --------------------------------------------------------------------- */
    // priceQuote
    /* --------------------------------------------------------------------- */

    void setPriceQuote(PriceQuote priceQuote) {
        this.priceQuote.set(priceQuote);
    }

    @Override
    public ReadOnlyObservable<PriceQuote> priceQuoteObservable() {
        return priceQuote;
    }

    @Override
    public PriceQuote getPriceQuote() {
        return priceQuote.get();
    }

    /* --------------------------------------------------------------------- */
    // priceDeviation
    /* --------------------------------------------------------------------- */

    void setPriceDeviation(Double priceDeviation) {
        this.priceDeviation.set(priceDeviation);
    }

    @Override
    public ReadOnlyObservable<Double> priceDeviationObservable() {
        return priceDeviation;
    }

    @Override
    public Double getPriceDeviation() {
        return priceDeviation.get();
    }

    /* --------------------------------------------------------------------- */
    // marketPriceQuote
    /* --------------------------------------------------------------------- */

    void setMarketPriceQuote(PriceQuote marketPriceQuote) {
        this.marketPriceQuote.set(marketPriceQuote);
    }

    @Override
    public ReadOnlyObservable<PriceQuote> marketPriceQuoteObservable() {
        return marketPriceQuote;
    }

    @Override
    public PriceQuote getMarketPriceQuote() {
        return marketPriceQuote.get();
    }
}
