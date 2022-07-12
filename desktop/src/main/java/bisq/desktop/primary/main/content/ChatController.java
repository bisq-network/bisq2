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

package bisq.desktop.primary.main.content;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.components.*;
import bisq.settings.DontShowAgainService;
import bisq.chat.ChatService;
import bisq.chat.channel.Channel;
import bisq.chat.message.ChatMessage;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

import static bisq.settings.DontShowAgainKey.TRADE_GUIDE_BOX;

@Slf4j
public abstract class ChatController<V extends ChatView, M extends ChatModel> implements Controller {
    protected final ChatService chatService;
    protected final FilterBox filterBox;
    protected final M model;
    private final UserProfileService userProfileService;
    @Getter
    protected V view;
    protected final UserIdentityService userIdentityService;
    protected final DefaultApplicationService applicationService;
    protected final PrivateChannelSelection privateChannelSelection;
    protected final ChannelInfo channelInfo;
    protected final NotificationsSettings notificationsSettings;
    protected final HelpPane helpPane;
    protected final QuotedMessageBlock quotedMessageBlock;
    protected final ChatMessagesComponent chatMessagesComponent;
    protected Pin selectedChannelPin;
    protected Subscription notificationSettingSubscription;

    public ChatController(DefaultApplicationService applicationService, boolean isDiscussionsChat) {
        this.applicationService = applicationService;
        chatService = applicationService.getChatService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        ReputationService reputationService = applicationService.getUserService().getReputationService();
        privateChannelSelection = new PrivateChannelSelection(applicationService, isDiscussionsChat);
        chatMessagesComponent = new ChatMessagesComponent(applicationService, isDiscussionsChat);
        channelInfo = new ChannelInfo(applicationService);
        notificationsSettings = new NotificationsSettings();
        helpPane = new HelpPane();
        quotedMessageBlock = new QuotedMessageBlock(applicationService);

        //todo
        filterBox = new FilterBox(chatMessagesComponent.getFilteredChatMessages());
        createComponents();
        model = getChatModel(isDiscussionsChat);
        view = getChatView();
    }

    public abstract void createComponents();

    public abstract M getChatModel(boolean isDiscussionsChat);

    public abstract V getChatView();

    @Override
    public void onActivate() {
        chatMessagesComponent.setOnShowChatUserDetails(chatUser -> {
            onCloseSideBar();
            model.getSideBarVisible().set(true);
            model.getSideBarWidth().set(240);

            ChatUserDetails chatUserDetails = new ChatUserDetails(userProfileService, chatService, chatUser);
            chatUserDetails.setOnSendPrivateMessageHandler(chatMessagesComponent::openPrivateChannel);
            chatUserDetails.setIgnoreUserStateHandler(chatMessagesComponent::refreshMessages);
            chatUserDetails.setOnMentionUserHandler(chatMessagesComponent::mentionUser);
            model.setChatUserDetails(Optional.of(chatUserDetails));
            model.getChatUserDetailsRoot().set(chatUserDetails.getRoot());
        });
    }

    @Override
    public void onDeactivate() {
        if (notificationSettingSubscription != null) {
            notificationSettingSubscription.unsubscribe();
        }
        selectedChannelPin.unbind();
    }

    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        model.getSelectedChannelAsString().set(channel.getDisplayString());
        model.getSelectedChannel().set(channel);

        if (model.getChannelInfoVisible().get()) {
            cleanupChannelInfo();
            showChannelInfo();
        }
        
         /* 
            if (model.getNotificationsVisible().get()) {
                notificationsSettings.setChannel(channel);
            }*/
    }

    public void onToggleFilterBox() {
        boolean visible = !model.getFilterBoxVisible().get();
        model.getFilterBoxVisible().set(visible);
    }

    public void onToggleNotifications() {
        boolean visible = !model.getNotificationsVisible().get();
        onCloseSideBar();
        model.getNotificationsVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getSideBarWidth().set(visible ? 240 : 0);
        if (visible) {
            notificationsSettings.setChannel(model.getSelectedChannel().get());
        }
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelInfoVisible().get();
        onCloseSideBar();
        model.getChannelInfoVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getSideBarWidth().set(visible ? 240 : 0);
        if (visible) {
            showChannelInfo();
        }
    }

    public void onToggleHelp() {
        boolean visible = !model.getHelpVisible().get();
        // onCloseSideBar();
        model.getHelpVisible().set(visible);

        DontShowAgainService.putDontShowAgain(TRADE_GUIDE_BOX.name(), false);
        
     /*   model.getSideBarVisible().set(visible);
        model.getSideBarWidth().set(visible ? 540 : 0);
        if (visible) {
            //tradeChatHelp.setChannel();
        }*/
    }

    public void onCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getSideBarWidth().set(0);
        model.getChannelInfoVisible().set(false);
        model.getNotificationsVisible().set(false);
        model.getHelpVisible().set(false);

        cleanupChatUserDetails();
        cleanupChannelInfo();
    }

    public void onCreateOffer() {
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER);
    }

    protected void showChannelInfo() {
        channelInfo.setChannel(model.getSelectedChannel().get());
        channelInfo.setOnUndoIgnoreChatUser(() -> {
            chatMessagesComponent.refreshMessages();
            channelInfo.setChannel(model.getSelectedChannel().get());
        });
    }

    protected void cleanupChatUserDetails() {
        model.getChatUserDetails().ifPresent(e -> e.setOnMentionUserHandler(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnSendPrivateMessageHandler(null));
        model.getChatUserDetails().ifPresent(e -> e.setIgnoreUserStateHandler(null));
        model.setChatUserDetails(Optional.empty());
        model.getChatUserDetailsRoot().set(null);
    }

    protected void cleanupChannelInfo() {
        channelInfo.setOnUndoIgnoreChatUser(null);
    }
}
