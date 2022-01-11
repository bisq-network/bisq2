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
import bisq.common.data.Pair;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.social.tradeintent.TradeIntent;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.message.ChatMessage;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class HangoutModel implements Model, Node.Listener {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private final String myUseName;
    private Optional<TradeIntent> selectedTradeIntent = Optional.empty();
    private Optional<NetworkId> selectedNetworkId = Optional.empty();
    public StringProperty chatText = new SimpleStringProperty("");
    public ObservableList<String> chatPeers = FXCollections.observableArrayList();
    public Optional<String> selectedChatPeer = Optional.empty();
    private String tradeIntentId;

    public HangoutModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();
        networkService.addMessageListener(this);

        myUseName = "Bob"; //todo
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof ChatMessage chatMessage) {
            UIThread.run(() -> {
                chatText.set(chatText.get() + "<< " + chatMessage.getText() + "\n");
                tradeIntentId = chatMessage.getTradeIntentId();
                selectedNetworkId = Optional.of(chatMessage.getSenderNetworkId());
                String senderUseName = chatMessage.getSenderUseName();
                if (!chatPeers.contains(senderUseName)) {
                    chatPeers.add(senderUseName);
                }
            });
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    void setData(Object data) {
        Pair<TradeIntent, NetworkId> pair = Pair.class.cast(data);
        selectedTradeIntent = Optional.of(pair.first());
        selectedNetworkId = Optional.of(pair.second());
        tradeIntentId = selectedTradeIntent.orElseThrow().id();
        String userId = selectedTradeIntent.get().userId();
        setSelectedChatPeer(userId);
        if (!chatPeers.contains(userId)) {
            chatPeers.add(userId);
        }
    }

    public void setSelectedChatPeer(String chatPeer) {
        selectedChatPeer = Optional.of(chatPeer);
    }

    public void sendMessage(String message) {
        chatText.set(chatText.get() + ">> " + message + "\n");
        checkArgument(selectedNetworkId.isPresent(), "Network ID must be set before calling sendMessage");
        NetworkId receiverNetworkId = selectedNetworkId.get();
        KeyPair senderKeyPair = keyPairService.getOrCreateKeyPair(tradeIntentId);
        CompletableFuture<String> future = new CompletableFuture<>();
        String senderNodeId = selectedNetworkId.get().getNodeId();
        PubKey senderPubKey = new PubKey(senderKeyPair.getPublic(), tradeIntentId);
        networkService.maybeInitializeServer(senderNodeId).get(Transport.Type.CLEAR)
                .whenComplete((result1, throwable1) -> {
                    NetworkId senderNetworkId = networkService.findNetworkId(senderNodeId, senderPubKey).orElseThrow();
                    ChatMessage chatMessage = new ChatMessage(message, tradeIntentId, senderNetworkId, myUseName);

                    networkService.confidentialSendAsync(chatMessage, receiverNetworkId, senderKeyPair, senderNodeId)
                            .whenComplete((resultMap, throwable) -> {
                                if (throwable == null) {
                                    resultMap.forEach((transportType, value1) -> {
                                        ConfidentialMessageService.Result result = resultMap.get(transportType);
                                        result.getMailboxFuture().forEach(broadcastFuture -> broadcastFuture.whenComplete((broadcastResult, error) -> {
                                            if (error == null) {
                                                future.complete(result.getState() + "; " + broadcastResult.toString());
                                            } else {
                                                String value = result.getState().toString();
                                                if (result.getState() == ConfidentialMessageService.State.FAILED) {
                                                    value += " with Error: " + result.getErrorMsg();
                                                }
                                                future.complete(value);
                                            }
                                        }));
                                    });
                                }
                            });
                });
    }
}
