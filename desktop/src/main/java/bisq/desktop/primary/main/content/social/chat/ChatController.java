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

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.social.chat.*;
import bisq.social.user.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Date;

import static bisq.social.chat.ChannelType.PRIVATE;
import static bisq.social.chat.ChannelType.PUBLIC;

@Slf4j
public class ChatController implements Controller {
    private final ChatService chatService;
    private final ChatModel model;
    @Getter
    private final ChatView view;
    private final UserProfileService userProfileService;
    private final NetworkService networkService;
    private final PublicChannelSelection publicChannelSelection;
    private final PrivateChannelSelection privateChannelSelection;
    private Pin chatMessagesPin;
    private Pin selectedChannelPin;

    public ChatController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        userProfileService = applicationService.getUserProfileService();
        networkService = applicationService.getNetworkService();

        UserProfileComboBox userProfileDisplay = new UserProfileComboBox(userProfileService);
        publicChannelSelection = new PublicChannelSelection(chatService);
        privateChannelSelection = new PrivateChannelSelection(chatService);
        model = new ChatModel();
        view = new ChatView(model,
                this,
                userProfileDisplay.getComboBox(),
                publicChannelSelection.getRoot(),
                privateChannelSelection.getRoot());

        model.getSortedChatMessages().setComparator(Comparator.naturalOrder());
    }

    @Override
    public void onViewAttached() {
        chatService.addDummyChannels();

        selectedChannelPin = chatService.getPersistableStore().getSelectedChannel().addObserver(selected -> {
            log.error("selected " + selected);
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
            chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem>bind(model.chatMessages)
                    .map(ChatMessageListItem::new)
                    .to(selected.getChatMessages());
        });
    }

    @Override
    public void onViewDetached() {
        selectedChannelPin.unbind();
        chatMessagesPin.unbind();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onSendMessage(String text) {
        Channel channel = chatService.getPersistableStore().getSelectedChannel().get();
        Identity identity = userProfileService.getPersistableStore().getSelectedUserProfile().get().identity();
        if (channel instanceof PublicChannel publicChannel) {
            sendMessageToPublicChannel(text, publicChannel, identity);
        } else if (channel instanceof PrivateChannel privateChannel) {
            sendMessageToPrivateChannel(text, privateChannel, identity);
        }
    }

    private void sendMessageToPublicChannel(String text, PublicChannel publicChannel, Identity identity) {
        log.error("sendMessageToPublicChannel");
        networkService.addData(new ChatMessage(publicChannel.getId(),
                        text,
                        identity.domainId(),
                        identity.networkId(),
                        new Date().getTime(),
                        PUBLIC),
                identity.getNodeIdAndKeyPair());
    }

    private void sendMessageToPrivateChannel(String text, PrivateChannel privateChannel, Identity identity) {
        String channelId = privateChannel.getId();
        ChatMessage chatMessage = new ChatMessage(channelId,
                text,
                identity.domainId(),
                identity.networkId(),
                new Date().getTime(),
                PRIVATE);
        chatService.addChatMessage(chatMessage, privateChannel);
        NetworkId receiverNetworkId = privateChannel.getChatPeer().networkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = identity.getNodeIdAndKeyPair();
        networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair)
                .whenComplete((resultMap, throwable2) -> {
                    if (throwable2 != null) {
                        UIThread.run(() -> model.setSendMessageError(channelId, throwable2));
                        return;
                    }
                    resultMap.forEach((transportType, res) -> {
                        ConfidentialMessageService.Result result = resultMap.get(transportType);
                        result.getMailboxFuture().values().forEach(broadcastFuture -> broadcastFuture
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

    public void onToggleSettings() {
        boolean sideBarVisible = !model.getSideBarVisible().get();
        model.getSideBarVisible().set(sideBarVisible);
    }
}
