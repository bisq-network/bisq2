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

package bisq.desktop.primary.overlay.bisq_easy.createoffer.review;

import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class ReviewOfferModel implements Model {
    @Setter
    private boolean showMatchingOffers;
    @Setter
    private BisqEasyPublicChatChannel selectedChannel;
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private Monetary baseSideAmount;
    @Setter
    private Monetary quoteSideAmount;
    @Setter
    private List<String> paymentMethods;
    @Setter
    private String myOfferText;
    @Setter
    private BisqEasyPublicChatMessage myOfferMessage;
    private final BooleanProperty matchingOffersFound = new SimpleBooleanProperty();
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();
    private final ObservableList<ReviewOfferView.ListItem> matchingOffers = FXCollections.observableArrayList();
    private final SortedList<ReviewOfferView.ListItem> sortedList = new SortedList<>(matchingOffers);

    void reset() {
        showMatchingOffers = false;
        selectedChannel = null;
        direction = null;
        market = null;
        baseSideAmount = null;
        quoteSideAmount = null;
        paymentMethods = null;
        myOfferText = null;
        myOfferMessage = null;
        matchingOffersFound.set(false);
        showCreateOfferSuccess.set(false);
        showTakeOfferSuccess.set(false);
        matchingOffers.clear();
    }
}