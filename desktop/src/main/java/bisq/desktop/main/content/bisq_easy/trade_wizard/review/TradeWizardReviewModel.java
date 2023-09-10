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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class TradeWizardReviewModel implements Model {
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    @Setter
    private FiatPaymentMethodSpec paymentMethodSpec;
    @Setter
    private List<String> paymentMethodNames;

    @Setter
    private BisqEasyTrade bisqEasyTrade;
    @Setter
    private UserProfile peersUserProfile;

    private final BooleanProperty requirePaymentMethodSelection = new SimpleBooleanProperty();
    private final BooleanProperty matchingOffersVisible = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();
    private final StringProperty fiatPaymentMethodDisplayString = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final StringProperty amountDescription = new SimpleStringProperty();
    private final StringProperty toPay = new SimpleStringProperty();
    private final StringProperty toReceive = new SimpleStringProperty();
    private final StringProperty sellersPrice = new SimpleStringProperty();
    private final StringProperty sellersPriceDetails = new SimpleStringProperty();
    private final StringProperty sellersPremium = new SimpleStringProperty();

    @Setter
    private Monetary takersBaseSideAmount;
    @Setter
    private Monetary takersQuoteSideAmount;
    @Setter
    private PriceSpec sellersPriceSpec;

    public void reset() {
        bisqEasyOffer = null;
        paymentMethodSpec = null;
        paymentMethodNames = null;

        peersUserProfile = null;
        bisqEasyTrade = null;
    }
}