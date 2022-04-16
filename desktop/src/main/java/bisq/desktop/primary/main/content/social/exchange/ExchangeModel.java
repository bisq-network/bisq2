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

package bisq.desktop.primary.main.content.social.exchange;

import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.social.components.ChatUserDetails;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.ChatService;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
public class ExchangeModel implements Model {
    private final Map<String, StringProperty> chatMessagesByChannelId = new HashMap<>();
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");
    private final StringProperty selectedChannelAsString = new SimpleStringProperty("");
    private final ObjectProperty<Channel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
    private final ObjectProperty<Pane> chatUserDetailsRoot = new SimpleObjectProperty<>();
    private final BooleanProperty sideBarVisible = new SimpleBooleanProperty();
    private final BooleanProperty channelInfoVisible = new SimpleBooleanProperty();
    private final BooleanProperty notificationsVisible = new SimpleBooleanProperty();
    private final BooleanProperty filterBoxVisible = new SimpleBooleanProperty();

  
    private final ChatService chatService;
   
    private final UserProfileService userProfileService;
    @Setter
    private Optional<ChatUserDetails> chatUserDetails = Optional.empty();
    private final ObservableList<String> tradeTags = FXCollections.observableArrayList();
    private final ObservableList<String> currencyTags = FXCollections.observableArrayList();
    private final ObservableList<String> paymentMethodsTags = FXCollections.observableArrayList();
    private final ObservableList<String> customTags = FXCollections.observableArrayList();
    
    public ExchangeModel(ChatService chatService, UserProfileService userProfileService) {
        this.chatService = chatService;
        this.userProfileService = userProfileService;
      
    }

    void setSendMessageResult(String channelId, ConfidentialMessageService.Result result, BroadcastResult broadcastResult) {
        log.info("Send message result for channelId {}: {}",
                channelId, result.getState() + "; " + broadcastResult.toString()); //todo
    }

    void setSendMessageError(String channelId, Throwable throwable) {
        log.error("Send message resulted in an error: channelId={}, error={}", channelId, throwable.toString());  //todo
    }

    boolean isMyMessage(ChatMessage chatMessage) {
        return chatService.isMyMessage(chatMessage);
    }
}
