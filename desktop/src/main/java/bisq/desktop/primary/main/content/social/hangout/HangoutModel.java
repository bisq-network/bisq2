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

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.social.tradeintent.TradeIntent;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.security.KeyPairService;
import bisq.social.chat.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
public class HangoutModel implements Model {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private final ChatService chatService;
    private Optional<TradeIntent> selectedTradeIntent = Optional.empty();
    public final StringProperty chatHistory = new SimpleStringProperty("");
    public Map<String, Channel> channelsById = new HashMap<>();
    public Optional<String> selectedChannelId = Optional.empty();
    private Optional<String> tradeIntentId;
    private final ObservableList<String> channelIds = FXCollections.observableArrayList();
    private Optional<ChatUser> selectedChatPeer = Optional.empty();

    public HangoutModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();
        chatService= serviceProvider.getChatService();
    }

    void setData(Object data) {
        selectedTradeIntent = Optional.of(TradeIntent.class.cast(data));
        tradeIntentId = Optional.of(selectedTradeIntent.orElseThrow().id());
        selectedChatPeer = Optional.of(selectedTradeIntent.get().maker());
        maybeAddChannel(selectedChatPeer.get().id(), selectedChatPeer.get().userName());
    }

    void selectChannel(Channel channel) {
        String channelId = channel.getId();
        selectedChannelId = Optional.of(channelId);
        if (channelsById.containsKey(channelId)) {
            updateChatHistory(channel);
        }
    }

    void addChatMessage(ChatMessage chatMessage) {
        tradeIntentId = Optional.of(chatMessage.getTradeIntentId());
        selectedChatPeer = Optional.of(chatMessage.getSender());
        String channelId = chatMessage.getChannelId();
        Channel channel = maybeAddChannel(channelId, selectedChatPeer.get().userName());
        channel.addMessages(new ChatEntry(selectedChatPeer.get(), chatMessage.getText(), new Date().getTime()));
        updateChatHistory(channel);
    }

    void setSendMessageResult(String channelId, ConfidentialMessageService.Result result, BroadcastResult broadcastResult) {
        log.info("Send message result for channelId {}: {}",
                channelId, result.getState() + "; " + broadcastResult.toString()); //todo
    }

    void setSendMessageError(String channelId, Throwable throwable) {
        log.error("Send message resulted in an error: channelId={}, error={}", channelId, throwable.toString());  //todo
    }

    ChatMessage createAndAddChatMessage(String text, String channelId, NetworkId senderNetworkId) {
        String tradeIntentId = this.tradeIntentId.orElseThrow();
        ChatUser senderChatUser = new ChatUser(tradeIntentId, tradeIntentId, senderNetworkId);
        ChatMessage chatMessage = new ChatMessage(channelId, text, tradeIntentId, senderChatUser);
        Channel channel = maybeAddChannel(channelId, selectedChatPeer.get().userName());
        channel.addMessages(new ChatEntry(senderChatUser, text, new Date().getTime()));
        updateChatHistory(channel);
        return chatMessage;
    }

    Optional<String> getChannelId() {
        return selectedChatPeer.map(ChatUser::id);
    }

    private void updateChatHistory(Channel channel) {
        StringBuilder sb = new StringBuilder();
        channel.getMessages()
                .forEach(chatEntry -> sb.append(DateFormatter.formatDateTime(new Date(chatEntry.date())))
                        .append(": ")
                        .append(chatEntry.text())
                        .append("\n"));
        chatHistory.set(sb.toString());
    }

    Channel maybeAddChannel(String channelId, String name) {
        return maybeAddChannel(Channel.ChannelType.PRIVATE, channelId, name);
    }

    Channel maybeAddChannel(Channel.ChannelType channelType, String channelId, String name) {
        if (channelsById.containsKey(channelId)) {
            return channelsById.get(channelId);
        }
        Channel channel = new Channel(channelType, channelId, name);
        channelsById.put(channelId, channel);
        channelIds.add(channelId);
        return channel;
    }

    public void addPublicChannel(Channel.ChannelType channelType) {
        String name = switch (channelType) {
            case BTC_EUR -> Res.common.get("social.hangout.btcEurMarket");
            case BTC_USD -> Res.common.get("social.hangout.btcUsdMarket");
            case PUBLIC -> Res.common.get("social.hangout.anyMarket");
            default -> throw new IllegalStateException("Unexpected value: " + channelType);
        };
        maybeAddChannel(channelType, channelType.name(), name);
    }
}
