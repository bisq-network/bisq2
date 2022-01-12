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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.user.ChatUserController;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.NodeIdAndKeyPair;
import bisq.network.NodeIdAndPubKey;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HangoutController implements Controller, Node.Listener {
    private final DefaultServiceProvider serviceProvider;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final HangoutModel model;
    @Getter
    private final HangoutView view;

    public HangoutController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();

        ChatUserController chatUserController = new ChatUserController(serviceProvider);
        model = new HangoutModel(serviceProvider);
        view = new HangoutView(model, this, chatUserController.getView());


        model.addPublicChannel(Channel.ChannelType.BTC_EUR);
        model.addPublicChannel(Channel.ChannelType.BTC_USD);
        model.addPublicChannel(Channel.ChannelType.PUBLIC);
    }

    @Override
    public void onViewAttached() {
        networkService.addMessageListener(this);
    }

    @Override
    public void onViewDetached() {
        networkService.removeMessageListener(this);
    }

    @Override
    public void setData(Object data) {
        model.setData(data);
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof ChatMessage chatMessage) {
            UIThread.run(() -> model.addChatMessage(chatMessage));
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    public void send(String text) {
        model.getChannelId().ifPresent(channelId -> {
            log.error("channelId {}", channelId);
            NodeIdAndPubKey nodeIdAndPubKey = identityService.getNodeIdAndPubKey(channelId);
            networkService.getInitializedNetworkIdAsync(nodeIdAndPubKey)
                    .whenComplete((senderNetworkId, throwable) -> {
                        if (throwable != null) {
                            UIThread.run(() -> model.setSendMessageError(channelId, throwable));
                            return;
                        }
                        ChatMessage chatMessage = model.createAndAddChatMessage(text, channelId, senderNetworkId);
                        NetworkId receiverNetworkId = model.getSelectedChatPeer().orElseThrow().networkId();
                        NodeIdAndKeyPair nodeIdAndKeyPair = identityService.getNodeIdAndKeyPair(channelId);
                        networkService.confidentialSendAsync(chatMessage, receiverNetworkId, nodeIdAndKeyPair)
                                .whenComplete((resultMap, throwable2) -> {
                                    if (throwable2 != null) {
                                        UIThread.run(() -> model.setSendMessageError(channelId, throwable2));
                                        return;
                                    }
                                    resultMap.forEach((transportType, res) -> {
                                        ConfidentialMessageService.Result result = resultMap.get(transportType);
                                        result.getMailboxFuture().forEach(broadcastFuture -> broadcastFuture
                                                .whenComplete((broadcastResult, throwable3) -> {
                                                    if (throwable3 != null) {
                                                        UIThread.run(() -> model.setSendMessageError(channelId, throwable3));
                                                        return;
                                                    }
                                                    UIThread.run(() -> model.setSendMessageResult(channelId, result, broadcastResult));
                                                }));
                                    });
                                });
                    });
        });
    }

    public void onSelectChannel(Channel channel) {
        model.selectChannel(channel);
    }
}
