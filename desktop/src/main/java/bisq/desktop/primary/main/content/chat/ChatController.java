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
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.PrivateGroupChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.chat.channels.TwoPartyPrivateChatChannelSelection;
import bisq.desktop.primary.main.content.chat.sidebar.ChannelSidebar;
import bisq.desktop.primary.main.content.chat.sidebar.UserProfileSidebar;
import bisq.desktop.primary.main.content.components.ChatMessagesComponent;
import bisq.desktop.primary.main.content.components.QuotedMessageBlock;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
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
    protected final TwoPartyPrivateChatChannelSelection twoPartyPrivateChatChannelSelection;
    protected final ChannelSidebar channelSidebar;
    protected final QuotedMessageBlock quotedMessageBlock;
    protected final ChatMessagesComponent chatMessagesComponent;
    protected Pin selectedChannelPin, twoPartyPrivateChatChannelsPin;
    private Subscription searchTextPin;

    public ChatController(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain, NavigationTarget host) {
        super(host);

        this.applicationService = applicationService;
        chatService = applicationService.getChatService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        reputationService = applicationService.getUserService().getReputationService();
        twoPartyPrivateChatChannelSelection = new TwoPartyPrivateChatChannelSelection(applicationService, chatChannelDomain);
        chatMessagesComponent = new ChatMessagesComponent(applicationService, chatChannelDomain);
        channelSidebar = new ChannelSidebar(applicationService, () -> {
            onCloseSideBar();
            chatMessagesComponent.resetSelectedChatMessage();
        });
        quotedMessageBlock = new QuotedMessageBlock(applicationService);

        createServices(chatChannelDomain);

        model = createAndGetModel(chatChannelDomain);
        view = createAndGetView();
    }

    public abstract void createServices(ChatChannelDomain chatChannelDomain);

    public abstract M createAndGetModel(ChatChannelDomain chatChannelDomain);

    public abstract V createAndGetView();

    @Override
    public void onActivate() {
        chatMessagesComponent.setOnShowChatUserDetails(userProfile -> {
            onCloseSideBar();
            model.getSideBarVisible().set(true);

            UserProfileSidebar userProfileSidebar = new UserProfileSidebar(userProfileService,
                    userIdentityService,
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

        model.getSearchText().set("");
        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                chatMessagesComponent.setSearchPredicate(item -> true);
            } else {
                chatMessagesComponent.setSearchPredicate(item -> item.match(searchText));
            }
        });

        ObservableArray<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels = chatService.getTwoPartyPrivateChatChannelServices().get(model.getChatChannelDomain()).getChannels();
        twoPartyPrivateChatChannelsPin = twoPartyPrivateChatChannels.addListener(() -> {
            model.getIsTwoPartyPrivateChatChannelSelectionVisible().set(!twoPartyPrivateChatChannels.isEmpty());
        });
    }

    @Override
    public void onDeactivate() {
        selectedChannelPin.unbind();
        searchTextPin.unsubscribe();
        twoPartyPrivateChatChannelsPin.unbind();
    }

    protected void chatChannelChanged(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
        UIThread.run(() -> {
            model.getSearchText().set("");

            model.getChannelTitle().set(chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(Objects.requireNonNull(chatChannel)))
                    .orElse(""));
            model.selectedChannelProperty().set(chatChannel);

            if (chatChannel == null) {
                doCloseSideBar();
            } else if (model.getChannelSidebarVisible().get()) {
                cleanupChannelInfo();
                showChannelInfo();
            }
        });
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelSidebarVisible().get();
        onCloseSideBar();
        chatMessagesComponent.resetSelectedChatMessage();
        model.getChannelSidebarVisible().set(visible);
        model.getSideBarVisible().set(visible);
        if (visible) {
            showChannelInfo();
        }
    }

    public void onToggleHelp() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_HELP);
    }

    public void onCloseSideBar() {
        doCloseSideBar();
    }

    public void showChannelInfo() {
        channelSidebar.setChannel(model.getSelectedChannel());
        channelSidebar.setOnUndoIgnoreChatUser(() -> {
            chatMessagesComponent.refreshMessages();
            channelSidebar.setChannel(model.getSelectedChannel());
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

    protected void applyPeersIcon(PrivateChatChannel<?> privateChatChannel) {
        if (privateChatChannel instanceof TwoPartyPrivateChatChannel) {
            TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = (TwoPartyPrivateChatChannel) privateChatChannel;
            Image image = RoboHash.getImage(twoPartyPrivateChatChannel.getPeer().getPubKeyHash());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(35);
            imageView.setFitHeight(35);
            model.getChannelIcon().set(BisqIconButton.createIconButton(imageView));
        } else if (privateChatChannel instanceof PrivateGroupChatChannel<?>) {
            PrivateGroupChatChannel<?> privateGroupChatChannel = (PrivateGroupChatChannel<?>) privateChatChannel;
            List<UserProfile> peers = privateGroupChatChannel.getPeers();
            //todo impl multiple icons
        }
    }

    protected void applyDefaultPublicChannelIcon(PublicChatChannel<?> channel) {
        String iconId = "channels-" + channel.getId().replace(".", "-");
        Button iconButton = BisqIconButton.createIconButton(iconId);
        //todo get larger icons and dont use scaling
        iconButton.setScaleX(1.25);
        iconButton.setScaleY(1.25);
        model.getChannelIcon().set(iconButton);
    }

    protected void doCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getSideBarWidth().set(0);
        model.getChannelSidebarVisible().set(false);
        model.getSideBarChanged().set(!model.getSideBarChanged().get());

        cleanupChatUserDetails();
        cleanupChannelInfo();
    }
}
