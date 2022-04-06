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
import bisq.common.observable.ObservableSet;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.Notification;
import bisq.desktop.primary.main.content.social.chat.components.*;
import bisq.i18n.Res;
import bisq.social.chat.*;
import bisq.social.user.ChatUser;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import javafx.collections.ListChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ChatController implements Controller {
    private final ChatService chatService;
    private final ChatModel model;
    @Getter
    private final ChatView view;
    private final UserProfileService userProfileService;
    private final PublicChannelSelection publicChannelSelection;
    private final PrivateChannelSelection privateChannelSelection;
    private final ChannelInfo channelInfo;
    private final NotificationsSettings notificationsSettings;
    private final QuotedMessageBlock quotedMessageBlock;
    private Pin chatMessagesPin, selectedChannelPin, tradeTagsPin, currencyTagsPin, paymentMethodTagsPin, customTagsPin;

    private Subscription notificationSettingSubscription;
    private ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messageListener;

    public ChatController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        userProfileService = applicationService.getUserProfileService();

        channelInfo = new ChannelInfo(chatService);
        notificationsSettings = new NotificationsSettings();
        UserProfileComboBox userProfileDisplay = new UserProfileComboBox(userProfileService);
        publicChannelSelection = new PublicChannelSelection(chatService);
        privateChannelSelection = new PrivateChannelSelection(chatService);
        quotedMessageBlock = new QuotedMessageBlock();
        model = new ChatModel(chatService, userProfileService);
        view = new ChatView(model,
                this,
                userProfileDisplay.getComboBox(),
                publicChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot(),
                quotedMessageBlock.getRoot());

        model.getSortedChatMessages().setComparator(ChatMessageListItem::compareTo);
    }

    @Override
    public void onActivate() {
        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> chatService.setNotificationSetting(chatService.getPersistableStore().getSelectedChannel().get(), value));

        selectedChannelPin = chatService.getPersistableStore().getSelectedChannel().addObserver(channel -> {
            if (channel instanceof PublicChannel publicChannel) {
                tradeTagsPin = FxBindings.<String, String>bind(model.getTradeTags()).map(String::toUpperCase).to(publicChannel.getTradeTags());
                currencyTagsPin = FxBindings.<String, String>bind(model.getCurrencyTags()).map(String::toUpperCase).to(publicChannel.getCurrencyTags());
                paymentMethodTagsPin = FxBindings.<String, String>bind(model.getPaymentMethodsTags()).map(String::toUpperCase).to(publicChannel.getPaymentMethodTags());
                customTagsPin = FxBindings.<String, String>bind(model.getCustomTags()).map(String::toUpperCase).to(publicChannel.getCustomTags());
            }

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
            chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.getChatMessages())
                    .map(chatMessage -> {
                        if (chatMessage instanceof PrivateChatMessage privateChatMessage) {
                            return new ChatMessageListItem<>(privateChatMessage);
                        } else if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                            return new ChatMessageListItem<>(publicChatMessage);
                        } else {
                            throw new RuntimeException("ChatMessage has unexpected type. chatMessage=" + chatMessage);
                        }
                    })
                    .to((ObservableSet<ChatMessage>) channel.getChatMessages()); //todo expected type <? extends ChatMessage> does not work ;-(

            model.getSelectedChannelAsString().set(channel.getChannelName());
            model.getSelectedChannel().set(channel);
            if (model.getChannelInfoVisible().get()) {
                channelInfo.setChannel(channel);
                channelInfo.setOnUndoIgnoreChatUser(() -> {
                    refreshMessages();
                    channelInfo.setChannel(channel);
                });
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
                    .map(userProfile -> userProfile.chatUser().getUserName()) // UserNameGenerator.fromHash(userProfile.chatUser().getPubKeyHash()))
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
                                    .map(item -> item.getAuthorUserName() + ": " + item.getChatMessage().getText())
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
                                    .filter(item -> !myUserNames.contains(item.getAuthorUserName()))
                                    .map(item -> Res.get("social.notification.getMentioned",
                                            item.getAuthorUserName(),
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
    public void onDeactivate() {
        notificationSettingSubscription.unsubscribe();
        selectedChannelPin.unbind();
        chatMessagesPin.unbind();

        if (messageListener != null) {
            model.getChatMessages().removeListener(messageListener);
        }

        if (tradeTagsPin != null) {
            tradeTagsPin.unbind();
            currencyTagsPin.unbind();
            paymentMethodTagsPin.unbind();
            customTagsPin.unbind();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onSendMessage(String text) {
        Channel<? extends ChatMessage> channel = chatService.getPersistableStore().getSelectedChannel().get();
        UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
        if (channel instanceof PublicChannel publicChannel) {
            chatService.publishPublicChatMessage(text, quotedMessageBlock.getQuotedMessage(), publicChannel, userProfile);
        } else if (channel instanceof PrivateChannel privateChannel) {
            chatService.sendPrivateChatMessage(text, quotedMessageBlock.getQuotedMessage(), privateChannel);
        }
        quotedMessageBlock.close();
    }

    public void onToggleFilterBox() {
        boolean visible = !model.getFilterBoxVisible().get();
        model.getFilterBoxVisible().set(visible);
    }

    public void onToggleNotifications() {
        boolean visible = !model.getNotificationsVisible().get();
        model.getNotificationsVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getChannelInfoVisible().set(false);
        removeChatUserDetails();
        if (visible) {
            notificationsSettings.setChannel(model.getSelectedChannel().get());
        }
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelInfoVisible().get();
        model.getChannelInfoVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getNotificationsVisible().set(false);
        removeChatUserDetails();
        if (visible) {
            channelInfo.setChannel(model.getSelectedChannel().get());
            channelInfo.setOnUndoIgnoreChatUser(() -> {
                refreshMessages();
                channelInfo.setChannel(model.getSelectedChannel().get());
            });
        }
    }

    public void onShowChatUserDetails(ChatMessage chatMessage) {
        model.getSideBarVisible().set(true);
        model.getChannelInfoVisible().set(false);
        model.getNotificationsVisible().set(false);

        ChatUserDetails chatUserDetails = new ChatUserDetails(model.getChatService(), chatMessage.getAuthor());
        chatUserDetails.setOnSendPrivateMessage(chatUser -> {
            // todo
            log.info("onSendPrivateMessage {}", chatUser);
        });
        chatUserDetails.setOnIgnoreChatUser(this::refreshMessages);
        chatUserDetails.setOnMentionUser(chatUser -> mentionUser(chatUser.getUserName()));
        model.setChatUserDetails(Optional.of(chatUserDetails));
        model.getChatUserDetailsRoot().set(chatUserDetails.getRoot());
    }

    public void onUserNameClicked(String userName) {
        mentionUser(userName);
    }

    public void onCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getChannelInfoVisible().set(false);
        model.getNotificationsVisible().set(false);
        removeChatUserDetails();
    }

    private void removeChatUserDetails() {
        model.getChatUserDetails().ifPresent(e -> e.setOnMentionUser(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnSendPrivateMessage(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnIgnoreChatUser(null));
        model.setChatUserDetails(Optional.empty());
        model.getChatUserDetailsRoot().set(null);
    }

    private void mentionUser(String userName) {
        String existingText = model.getTextInput().get();
        if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
            existingText += " ";
        }
        model.getTextInput().set(existingText + "@" + userName + " ");
    }

    private void refreshMessages() {
        chatMessagesPin.unbind();
        chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.getChatMessages())
                .map(ChatMessageListItem::new)
                .to((ObservableSet<ChatMessage>) model.getSelectedChannel().get().getChatMessages()); //todo expected type <? extends ChatMessage> does not work ;-(
    }

    public void onOpenEmojiSelector(ChatMessage chatMessage) {

    }

    public void onReply(ChatMessage chatMessage) {
        if (!chatService.isMyMessage(chatMessage)) {
            quotedMessageBlock.reply(chatMessage);
        }
    }

    /**
     * open a private channel to specified user. Automatically select it so user is ready to type the message to send.
     *
     * @param chatMessage
     */
    public void onOpenPrivateChannel(ChatMessage chatMessage) {
        if (chatService.isMyMessage(chatMessage)) {
            return; // should never happen as the button for opening the channel should not appear
            // but kept here for double safety
        }
        ChatUser peer = chatMessage.getAuthor();
        String channelId = PrivateChannel.createChannelId(peer, userProfileService.getPersistableStore().getSelectedUserProfile().get());
        PrivateChannel channel = chatService.getOrCreatePrivateChannel(channelId, peer);
        chatService.selectChannel(channel);
    }

    public void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
        if (!chatService.isMyMessage(chatMessage)) {
            return;
        }
        if (chatMessage instanceof PublicChatMessage publicChatMessage) {
            UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
            chatService.publishEditedPublicChatMessage(publicChatMessage, editedText, userProfile)
                    .whenComplete((r, t) -> {
                        // todo maybe show spinner while deleting old msg and hide it once done?
                    });
        } else {
            //todo private message
        }
    }

    public void onDeleteMessage(ChatMessage chatMessage) {
        if (chatService.isMyMessage(chatMessage)) {
            if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
                chatService.deletePublicChatMessage(publicChatMessage, userProfile);
            } else {
                //todo delete private message
            }
        }
    }

    public void onOpenMoreOptions(ChatMessage chatMessage) {

    }

    public void onAddEmoji(String emojiId) {

    }
}
