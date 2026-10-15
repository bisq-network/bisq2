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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Delegate;

public class TakeOfferPriceService {
    @Getter(AccessLevel.PACKAGE)
    @Delegate
    private final TakeOfferPriceModel model;

    public TakeOfferPriceService() {
        this.model = new TakeOfferPriceModel();
    }

    public void setPriceQuote(PriceQuote priceQuote) {
        model.setPriceQuote(priceQuote);
    }

    public void setPriceDeviation(Double priceDeviation) {
        model.setPriceDeviation(priceDeviation);
    }

    public void setMarketPriceQuote(PriceQuote marketPriceQuote) {
        model.setMarketPriceQuote(marketPriceQuote);
    }
}
