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

package bisq.desktop.main.content.bisq_easy.take_offer.review;

import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class TakeOfferReviewModel implements Model {
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    @Setter
    private BisqEasyTrade bisqEasyTrade;
    @Setter
    private UserProfile peersUserProfile;
    @Setter
    private List<String> paymentMethodNames;
    @Setter
    private BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec;
    @Setter
    private FiatPaymentMethodSpec fiatPaymentMethodSpec;
    @Setter
    private Monetary takersBaseSideAmount;
    @Setter
    private Monetary takersQuoteSideAmount;
    private final ObjectProperty<TakeOfferStatus> takeOfferStatus = new SimpleObjectProperty<>(TakeOfferStatus.NOT_STARTED);
    @Setter
    private String price;
    @Setter
    private String priceDetails;
    @Setter
    private String bitcoinPaymentMethod;
    @Setter
    private String fiatPaymentMethod;
    @Setter
    private String fee;
    @Setter
    private String feeDetails;
    @Setter
    private boolean feeDetailsVisible;
    @Setter
    private long marketPrice;

    enum TakeOfferStatus {
        NOT_STARTED,
        SENT,
        SUCCESS
    }
}
