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

package bisq.desktop.primary.main.content.chat;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.channel.BasePrivateChannel;
import bisq.chat.channel.BasePublicChannel;
import bisq.chat.channel.Channel;
import bisq.chat.channel.ChannelDomain;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.chat.channels.PrivateChannelSelection;
import bisq.desktop.primary.main.content.chat.sidebar.ChannelSidebar;
import bisq.desktop.primary.main.content.chat.sidebar.UserProfileSidebar;
import bisq.desktop.primary.main.content.components.ChatMessagesComponent;
import bisq.desktop.primary.main.content.components.QuotedMessageBlock;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;


@Slf4j
public abstract class ChatController<V extends ChatView, M extends ChatModel> extends NavigationController {
    protected final ChatService chatService;
    @Getter
    protected final M model;
    protected final UserProfileService userProfileService;
    private final ReputationService reputationService;
    @Getter
    protected V view;
    protected final UserIdentityService userIdentityService;
    protected final DefaultApplicationService applicationService;
    protected final PrivateChannelSelection privateChannelSelection;
    protected final ChannelSidebar channelSidebar;
    protected final QuotedMessageBlock quotedMessageBlock;
    protected final ChatMessagesComponent chatMessagesComponent;
    protected Pin selectedChannelPin;
    private Subscription searchTextPin;

    public ChatController(DefaultApplicationService applicationService, ChannelDomain channelDomain, NavigationTarget host) {
        super(host);

        this.applicationService = applicationService;
        chatService = applicationService.getChatService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        reputationService = applicationService.getUserService().getReputationService();
        privateChannelSelection = new PrivateChannelSelection(applicationService, channelDomain);
        chatMessagesComponent = new ChatMessagesComponent(applicationService, channelDomain);
        channelSidebar = new ChannelSidebar(applicationService, () -> {
            onCloseSideBar();
            chatMessagesComponent.resetSelectedChatMessage();
        });
        quotedMessageBlock = new QuotedMessageBlock(applicationService);

        createComponents();
        model = getChatModel(channelDomain);
        view = getChatView();
    }

    public abstract void createComponents();

    public abstract M getChatModel(ChannelDomain channelDomain);

    public abstract V getChatView();

    @Override
    public void onActivate() {
        chatMessagesComponent.setOnShowChatUserDetails(userProfile -> {
            onCloseSideBar();
            model.getSideBarVisible().set(true);

            UserProfileSidebar userProfileSidebar = new UserProfileSidebar(userProfileService,
                    chatService,
                    reputationService,
                    userProfile,
                    () -> {
                        onCloseSideBar();
                        chatMessagesComponent.resetSelectedChatMessage();
                    });
            model.getSideBarWidth().set(userProfileSidebar.getRoot().getMinWidth());
            userProfileSidebar.setOnSendPrivateMessageHandler(chatMessagesComponent::openPrivateChannel);
            userProfileSidebar.setIgnoreUserStateHandler(chatMessagesComponent::refreshMessages);
            userProfileSidebar.setOnMentionUserHandler(chatMessagesComponent::mentionUser);
            model.setChatUserDetails(Optional.of(userProfileSidebar));
            model.getChatUserDetailsRoot().set(userProfileSidebar.getRoot());
        });

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                chatMessagesComponent.setSearchPredicate(item -> true);
            } else {
                chatMessagesComponent.setSearchPredicate(item -> item.match(searchText));
            }
        });
    }

    @Override
    public void onDeactivate() {
        selectedChannelPin.unbind();
        searchTextPin.unsubscribe();
    }

    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        UIThread.run(() -> {
            model.getSelectedChannelAsString().set(channel != null ? channel.getDisplayString() : "");
            model.getSelectedChannel().set(channel);

            if (model.getChannelInfoVisible().get()) {
                cleanupChannelInfo();
                showChannelInfo();
            }
        
         /* 
            if (model.getNotificationsVisible().get()) {
                notificationsSettings.setChannel(channel);
            }*/
        });
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelInfoVisible().get();
        onCloseSideBar();
        chatMessagesComponent.resetSelectedChatMessage();
        model.getChannelInfoVisible().set(visible);
        model.getSideBarVisible().set(visible);
        if (visible) {
            showChannelInfo();
        }
    }

    public void onToggleHelp() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_HELP);
    }

    public void onCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getSideBarWidth().set(0);
        model.getChannelInfoVisible().set(false);
        model.getSideBarChanged().set(!model.getSideBarChanged().get());

        cleanupChatUserDetails();
        cleanupChannelInfo();
    }

    public void showChannelInfo() {
        channelSidebar.setChannel(model.getSelectedChannel().get());
        channelSidebar.setOnUndoIgnoreChatUser(() -> {
            chatMessagesComponent.refreshMessages();
            channelSidebar.setChannel(model.getSelectedChannel().get());
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
        channelSidebar.setOnUndoIgnoreChatUser(null);
    }

    protected void applyPeersIcon(BasePrivateChannel<?> privateChannel) {
        Image image = RoboHash.getImage(privateChannel.getPeer().getPubKeyHash());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(35);
        imageView.setFitHeight(35);
        model.getChannelIcon().set(BisqIconButton.createIconButton(imageView));
    }

    protected void applyDefaultPublicChannelIcon(BasePublicChannel<?> channel) {
        String domain = "-" + channel.getChannelDomain().name().toLowerCase() + "-";
        String iconId = "channels" + domain + channel.getChannelName();
        Button iconButton = BisqIconButton.createIconButton(iconId);
        //todo get larger icons and dont use scaling
        iconButton.setScaleX(1.25);
        iconButton.setScaleY(1.25);
        model.getChannelIcon().set(iconButton);
    }
}
