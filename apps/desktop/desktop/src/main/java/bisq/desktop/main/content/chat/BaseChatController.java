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

package bisq.desktop.main.content.chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.desktop.main.content.chat.message_container.sidebar.ChannelSidebar;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class BaseChatController<V extends BaseChatView, M extends BaseChatModel> extends NavigationController {
    @Getter
    protected final M model;
    @Getter
    protected final V view;
    protected final ServiceProvider serviceProvider;
    protected final ChatService chatService;
    protected final ChatChannelSelectionService selectionService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChannelSidebar channelSidebar;
    protected final ChatMessageContainerController chatMessageContainerController;
    protected Subscription searchTextPin;
    protected Pin selectedChannelPin, selectedNotificationSettingPin;

    public BaseChatController(ServiceProvider serviceProvider,
                              ChatChannelDomain chatChannelDomain,
                              NavigationTarget host) {
        super(host);

        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        selectionService = chatService.getChatChannelSelectionService(chatChannelDomain);
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider,
                chatChannelDomain,
                this::openProfileCard);

        channelSidebar = new ChannelSidebar(serviceProvider,
                () -> {
                    doCloseSideBar();
                    chatMessageContainerController.resetSelectedChatMessage();
                },
                this::openProfileCard);

        createDependencies(chatChannelDomain);

        model = createAndGetModel(chatChannelDomain);
        view = createAndGetView();
    }

    @Override
    public void onActivate() {
        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
        model.getSearchText().set("");
        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                chatMessageContainerController.setSearchPredicate(item -> true);
            } else {
                chatMessageContainerController.setSearchPredicate(item -> item.match(searchText));
            }
        });
    }

    @Override
    public void onDeactivate() {
        selectedChannelPin.unbind();
        searchTextPin.unsubscribe();
        if (selectedNotificationSettingPin != null) {
            selectedNotificationSettingPin.unbind();
        }
    }

    public void createDependencies(ChatChannelDomain chatChannelDomain) {
    }

    public abstract M createAndGetModel(ChatChannelDomain chatChannelDomain);

    public abstract V createAndGetView();

    protected void openProfileCard(UserProfile userProfile) {
//        doCloseSideBar();
//        model.getSideBarVisible().set(true);

//        UserProfileSidebar userProfileSidebar = new UserProfileSidebar(serviceProvider,
//                userProfile,
//                model.getSelectedChannel(),
//                () -> {
//                    doCloseSideBar();
//                    chatMessageContainerController.resetSelectedChatMessage();
//                });
//        model.getSideBarWidth().set(userProfileSidebar.getRoot().getMinWidth());
//        userProfileSidebar.setOnSendPrivateMessageHandler(chatMessageContainerController::createAndSelectTwoPartyPrivateChatChannel);
//        userProfileSidebar.setIgnoreUserStateHandler(chatMessageContainerController::refreshMessages);
//        userProfileSidebar.setOnMentionUserHandler(chatMessageContainerController::mentionUser);
//        model.setChatUserDetails(Optional.of(userProfileSidebar));
//        model.getChatUserDetailsRoot().set(userProfileSidebar.getRoot());

        cleanupChatUserDetails();
        cleanupChannelInfo();

        Navigation.navigateTo(NavigationTarget.PROFILE_CARD,
                new ProfileCardController.InitData(
                        userProfile,
                        model.getSelectedChannel(),
                        chatMessageContainerController::refreshMessages,
                        () -> {
                            cleanupChatUserDetails();
                            cleanupChannelInfo();
                            chatMessageContainerController.resetSelectedChatMessage();
                        }));
    }

    protected void selectedChannelChanged(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
        UIThread.run(() -> {
            model.selectedChannelProperty().set(chatChannel);

            if (chatChannel == null) {
                doCloseSideBar();
            } else {
                model.getChannelTitle().set(chatService.findChatChannelService(chatChannel)
                        .map(service -> service.getChannelTitle(Objects.requireNonNull(chatChannel)))
                        .orElse(""));
                model.getChannelIconId().set(ChatUtil.getChannelIconId(chatChannel.getId()));

                if (model.getChannelSidebarVisible().get()) {
                    cleanupChannelInfo();
                    showChannelInfo();
                }

                if (chatChannel instanceof CommonPublicChatChannel) {
                    model.getChannelDescription().set(((CommonPublicChatChannel) chatChannel).getDescription());
                }

                selectedNotificationSettingPin = FxBindings.bind(model.getSelectedNotificationSetting())
                        .to(chatChannel.getChatChannelNotificationType());
            }
        });
    }

    protected void onToggleChannelInfo() {
        boolean visible = !model.getChannelSidebarVisible().get();
        doCloseSideBar();
        chatMessageContainerController.resetSelectedChatMessage();
        model.getChannelSidebarVisible().set(visible);
        model.getSideBarVisible().set(visible);
        if (visible) {
            showChannelInfo();
        }
    }

    protected void onOpenHelp() {
        switch (model.chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
                Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
                break;
            case DISCUSSION:
            case SUPPORT:
                Navigation.navigateTo(NavigationTarget.CHAT_RULES);
                break;
        }
    }

    String getHelpButtonText() {
        return switch (model.chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK,
                 BISQ_EASY_OPEN_TRADES -> Res.get("chat.ellipsisMenu.tradeGuide");
            default -> Res.get("chat.ellipsisMenu.chatRules");
        };
    }

    private void doCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getSideBarWidth().set(0);
        model.getChannelSidebarVisible().set(false);
        model.getSideBarChanged().set(!model.getSideBarChanged().get());

        cleanupChatUserDetails();
        cleanupChannelInfo();
    }

    private void showChannelInfo() {
        channelSidebar.setChannel(model.getSelectedChannel());
        channelSidebar.setOnUndoIgnoreChatUser(() -> {
            chatMessageContainerController.refreshMessages();
            channelSidebar.setChannel(model.getSelectedChannel());
        });
    }

    private void cleanupChatUserDetails() {
        model.getChatUserDetails().ifPresent(e -> e.setOnMentionUserHandler(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnSendPrivateMessageHandler(null));
        model.getChatUserDetails().ifPresent(e -> e.setIgnoreUserStateHandler(null));
        model.setChatUserDetails(Optional.empty());
        model.getChatUserDetailsRoot().set(null);
    }

    private void cleanupChannelInfo() {
        channelSidebar.setOnUndoIgnoreChatUser(null);
    }

    void onSetNotificationType(ChatChannelNotificationType type) {
        model.getSelectedNotificationSetting().set(type);
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        if (chatChannel != null) {
            chatService.findChatChannelService(chatChannel)
                    .ifPresent(service -> service.setChatChannelNotificationType(chatChannel, type));
        }
    }
}
