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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.review;

import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class CreateOfferReviewOfferModel implements Model {
    @Setter
    private boolean showMatchingOffers;
    @Setter
    private BisqEasyPublicChatChannel selectedChannel;
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private List<String> settlementMethodNames;
    @Setter
    private String myOfferText;
    @Setter
    private BisqEasyPublicChatMessage myOfferMessage;
    @Setter
    private boolean isMinAmountEnabled;
    @Setter
    private PriceSpec priceSpec = new MarketPriceSpec();
    @Setter
    private AmountSpec amountSpec;
    private final BooleanProperty matchingOffersVisible = new SimpleBooleanProperty();
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();
    private final ObservableList<CreateOfferReviewOfferView.ListItem> matchingOffers = FXCollections.observableArrayList();
    private final SortedList<CreateOfferReviewOfferView.ListItem> sortedList = new SortedList<>(matchingOffers);

    void reset() {
        showMatchingOffers = false;
        selectedChannel = null;
        direction = null;
        market = null;
        settlementMethodNames.clear();
        myOfferText = null;
        myOfferMessage = null;
        isMinAmountEnabled = false;
        priceSpec = new MarketPriceSpec();
        amountSpec = null;
        matchingOffersVisible.set(false);
        showCreateOfferSuccess.set(false);
        matchingOffers.clear();

    }
}