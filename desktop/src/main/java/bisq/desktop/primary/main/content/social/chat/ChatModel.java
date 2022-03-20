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

package bisq.desktop.primary.main.content.social.chat;

import bisq.desktop.common.view.Model;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.social.chat.Channel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class ChatModel implements Model {
    private final Map<String, StringProperty> chatMessagesByChannelId = new HashMap<>();
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");
    private final StringProperty selectedChannelAsString = new SimpleStringProperty("");
    private final ObjectProperty<Channel> selectedChannel = new SimpleObjectProperty<>();
    private final BooleanProperty infoVisible = new SimpleBooleanProperty();
    private final BooleanProperty notificationsVisible = new SimpleBooleanProperty();
    private final BooleanProperty filterBoxVisible = new SimpleBooleanProperty();
    private final double defaultLeftDividerPosition = 0.3;
    private final ObservableList<ChatMessageListItem> chatMessages = FXCollections.observableArrayList();
    private final SortedList<ChatMessageListItem> sortedChatMessages = new SortedList<>(chatMessages);
    private final FilteredList<ChatMessageListItem> filteredChatMessages = new FilteredList<>(sortedChatMessages);
    private final StringProperty textInput = new SimpleStringProperty("");

    public ChatModel() {
    }
/*
    void addChatMessage(Channel channel, ChatMessage chatMessage) {
        chatMessagesByChannelId.putIfAbsent(channel.getId(), new SimpleStringProperty(""));
        StringProperty chatMessages = chatMessagesByChannelId.get(channel.getId());
        String previous = chatMessages.get();
        if (!previous.isEmpty()) {
            previous += "\n";
        }
        chatMessages.set(previous +
                "[" + DateFormatter.formatDateTime(new Date(chatMessage.getDate())) +
                "] [" +
                chatMessage.getSenderUserName().substring(0, 12) +
                "] " +
                chatMessage.getText());

        if (selectedChannel.get() != null && selectedChannel.get().getId().equals(channel.getId())) {
            selectedChatMessages.set(chatMessages.get());
        }
    }

    String getDisplayString(PublicChannelId publicChannelId) {
        return switch (publicChannelId) {
            case BTC_EUR -> Res.get("social.chat.btcEurMarket");
            case BTC_USD -> Res.get("social.chat.btcUsdMarket");
            case ANY -> Res.get("social.chat.anyMarket");
        };
    }*/

    void setSendMessageResult(String channelId, ConfidentialMessageService.Result result, BroadcastResult broadcastResult) {
        log.info("Send message result for channelId {}: {}",
                channelId, result.getState() + "; " + broadcastResult.toString()); //todo
    }

    void setSendMessageError(String channelId, Throwable throwable) {
        log.error("Send message resulted in an error: channelId={}, error={}", channelId, throwable.toString());  //todo
    }

}
