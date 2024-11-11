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

package bisq.desktop.main.content.user.user_card;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.user.user_card.details.UserCardDetailsController;
import bisq.desktop.main.content.user.user_card.overview.UserCardOverviewController;
import bisq.desktop.overlay.OverlayController;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class UserCardController extends TabController<UserCardModel>
        implements InitWithDataController<UserCardController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final UserProfile userProfile;
        private final Optional<ChatChannel<? extends ChatMessage>> selectedChannel;
        private final Optional<Consumer<UserProfile>> sendPrivateMessageHandler;
        private final Optional<Runnable> ignoreUserStateHandler, closeHandler;

        public InitData(UserProfile userProfile,
                        @Nullable ChatChannel<? extends ChatMessage> selectedChannel,
                        Consumer<UserProfile> sendPrivateMessageHandler,
                        Runnable ignoreUserStateHandler,
                        Runnable closeHandler) {
            this.userProfile = userProfile;
            this.selectedChannel = Optional.ofNullable(selectedChannel);
            this.sendPrivateMessageHandler = Optional.ofNullable(sendPrivateMessageHandler);
            this.ignoreUserStateHandler = Optional.ofNullable(ignoreUserStateHandler);
            this.closeHandler = Optional.ofNullable(closeHandler);
        }

        public InitData(UserProfile userProfile) {
            this(userProfile, null, null, null, null);
        }
    }

    @Getter
    private final UserCardView view;
    private final ReputationService reputationService;
    private final BannedUserService bannedUserService;
    private final UserProfileService userProfileService;
    private final UserCardOverviewController userCardOverviewController;
    private final UserCardDetailsController userCardDetailsController;
    private Optional<ChatChannel<? extends ChatMessage>> selectedChannel;
    private Optional<Consumer<UserProfile>> sendPrivateMessageHandler;
    private Optional<Runnable> ignoreUserStateHandler, closeHandler;
    private Subscription userProfilePin;

    public UserCardController(ServiceProvider serviceProvider) {
        super(new UserCardModel(), NavigationTarget.USER_CARD);

        reputationService = serviceProvider.getUserService().getReputationService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        userCardOverviewController = new UserCardOverviewController(serviceProvider);
        userCardDetailsController = new UserCardDetailsController(serviceProvider);
        view = new UserCardView(model, this);
    }

    @Override
    public void onActivate() {
        userProfilePin = EasyBind.subscribe(model.getUserProfile(), userProfile -> {
            model.getReputationScore().set(reputationService.getReputationScore(userProfile));
            model.getShouldShowReportButton().set(selectedChannel.isPresent());
            userCardDetailsController.updateUserProfileData(userProfile);
            userCardOverviewController.updateUserProfileData(userProfile);
        });
    }

    @Override
    public void onDeactivate() {
        userProfilePin.unsubscribe();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case USER_CARD_OVERVIEW -> Optional.of(userCardOverviewController);
            case USER_CARD_DETAILS -> Optional.of(userCardDetailsController);
//            case USER_DETAILS_OFFERS -> Optional.of();
//            case USER_DETAILS_REPUTATION -> Optional.of();
            default -> Optional.empty();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        selectedChannel = initData.selectedChannel;
        sendPrivateMessageHandler = initData.sendPrivateMessageHandler;
        ignoreUserStateHandler = initData.ignoreUserStateHandler;
        closeHandler = initData.closeHandler;
        model.getUserProfile().set(initData.userProfile);
    }

    boolean isUserProfileBanned() {
        return bannedUserService.isUserProfileBanned(model.getUserProfile().get());
    }

    void onSendPrivateMessage() {
        sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.getUserProfile().get()));
    }

    void onToggleIgnoreUser() {
        model.getIgnoreUserSelected().set(!model.getIgnoreUserSelected().get());
        if (model.getIgnoreUserSelected().get()) {
            userProfileService.ignoreUserProfile(model.getUserProfile().get());
        } else {
            userProfileService.undoIgnoreUserProfile(model.getUserProfile().get());
        }
        ignoreUserStateHandler.ifPresent(Runnable::run);
    }

    void onReportUser() {
        if (selectedChannel.isPresent()) {
            ChatChannelDomain chatChannelDomain = selectedChannel.get().getChatChannelDomain();
            OverlayController.hide(() -> {
                Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                        new ReportToModeratorWindow.InitData(model.getUserProfile().get(), chatChannelDomain));
                closeHandler.ifPresent(Runnable::run);
            });
        }
    }

    void onClose() {
        OverlayController.hide(() -> closeHandler.ifPresent(Runnable::run));
    }
}
