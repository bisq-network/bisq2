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

package bisq.desktop.main.content.mu_sig.create_offer.review;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class MuSigCreateOfferReviewModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market = MarketRepository.getDefaultBtcFiatMarket();
    @Setter
    private MuSigOffer offer;
    @Setter
    private FiatPaymentMethod takersSelectedFiatPaymentMethod;
    @Setter
    private Monetary minBaseSideAmount;
    @Setter
    private Monetary maxBaseSideAmount;
    @Setter
    private Monetary fixBaseSideAmount;
    @Setter
    private Monetary minQuoteSideAmount;
    @Setter
    private Monetary maxQuoteSideAmount;
    @Setter
    private Monetary fixQuoteSideAmount;
    @Setter
    private PriceSpec priceSpec;
    @Setter
    private List<FiatPaymentMethod> paymentMethods;
    @Setter
    private String headline;
    @Setter
    private String headerFiatPaymentMethod;
    @Setter
    private String detailsHeadline;
    @Setter
    private boolean isRangeAmount;
    @Setter
    private String paymentMethodDescription;
    @Setter
    private String paymentMethod;
    @Setter
    private String priceDescription;
    @Setter
    private String price;
    @Setter
    private String priceDetails;
    @Setter
    private String fee;
    @Setter
    private String feeDetails;
    private final ObservableList<FiatPaymentMethod> takersFiatPaymentMethods = FXCollections.observableArrayList();
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();
    @Setter
    private long marketPrice;

    public void reset() {
        offer = null;
        takersSelectedFiatPaymentMethod = null;
        minBaseSideAmount = null;
        maxBaseSideAmount = null;
        fixBaseSideAmount = null;
        minQuoteSideAmount = null;
        maxQuoteSideAmount = null;
        fixQuoteSideAmount = null;
        priceSpec = null;
        paymentMethods = null;
        headline = null;
        detailsHeadline = null;
        priceDescription = null;
        price = null;
        priceDetails = null;
        paymentMethodDescription = null;
        paymentMethod = null;
        fee = null;
        feeDetails = null;
        takersFiatPaymentMethods.clear();
        showCreateOfferSuccess.set(false);
        marketPrice = 0;
    }
}
