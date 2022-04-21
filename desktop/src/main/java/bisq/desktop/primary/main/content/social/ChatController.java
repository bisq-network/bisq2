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

package bisq.desktop.primary.main.content.social;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.common.observable.Pin;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.social.components.*;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import bisq.social.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public abstract class ChatController<V extends ChatView, M extends ChatModel> implements Controller {
    protected final ChatService chatService;
    protected final FilterBox filterBox;
    protected final M model;
    @Getter
    protected V view;
    protected final UserProfileService userProfileService;
    protected final DefaultApplicationService applicationService;
    protected final PrivateChannelSelection privateChannelSelection;
    protected final ChannelInfo channelInfo;
    protected final NotificationsSettings notificationsSettings;
    protected final QuotedMessageBlock quotedMessageBlock;
    protected final UserProfileSelection userProfileSelection;
    protected final ChatMessagesComponent chatMessagesComponent;
    protected Pin selectedChannelPin;
    protected Subscription notificationSettingSubscription;

    public ChatController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        chatService = applicationService.getChatService();
        userProfileService = applicationService.getUserProfileService();

        userProfileSelection = new UserProfileSelection(userProfileService);
        privateChannelSelection = new PrivateChannelSelection(applicationService);
     
        chatMessagesComponent = new ChatMessagesComponent(chatService, userProfileService);
        channelInfo = new ChannelInfo(chatService);
        notificationsSettings = new NotificationsSettings();
        quotedMessageBlock = new QuotedMessageBlock();

        //todo
        filterBox = new FilterBox(chatMessagesComponent.getFilteredChatMessages());

        model = getChatModel();
        view = getChatView();
    }

    public abstract M getChatModel();

    public abstract V getChatView();

    @Override
    public void onActivate() {
        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> chatService.setNotificationSetting(chatService.getSelectedChannel().get(), value));

        chatMessagesComponent.setOnShowChatUserDetails(chatUser -> {
            model.getSideBarVisible().set(true);
            model.getChannelInfoVisible().set(false);
            model.getNotificationsVisible().set(false);

            ChatUserDetails chatUserDetails = new ChatUserDetails(chatService, chatUser);
            chatUserDetails.setOnSendPrivateMessage(chatMessagesComponent::openPrivateChannel);
            chatUserDetails.setOnIgnoreChatUser(chatMessagesComponent::refreshMessages);
            chatUserDetails.setOnMentionUser(chatMessagesComponent::mentionUser);
            model.setChatUserDetails(Optional.of(chatUserDetails));
            model.getChatUserDetailsRoot().set(chatUserDetails.getRoot());
        });

        selectedChannelPin = chatService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        model.getSelectedChannelAsString().set(channel.getDisplayString());
        model.getSelectedChannel().set(channel);

        if (channel instanceof PrivateChannel privateChannel) {
            model.getPeersRoboIconImage().set(RoboHash.getImage(new ByteArray(privateChannel.getPeer().getPubKeyHash())));
            model.getPeersRoboIconVisible().set(true);
        } else {
            model.getPeersRoboIconVisible().set(false);
        }

        if (model.getChannelInfoVisible().get()) {
            cleanupChannelInfo();
            showChannelInfo();
        }
        
         /* 
            if (model.getNotificationsVisible().get()) {
                notificationsSettings.setChannel(channel);
            }*/
    }

    @Override
    public void onDeactivate() {
        notificationSettingSubscription.unsubscribe();
        selectedChannelPin.unbind();
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
        cleanupChatUserDetails();
        cleanupChannelInfo();
        if (visible) {
            notificationsSettings.setChannel(model.getSelectedChannel().get());
        }
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelInfoVisible().get();
        model.getChannelInfoVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getNotificationsVisible().set(false);
        cleanupChatUserDetails();
        if (visible) {
            showChannelInfo();
        } else {
            cleanupChannelInfo();
        }
    }

    protected void showChannelInfo() {
        channelInfo.setChannel(model.getSelectedChannel().get());
        channelInfo.setOnUndoIgnoreChatUser(() -> {
            chatMessagesComponent.refreshMessages();
            channelInfo.setChannel(model.getSelectedChannel().get());
        });
    }

    public void onCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getChannelInfoVisible().set(false);
        model.getNotificationsVisible().set(false);
        cleanupChatUserDetails();
        cleanupChannelInfo();
    }
    public void onCreateOffer() {
        //todo
        //Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
    }
    protected void cleanupChatUserDetails() {
        model.getChatUserDetails().ifPresent(e -> e.setOnMentionUser(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnSendPrivateMessage(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnIgnoreChatUser(null));
        model.setChatUserDetails(Optional.empty());
        model.getChatUserDetailsRoot().set(null);
    }

    protected void cleanupChannelInfo() {
        channelInfo.setOnUndoIgnoreChatUser(null);
    }
}
