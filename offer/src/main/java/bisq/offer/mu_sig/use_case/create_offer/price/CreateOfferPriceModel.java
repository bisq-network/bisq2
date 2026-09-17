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

package bisq.offer.mu_sig.use_case.create_offer.price;

import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;

public class CreateOfferPriceModel {
    protected final Observable<PriceQuote> priceQuote = new Observable<>();
    protected final Observable<Boolean> useFixPrice = new Observable<>(false);
    protected final Observable<Double> pricePercentage = new Observable<>(0d);

    public CreateOfferPriceModel() {
    }

    /* --------------------------------------------------------------------- */
    // priceQuote
    /* --------------------------------------------------------------------- */

    void setPriceQuote(PriceQuote priceQuote) {
        this.priceQuote.set(priceQuote);
    }

    public ReadOnlyObservable<PriceQuote> priceQuoteObservable() {
        return priceQuote;
    }

    public PriceQuote getPriceQuote() {
        return priceQuote.get();
    }


    /* --------------------------------------------------------------------- */
    // useFixPrice
    /* --------------------------------------------------------------------- */

    void setUseFixPrice(boolean useFixPrice) {
        this.useFixPrice.set(useFixPrice);
    }

    public ReadOnlyObservable<Boolean> useFixPriceObservable() {
        return useFixPrice;
    }

    public boolean getUseFixPrice() {
        return useFixPrice.get();
    }

    /* --------------------------------------------------------------------- */
    // pricePercentage
    /* --------------------------------------------------------------------- */

    void setPricePercentage(double pricePercentage) {
        this.pricePercentage.set(pricePercentage);
    }

    public ReadOnlyObservable<Double> pricePercentageObservable() {
        return pricePercentage;
    }

    public double getPricePercentage() {
        return pricePercentage.get();
    }
}
