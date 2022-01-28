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

package bisq.desktop.primary.main.content.social.hangout;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.security.KeyPairService;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class HangoutModel implements Model {


    public enum PublicChannelId implements Serializable {
        BTC_EUR,
        BTC_USD,
        ANY
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private final ChatService chatService;

    private final Map<String, StringProperty> chatMessagesByChannelId = new HashMap<>();
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");

    private final ObservableList<Channel> channels = FXCollections.observableArrayList();
    public final ObjectProperty<Channel> selectedChannel = new SimpleObjectProperty<>();

    public HangoutModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        identityService = applicationService.getIdentityService();
        keyPairService = applicationService.getKeyPairService();
        chatService = applicationService.getChatService();
    }

    public void setAllChannels(Collection<PrivateChannel> privateChannels) {
        this.channels.setAll(privateChannels);
    }

    public void addChannel(Channel channel) {
        if (!channels.contains(channel)) {
            channels.add(channel);
        }
    }

    void selectChannel(Channel channel) {
        selectedChannel.set(channel);
        setAllChatMessages(channel);
    }

    void setAllChatMessages(Channel channel) {
        chatMessagesByChannelId.putIfAbsent(channel.getId(), new SimpleStringProperty(""));
        StringProperty chatMessages = chatMessagesByChannelId.get(channel.getId());
        chatMessages.set("");
        channel.getChatMessages().forEach(chatMessage -> addChatMessage(channel, chatMessage));
        if (selectedChannel.get() != null && selectedChannel.get().getId().equals(channel.getId())) {
            selectedChatMessages.set(chatMessages.get());
        }
    }

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
                chatMessage.getSenderUserName().substring(0,12) +
                "] " +
                chatMessage.getText());

        if (selectedChannel.get() != null && selectedChannel.get().getId().equals(channel.getId())) {
            selectedChatMessages.set(chatMessages.get());
        }
    }

    String getDisplayString(PublicChannelId publicChannelId) {
        return switch (publicChannelId) {
            case BTC_EUR -> Res.common.get("social.hangout.btcEurMarket");
            case BTC_USD -> Res.common.get("social.hangout.btcUsdMarket");
            case ANY -> Res.common.get("social.hangout.anyMarket");
        };
    }

    void setSendMessageResult(String channelId, ConfidentialMessageService.Result result, BroadcastResult broadcastResult) {
        log.info("Send message result for channelId {}: {}",
                channelId, result.getState() + "; " + broadcastResult.toString()); //todo
    }

    void setSendMessageError(String channelId, Throwable throwable) {
        log.error("Send message resulted in an error: channelId={}, error={}", channelId, throwable.toString());  //todo
    }

}
