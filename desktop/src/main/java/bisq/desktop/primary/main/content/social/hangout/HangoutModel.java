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
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.security.KeyPairService;
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
    // private final String myUserName;
    //  private final String myUserId;
    private Optional<TradeIntent> selectedTradeIntent = Optional.empty();
    public StringProperty chatHistory = new SimpleStringProperty("");
    public Map<String, Channel> channelsById = new HashMap<>();
    public Optional<String> selectedChannelId = Optional.empty();
    private Optional<String> tradeIntentId;
    private ObservableList<String> channelIds = FXCollections.observableArrayList();
    private Optional<ChatUser> peer = Optional.empty();

    public HangoutModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();


        //  myUserName = "Bob"; //todo
        //  myUserId = "id"; //todo

        String id = Channel.ChannelType.BTC_EUR.name();
        channelsById.put(id, new Channel(id, "BTC-EUR market"));
        id = Channel.ChannelType.BTC_USD.name();
        channelsById.put(id, new Channel(id, "BTC-USD market"));
        id = Channel.ChannelType.PUBLIC.name();
        channelsById.put(id, new Channel(id, "Anything"));
    }

    void setData(Object data) {
        selectedTradeIntent = Optional.of(TradeIntent.class.cast(data));
        tradeIntentId = Optional.of(selectedTradeIntent.orElseThrow().id());
        peer = Optional.of(selectedTradeIntent.get().maker());
        maybeAddChannel(peer.get().id(), peer.get().userName());
    }

    void selectChannel(String channelId) {
        selectedChannelId = Optional.of(channelId);
        if (channelsById.containsKey(channelId)) {
            updateChatHistory(channelId);
        }
    }

    void addChatMessage(ChatMessage chatMessage) {
        tradeIntentId = Optional.of(chatMessage.getTradeIntentId());
        peer = Optional.of(chatMessage.getSender());
        String channelId = chatMessage.getChannelId();
        Channel channel = maybeAddChannel(channelId, peer.get().userName());
        channel.addMessages(new ChatEntry(peer.get(), chatMessage.getText(), new Date().getTime()));
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
        Channel channel = maybeAddChannel(channelId, peer.get().userName());
        channel.addMessages(new ChatEntry(senderChatUser, text, new Date().getTime()));
        updateChatHistory(channelId);
        return chatMessage;
    }

    Optional<String> getChannelId() {
        return peer.map(ChatUser::id);
    }

    private void updateChatHistory(String channelId) {
        StringBuilder sb = new StringBuilder();
        channelsById.get(channelId).getMessages()
                .forEach(chatEntry -> sb.append(DateFormatter.formatDateTime(new Date(chatEntry.date())))
                        .append(": ")
                        .append(chatEntry.text())
                        .append("\n"));
        chatHistory.set(sb.toString());
    }

    private Channel maybeAddChannel(String channelId, String userName) {
        if (channelsById.containsKey(channelId)) {
            return channelsById.get(channelId);
        }
        Channel channel = new Channel(channelId, userName);
        channelsById.put(channelId, channel);
        channelIds.add(channelId);
        return channel;
    }
}
