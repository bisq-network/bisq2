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

package bisq.desktop.primary.overlay.onboarding.offer.complete;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.components.ChatMessagesListView;
import bisq.offer.spec.Direction;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

@Getter
class OfferCompletedModel implements Model {
    private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
    private  PublicTradeChatMessage offerMessage ;

    private final ObservableList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> takerMessages = FXCollections.observableArrayList();
    private final SortedList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> sortedTakerMessages = new SortedList<>(takerMessages);

    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>();
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<String> paymentMethod = new SimpleObjectProperty<>();

    
    OfferCompletedModel() {
    }
}