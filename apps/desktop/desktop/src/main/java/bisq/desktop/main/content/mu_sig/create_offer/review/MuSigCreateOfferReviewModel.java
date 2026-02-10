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

import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class MuSigCreateOfferReviewModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private MuSigOffer offer;
    @Setter
    private PaymentMethod<?> takersSelectedPaymentMethod;
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
    private List<PaymentMethod<?>> paymentMethods;
    @Setter
    private String headline;
    @Setter
    private String headerPaymentMethod;
    @Setter
    private String detailsHeadline;
    @Setter
    private boolean isRangeAmount;
    @Setter
    private String paymentMethodDescription;
    @Setter
    private String paymentMethodsDisplayString;
    @Setter
    private String paymentMethodDetails;
    @Setter
    private String priceDescription;
    @Setter
    private String priceWithCode;
    @Setter
    private String priceDetails;
    @Setter
    private String price;
    @Setter
    private String priceCode;
    @Setter
    private String collateral;
    @Setter
    private String fee;
    @Setter
    private String feeDetails;
    @Setter
    private long marketPrice;
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();

    void reset() {
        direction = null;
        market = null;
        offer = null;
        takersSelectedPaymentMethod = null;
        minBaseSideAmount = null;
        maxBaseSideAmount = null;
        fixBaseSideAmount = null;
        minQuoteSideAmount = null;
        maxQuoteSideAmount = null;
        fixQuoteSideAmount = null;
        priceSpec = null;
        paymentMethods = null;
        headline = null;
        headerPaymentMethod = null;
        detailsHeadline = null;
        isRangeAmount = false;
        paymentMethodDescription = null;
        paymentMethodsDisplayString = null;
        priceDescription = null;
        priceWithCode = null;
        priceDetails = null;
        price = null;
        priceCode = null;
        fee = null;
        feeDetails = null;
        marketPrice = 0;
        showCreateOfferSuccess.set(false);
    }
}
