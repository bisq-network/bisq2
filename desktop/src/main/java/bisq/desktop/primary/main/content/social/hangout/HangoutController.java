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
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.social.user.ChatUserController;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.social.chat.*;
import bisq.social.intent.TradeIntent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import static bisq.social.chat.ChannelType.PRIVATE;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class HangoutController implements InitWithDataController<TradeIntent>, ChatService.Listener {
    private final ChatService chatService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final HangoutModel model;
    @Getter
    private final HangoutView view;

    public HangoutController(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        chatService = serviceProvider.getChatService();

        ChatUserController chatUserController = new ChatUserController(serviceProvider);
        model = new HangoutModel(serviceProvider);
        view = new HangoutView(model, this, chatUserController.getView());
    }

    @Override
    public void initWithData(TradeIntent tradeIntent) {
        // todo add tradeIntent
        String tradeIntentId = tradeIntent.id();
        ChatPeer chatPeer = tradeIntent.maker();
        String userName = chatService.findUserName(tradeIntentId).orElse("Taker@" + StringUtils.truncate(tradeIntentId, 8));
        ChatIdentity chatIdentity = chatService.getOrCreateChatIdentity(userName, tradeIntentId);
        PrivateChannel privateChannel = chatService.getOrCreatePrivateChannel(tradeIntentId, chatPeer, chatIdentity);
        model.addChannel(privateChannel);
        chatService.selectChannel(privateChannel);
    }

    @Override
    public void onViewAttached() {
        chatService.addListener(this);

        model.setAllChannels(chatService.getChatModel().getPrivateChannels());
        chatService.getChatModel().getSelectedChannel().ifPresent(model::selectChannel);
    }

    @Override
    public void onViewDetached() {
        chatService.removeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onChannelAdded(Channel channel) {
        UIThread.run(() -> model.addChannel(channel));
    }

    @Override
    public void onChannelSelected(Channel channel) {
        UIThread.run(() -> model.selectChannel(channel));
    }

    @Override
    public void onChatMessageAdded(Channel channel, ChatMessage newChatMessage) {
        UIThread.run(() -> model.addChatMessage(channel, newChatMessage));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onSendMessage(String text) {
        Channel channel = model.getSelectedChannel().get();
        ChatIdentity chatIdentity = chatService.findChatIdentity(channel.getId())
                .orElseThrow(() -> new IllegalArgumentException("optionalChatIdentity must be present"));
        String channelId = channel.getId();
        ChatMessage chatMessage = new ChatMessage(channelId,
                text,
                chatIdentity.userName(),
                chatIdentity.identity().networkId(),
                new Date().getTime(),
                PRIVATE);
        chatService.addChatMessage(chatMessage, channel);

        checkArgument(channel instanceof PrivateChannel, "channel must be PrivateChannel");
        PrivateChannel privateChannel = (PrivateChannel) channel;
        NetworkId receiverNetworkId = privateChannel.getChatPeer().networkId();

        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = chatIdentity.identity().getNodeIdAndKeyPair();
        networkService.confidentialSendAsync(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair)
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
    }

    public void onSelectChannel(Channel channel) {
        chatService.selectChannel(channel);
    }
}
