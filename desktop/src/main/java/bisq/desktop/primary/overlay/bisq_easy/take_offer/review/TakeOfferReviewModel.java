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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.desktop.common.view.Model;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class TakeOfferReviewModel implements Model {
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    @Setter
    private UserProfile peersUserProfile;
    private final StringProperty paymentMethod = new SimpleStringProperty();
    @Setter
    private List<String> paymentMethodNames;
    @Setter
    private BisqEasyPublicChatMessage myOfferMessage;
    @Setter
    private boolean isMinAmountEnabled;
    private final BooleanProperty matchingOffersVisible = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();

    private final StringProperty subtitle = new SimpleStringProperty();
    private final StringProperty amountDescription = new SimpleStringProperty();
    private final StringProperty toPay = new SimpleStringProperty();
    private final StringProperty toReceive = new SimpleStringProperty();
    private final StringProperty method = new SimpleStringProperty();
    private final StringProperty sellersPrice = new SimpleStringProperty();
    private final StringProperty sellersPriceDetails = new SimpleStringProperty();
    private final StringProperty sellersPremium = new SimpleStringProperty();

    @Setter
    private AmountSpec tradeAmountSpec;
    @Setter
    private PriceSpec sellersPriceSpec;
}