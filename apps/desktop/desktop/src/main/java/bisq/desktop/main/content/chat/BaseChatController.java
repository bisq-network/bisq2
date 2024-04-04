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
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.desktop.main.content.chat.sidebar.ChannelSidebar;
import bisq.desktop.main.content.chat.sidebar.UserProfileSidebar;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class BaseChatController<V extends BaseChatView, M extends BaseChatModel> extends NavigationController {
    @Getter
    protected final M model;
    @Getter
    protected final V view;
    protected final ServiceProvider serviceProvider;
    protected final ChatService chatService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChannelSidebar channelSidebar;
    protected final ChatMessageContainerController chatMessageContainerController;
    protected Subscription searchTextPin;

    public BaseChatController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget host) {
        super(host);

        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider,
                chatChannelDomain,
                this::openUserProfileSidebar);
        channelSidebar = new ChannelSidebar(serviceProvider,
                () -> {
                    doCloseSideBar();
                    chatMessageContainerController.resetSelectedChatMessage();
                },
                this::openUserProfileSidebar);

        createDependencies(chatChannelDomain);

        model = createAndGetModel(chatChannelDomain);
        view = createAndGetView();
    }

    public abstract void createDependencies(ChatChannelDomain chatChannelDomain);

    public abstract M createAndGetModel(ChatChannelDomain chatChannelDomain);

    public abstract V createAndGetView();

    protected void openUserProfileSidebar(UserProfile userProfile) {
        doCloseSideBar();
        model.getSideBarVisible().set(true);

        UserProfileSidebar userProfileSidebar = new UserProfileSidebar(serviceProvider,
                userProfile,
                model.getSelectedChannel(),
                () -> {
                    doCloseSideBar();
                    chatMessageContainerController.resetSelectedChatMessage();
                });
        model.getSideBarWidth().set(userProfileSidebar.getRoot().getMinWidth());
        userProfileSidebar.setOnSendPrivateMessageHandler(chatMessageContainerController::createAndSelectTwoPartyPrivateChatChannel);
        userProfileSidebar.setIgnoreUserStateHandler(chatMessageContainerController::refreshMessages);
        userProfileSidebar.setOnMentionUserHandler(chatMessageContainerController::mentionUser);
        model.setChatUserDetails(Optional.of(userProfileSidebar));
        model.getChatUserDetailsRoot().set(userProfileSidebar.getRoot());
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
            }
        });
    }

    protected void applyPeersIcon(PrivateChatChannel<?> privateChatChannel) {
        if (privateChatChannel instanceof TwoPartyPrivateChatChannel) {
            TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = (TwoPartyPrivateChatChannel) privateChatChannel;
            Image image = CatHash.getImage(twoPartyPrivateChatChannel.getPeer());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(35);
            imageView.setFitHeight(35);
            Button iconButton = BisqIconButton.createIconButton(imageView);
            model.getChannelIconNode().set(iconButton);
        } else if (privateChatChannel instanceof BisqEasyOpenTradeChannel) {
            BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel = (BisqEasyOpenTradeChannel) privateChatChannel;
            if (bisqEasyOpenTradeChannel.isInMediation() && bisqEasyOpenTradeChannel.getMediator().isPresent()) {
                UserProfile left;
                UserProfile right;
                if (bisqEasyOpenTradeChannel.isMediator()) {
                    List<UserProfile> traders = new ArrayList<>(bisqEasyOpenTradeChannel.getTraders());
                    checkArgument(traders.size() == 2);
                    left = traders.get(0);
                    right = traders.get(1);
                } else {
                    left = bisqEasyOpenTradeChannel.getPeer();
                    right = bisqEasyOpenTradeChannel.getMediator().get();
                }
                ImageView leftImageView = new ImageView(CatHash.getImage(left));
                leftImageView.setFitWidth(35);
                leftImageView.setFitHeight(35);
                Button leftIconButton = BisqIconButton.createIconButton(leftImageView);
                leftIconButton.setMouseTransparent(true);

                ImageView rightImageView = new ImageView(CatHash.getImage(right));
                rightImageView.setFitWidth(35);
                rightImageView.setFitHeight(35);
                Button rightIconButton = BisqIconButton.createIconButton(rightImageView);
                rightIconButton.setMouseTransparent(true);
                HBox.setMargin(rightIconButton, new Insets(0, 0, 0, -20));

                HBox hBox = new HBox(10, leftIconButton, rightIconButton);
                hBox.setAlignment(Pos.CENTER_LEFT);
                model.getChannelIconNode().set(hBox);
            } else {
                Image image = CatHash.getImage(bisqEasyOpenTradeChannel.getPeer());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(35);
                imageView.setFitHeight(35);
                Button iconButton = BisqIconButton.createIconButton(imageView);
                model.getChannelIconNode().set(iconButton);
            }
        }
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
            case BISQ_EASY_PRIVATE_CHAT:
                Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
                break;
            case DISCUSSION:
            case EVENTS:
            case SUPPORT:
                Navigation.navigateTo(NavigationTarget.CHAT_RULES);
                break;
        }
    }

    String getHelpButtonText() {
        switch (model.chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
            case BISQ_EASY_PRIVATE_CHAT:
                return Res.get("chat.dropdownMenu.tradeGuide");
            case DISCUSSION:
            case EVENTS:
            case SUPPORT:
            default:
                return Res.get("chat.dropdownMenu.chatRules");
        }
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
}
