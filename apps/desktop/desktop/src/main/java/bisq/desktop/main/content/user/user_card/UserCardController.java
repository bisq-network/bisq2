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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.user.user_card.details.UserCardDetailsController;
import bisq.desktop.overlay.OverlayController;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
        private final Optional<Runnable> ignoreUserStateHandler;

        public InitData(UserProfile userProfile,
                        @Nullable ChatChannel<? extends ChatMessage> selectedChannel,
                        Consumer<UserProfile> sendPrivateMessageHandler,
                        Runnable ignoreUserStateHandler) {
            this.userProfile = userProfile;
            this.selectedChannel = Optional.ofNullable(selectedChannel);
            this.sendPrivateMessageHandler = Optional.ofNullable(sendPrivateMessageHandler);
            this.ignoreUserStateHandler = Optional.ofNullable(ignoreUserStateHandler);
        }

        public InitData(UserProfile userProfile) {
            this(userProfile, null, null, null);
        }
    }

    @Getter
    private final UserCardView view;
    private final ServiceProvider serviceProvider;
    private final ReputationService reputationService;
    private final BannedUserService bannedUserService;
    private final UserProfileService userProfileService;
    private UserProfile userProfile;
    private Optional<ChatChannel<? extends ChatMessage>> selectedChannel;
    private Optional<Consumer<UserProfile>> sendPrivateMessageHandler;
    private Optional<Runnable> ignoreUserStateHandler;

    public UserCardController(ServiceProvider serviceProvider) {
        super(new UserCardModel(), NavigationTarget.USER_CARD);

        this.serviceProvider = serviceProvider;
        reputationService = serviceProvider.getUserService().getReputationService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        view = new UserCardView(model, this);
    }

    @Override
    public void onActivate() {
        model.setUserProfile(userProfile);
        model.getReputationScore().set(reputationService.getReputationScore(userProfile));
        model.getShouldShowReportButton().set(selectedChannel.isPresent());
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case USER_CARD_OVERVIEW -> Optional.of(new UserCardDetailsController(serviceProvider, userProfile));
//            case USER_DETAILS_OFFERS -> Optional.of(new (serviceProvider));
//            case USER_DETAILS_REPUTATION -> Optional.of(new (serviceProvider));
            default -> Optional.empty();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        userProfile = initData.userProfile;
        selectedChannel = initData.selectedChannel;
        sendPrivateMessageHandler = initData.sendPrivateMessageHandler;
        ignoreUserStateHandler = initData.ignoreUserStateHandler;
    }

    void onSendPrivateMessage() {
        sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.getUserProfile()));
    }

    void onToggleIgnoreUser() {
        model.getIgnoreUserSelected().set(!model.getIgnoreUserSelected().get());
        if (model.getIgnoreUserSelected().get()) {
            userProfileService.ignoreUserProfile(model.getUserProfile());
        } else {
            userProfileService.undoIgnoreUserProfile(model.getUserProfile());
        }
        ignoreUserStateHandler.ifPresent(Runnable::run);
    }

    void onReportUser() {
        if (selectedChannel.isPresent()) {
            ChatChannelDomain chatChannelDomain = selectedChannel.get().getChatChannelDomain();
            // FIXME
//            Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
//                        new ReportToModeratorWindow.InitData(model.getUserProfile(), chatChannelDomain));
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    public boolean isUserProfileBanned() {
        return bannedUserService.isUserProfileBanned(model.getUserProfile());
    }
}
