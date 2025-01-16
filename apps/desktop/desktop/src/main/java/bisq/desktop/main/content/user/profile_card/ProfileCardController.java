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

package bisq.desktop.main.content.user.profile_card;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.user.profile_card.details.ProfileCardDetailsController;
import bisq.desktop.main.content.user.profile_card.messages.ProfileCardMessagesController;
import bisq.desktop.main.content.user.profile_card.offers.ProfileCardOffersController;
import bisq.desktop.main.content.user.profile_card.overview.ProfileCardOverviewController;
import bisq.desktop.main.content.user.profile_card.reputation.ProfileCardReputationController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ProfileCardController extends TabController<ProfileCardModel>
        implements InitWithDataController<ProfileCardController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final UserProfile userProfile;
        private final Optional<Runnable> ignoreUserStateHandler;

        public InitData(UserProfile userProfile) {
            this(userProfile, null);
        }

        public InitData(UserProfile userProfile, Runnable ignoreUserStateHandler) {
            this.userProfile = userProfile;
            this.ignoreUserStateHandler = Optional.ofNullable(ignoreUserStateHandler);
        }
    }

    @Getter
    private final ProfileCardView view;
    private final ReputationService reputationService;
    private final BannedUserService bannedUserService;
    private final UserProfileService userProfileService;
    protected final UserIdentityService userIdentityService;
    private final ChatService chatService;
    private final ProfileCardOverviewController profileCardOverviewController;
    private final ProfileCardDetailsController profileCardDetailsController;
    private final ProfileCardReputationController profileCardReputationController;
    private final ProfileCardOffersController profileCardOffersController;
    private final ProfileCardMessagesController profileCardMessagesController;
    private Optional<Runnable> ignoreUserStateHandler;

    public ProfileCardController(ServiceProvider serviceProvider) {
        super(new ProfileCardModel(), NavigationTarget.PROFILE_CARD);

        UserService userService = serviceProvider.getUserService();
        reputationService = userService.getReputationService();
        bannedUserService = userService.getBannedUserService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        chatService = serviceProvider.getChatService();

        profileCardOverviewController = new ProfileCardOverviewController(serviceProvider);
        profileCardDetailsController = new ProfileCardDetailsController(serviceProvider);
        profileCardReputationController = new ProfileCardReputationController(serviceProvider);
        profileCardOffersController = new ProfileCardOffersController(serviceProvider);
        profileCardMessagesController = new ProfileCardMessagesController(serviceProvider);

        view = new ProfileCardView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case PROFILE_CARD_OVERVIEW -> Optional.of(profileCardOverviewController);
            case PROFILE_CARD_DETAILS -> Optional.of(profileCardDetailsController);
            case PROFILE_CARD_OFFERS -> Optional.of(profileCardOffersController);
            case PROFILE_CARD_REPUTATION -> Optional.of(profileCardReputationController);
            case PROFILE_CARD_MESSAGES -> Optional.of(profileCardMessagesController);
            default -> Optional.empty();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        ignoreUserStateHandler = initData.ignoreUserStateHandler;
        UserProfile userProfile = initData.userProfile;
        model.setUserProfile(userProfile);

        profileCardOverviewController.setUserProfile(userProfile);
        profileCardDetailsController.setUserProfile(userProfile);
        profileCardReputationController.setUserProfile(userProfile);
        profileCardOffersController.setUserProfile(userProfile);
        profileCardMessagesController.setUserProfile(userProfile);

        model.getReputationScore().set(reputationService.getReputationScore(userProfile));

        boolean isMyProfile = userIdentityService.isUserIdentityPresent(userProfile.getId());
        model.getShouldShowUserActionsMenu().set(!isMyProfile);
        model.getOffersTabButtonText().set(Res.get("user.profileCard.tab.offers",
                profileCardOffersController.getNumberOffers()).toUpperCase());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    boolean isUserProfileBanned() {
        return bannedUserService.isUserProfileBanned(model.getUserProfile());
    }

    void onSendPrivateMessage() {
        OverlayController.hide(() -> {
            chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, model.getUserProfile())
                    .ifPresent(channel -> Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE));
        });
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
        OverlayController.hide(() ->
                Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                        new ReportToModeratorWindow.InitData(model.getUserProfile())));
    }

    void onClose() {
        OverlayController.hide();
    }
}
