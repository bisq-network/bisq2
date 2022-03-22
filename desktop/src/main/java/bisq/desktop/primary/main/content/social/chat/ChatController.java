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
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.Notification;
import bisq.desktop.primary.main.content.social.chat.components.*;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.social.chat.*;
import bisq.social.user.UserNameGenerator;
import bisq.social.user.ChatUser;
import bisq.social.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import javafx.collections.ListChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ChannelInfo channelInfo;
    private final NotificationsSettings notificationsSettings;
    private Pin chatMessagesPin;
    private Pin selectedChannelPin;
    private Subscription notificationSettingSubscription;
    private ListChangeListener<ChatMessageListItem> messageListener;

    public ChatController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        userProfileService = applicationService.getUserProfileService();
        networkService = applicationService.getNetworkService();

        channelInfo = new ChannelInfo();
        notificationsSettings = new NotificationsSettings();
        UserProfileComboBox userProfileDisplay = new UserProfileComboBox(userProfileService);
        publicChannelSelection = new PublicChannelSelection(chatService);
        privateChannelSelection = new PrivateChannelSelection(chatService);
        model = new ChatModel();
        view = new ChatView(model,
                this,
                userProfileDisplay.getComboBox(),
                publicChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot());

        model.getSortedChatMessages().setComparator(Comparator.naturalOrder());
    }

    @Override
    public void onViewAttached() {
        chatService.addDummyChannels();

        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> chatService.setNotificationSetting(chatService.getPersistableStore().getSelectedChannel().get(), value));

        selectedChannelPin = chatService.getPersistableStore().getSelectedChannel().addObserver(channel -> {
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
            chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem>bind(model.getChatMessages())
                    .map(ChatMessageListItem::new)
                    .to(channel.getChatMessages());

            model.getSelectedChannelAsString().set(channel.getChannelName());
            model.getSelectedChannel().set(channel);
            if (model.getInfoVisible().get()) {
                channelInfo.setChannel(channel);
            }
            if (model.getNotificationsVisible().get()) {
                notificationsSettings.setChannel(channel);
            }

            if (messageListener != null) {
                model.getChatMessages().removeListener(messageListener);
            }

            // Notifications implementation is very preliminary. Not sure if another concept like its used in Element
            // would be better. E.g. Show all past notifications in the sidebar. When a new notification arrives, dont
            // show the popup but only highlight the notifications icon (we would need to add a notification
            // settings tab then in the notifications component).
            // We look up all our usernames, not only the selected one
            Set<String> myUserNames = userProfileService.getPersistableStore().getUserProfiles().stream()
                    .map(userProfile -> UserNameGenerator.fromHash(userProfile.identity().pubKeyHash()))
                    .collect(Collectors.toSet());

            messageListener = c -> {
                c.next();
                // At init, we get full list, but we don't want to show notifications in that event.
                if (c.getAddedSubList().equals(model.getChatMessages())) {
                    return;
                }
                if (channel.getNotificationSetting().get() == NotificationSetting.ALL) {
                    String messages = Joiner.on("\n").join(
                            c.getAddedSubList().stream()
                                    .map(item -> item.getSenderUserName() + ": " + item.getChatMessage().getText())
                                    .collect(Collectors.toSet()));
                    if (!messages.isEmpty()) {
                        new Notification().headLine(messages).autoClose().hideCloseButton().show();
                    }
                } else if (channel.getNotificationSetting().get() == NotificationSetting.MENTION) {
                    // TODO
                    // - Match only if mentioned username matches exactly (e.g. split item.getMessage()
                    // in space separated tokens and compare those)
                    // - show user icon of sender (requires extending Notification to support custom graphics)
                    //
                    String messages = Joiner.on("\n").join(
                            c.getAddedSubList().stream()
                                    .filter(item -> myUserNames.stream().anyMatch(myUserName -> item.getMessage().contains("@" + myUserName)))
                                    .filter(item -> !myUserNames.contains(item.getSenderUserName()))
                                    .map(item -> Res.get("social.notification.getMentioned",
                                            item.getSenderUserName(),
                                            item.getChatMessage().getText()))
                                    .collect(Collectors.toSet()));
                    if (!messages.isEmpty()) {
                        new Notification().headLine(messages).autoClose().hideCloseButton().show();
                    }
                }
            };
            model.getChatMessages().addListener(messageListener);
        });
    }

    @Override
    public void onViewDetached() {
        notificationSettingSubscription.unsubscribe();
        selectedChannelPin.unbind();
        chatMessagesPin.unbind();

        if (messageListener != null) {
            model.getChatMessages().removeListener(messageListener);
        }
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
        NetworkId receiverNetworkId = privateChannel.getPeer().networkId();
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

    public void onToggleFilterBox() {
        boolean visible = !model.getFilterBoxVisible().get();
        model.getFilterBoxVisible().set(visible);
    }

    public void onToggleNotifications() {
        boolean visible = !model.getNotificationsVisible().get();
        model.getNotificationsVisible().set(visible);
        model.getInfoVisible().set(false);
        if (visible) {
            notificationsSettings.setChannel(model.getSelectedChannel().get());
        }
    }

    public void onToggleInfo() {
        boolean visible = !model.getInfoVisible().get();
        model.getInfoVisible().set(visible);
        model.getNotificationsVisible().set(false);
        if (visible) {
            channelInfo.setChannel(model.getSelectedChannel().get());
        }
    }

    public void openPrivateChannel(String peerUserName, NetworkId peerNetworkId) {
        Identity myIdentity = userProfileService.getPersistableStore().getSelectedUserProfile().get().identity();
        ChatUser peerUser = new ChatUser(peerNetworkId);

        String whatsMyName = myIdentity.domainId();
        ChatIdentity meChatting = new ChatIdentity(whatsMyName, myIdentity);
        String channelId;// here in chat-land we need a channel id which is unique here and for the peer.
        if (peerUserName.compareTo(whatsMyName)<0) {
            channelId = "PC@"+peerUserName+"$"+whatsMyName;
        } else { // change order by lexi, so that the channelId is unique for all two users.
            channelId = "PC@"+whatsMyName+"$"+peerUserName;
        }

        PrivateChannel privateChannel = chatService.getOrCreatePrivateChannel(channelId, peerUser, meChatting);
        // display the channel and add to the list of private channels.
        chatService.selectChannel(privateChannel);
    }
}
