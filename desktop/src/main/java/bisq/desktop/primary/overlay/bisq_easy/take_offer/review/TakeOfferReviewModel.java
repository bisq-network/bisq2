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
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
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
    private final StringProperty baseSideAmount = new SimpleStringProperty();
    private final StringProperty quoteSideAmount = new SimpleStringProperty();
    private final StringProperty paymentMethod = new SimpleStringProperty();
    @Setter
    private Monetary baseSideAmountAsMonetary;
    @Setter
    private Monetary quoteSideAmountAsMonetary;
    @Setter
    private List<String> paymentMethodNames;
    @Setter
    private BisqEasyPublicChatMessage myOfferMessage;
    @Setter
    private boolean isMinAmountEnabled;
    private final BooleanProperty matchingOffersVisible = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();

    private final StringProperty subtitle = new SimpleStringProperty();
    private final StringProperty amounts = new SimpleStringProperty();
    private final StringProperty payValue = new SimpleStringProperty();
    private final StringProperty receiveValue = new SimpleStringProperty();
    private final StringProperty methodValue = new SimpleStringProperty();
    private final StringProperty sellersPriceValue = new SimpleStringProperty();
    private final StringProperty sellersPriceValueDetails = new SimpleStringProperty();
    private final StringProperty sellersPremiumValue = new SimpleStringProperty();
    private final StringProperty sellersPremiumValueDetails = new SimpleStringProperty();


    private final StringProperty marketPriceDescription = new SimpleStringProperty();
    private final StringProperty sellersPremium = new SimpleStringProperty();

    private final StringProperty sellersPrice = new SimpleStringProperty();
}