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
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.user.profile_card.details.ProfileCardDetailsController;
import bisq.desktop.main.content.user.profile_card.offers.ProfileCardOffersController;
import bisq.desktop.main.content.user.profile_card.overview.ProfileCardOverviewController;
import bisq.desktop.main.content.user.profile_card.reputation.ProfileCardReputationController;
import bisq.desktop.overlay.OverlayController;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
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

@Slf4j
public class ProfileCardController extends TabController<ProfileCardModel>
        implements InitWithDataController<ProfileCardController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final UserProfile userProfile;
        private final Optional<ChatChannel<? extends ChatMessage>> selectedChannel;
        private final Optional<Runnable> ignoreUserStateHandler;

        public InitData(UserProfile userProfile,
                        @Nullable ChatChannel<? extends ChatMessage> selectedChannel,
                        Runnable ignoreUserStateHandler) {
            this.userProfile = userProfile;
            this.selectedChannel = Optional.ofNullable(selectedChannel);
            this.ignoreUserStateHandler = Optional.ofNullable(ignoreUserStateHandler);
        }

        public InitData(UserProfile userProfile) {
            this(userProfile, null, null);
        }

        public InitData(UserProfile userProfile, @Nullable ChatChannel<? extends ChatMessage> selectedChannel) {
            this(userProfile, selectedChannel, null);
        }
    }

    @Getter
    private final ProfileCardView view;
    private final ReputationService reputationService;
    private final BannedUserService bannedUserService;
    private final UserProfileService userProfileService;
    private final ChatService chatService;
    protected final UserIdentityService userIdentityService;
    private final ProfileCardOverviewController profileCardOverviewController;
    private final ProfileCardDetailsController profileCardDetailsController;
    private final ProfileCardReputationController profileCardReputationController;
    private final ProfileCardOffersController profileCardOffersController;
    private Optional<ChatChannel<? extends ChatMessage>> selectedChannel;
    private Optional<Runnable> ignoreUserStateHandler;
    private Subscription userProfilePin;

    public ProfileCardController(ServiceProvider serviceProvider) {
        super(new ProfileCardModel(), NavigationTarget.PROFILE_CARD);

        reputationService = serviceProvider.getUserService().getReputationService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        profileCardOverviewController = new ProfileCardOverviewController(serviceProvider);
        profileCardDetailsController = new ProfileCardDetailsController(serviceProvider);
        profileCardReputationController = new ProfileCardReputationController(serviceProvider);
        profileCardOffersController = new ProfileCardOffersController(serviceProvider);
        view = new ProfileCardView(model, this);
    }

    @Override
    public void onActivate() {
        userProfilePin = EasyBind.subscribe(model.getUserProfile(), userProfile -> {
            model.getReputationScore().set(reputationService.getReputationScore(userProfile));
            profileCardOverviewController.updateUserProfileData(userProfile);
            profileCardDetailsController.updateUserProfileData(userProfile);
            profileCardReputationController.updateUserProfileData(userProfile);
            profileCardOffersController.updateUserProfileData(userProfile);
            boolean isMyProfile = userIdentityService.isUserIdentityPresent(userProfile.getId());
            model.getShouldShowReportButton().set(!isMyProfile && selectedChannel.isPresent());
            model.getShouldShowUserActionsMenu().set(!isMyProfile);
        });
    }

    @Override
    public void onDeactivate() {
        userProfilePin.unsubscribe();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case PROFILE_CARD_OVERVIEW -> Optional.of(profileCardOverviewController);
            case PROFILE_CARD_DETAILS -> Optional.of(profileCardDetailsController);
            case PROFILE_CARD_OFFERS -> Optional.of(profileCardOffersController);
            case PROFILE_CARD_REPUTATION -> Optional.of(profileCardReputationController);
            default -> Optional.empty();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        selectedChannel = initData.selectedChannel;
        ignoreUserStateHandler = initData.ignoreUserStateHandler;
        model.getUserProfile().set(initData.userProfile);
    }

    boolean isUserProfileBanned() {
        return bannedUserService.isUserProfileBanned(model.getUserProfile().get());
    }

    void onSendPrivateMessage() {
        OverlayController.hide(() -> {
            chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, model.getUserProfile().get())
                    .ifPresent(channel -> Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE));
        });
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
            });
        }
    }

    void onClose() {
        OverlayController.hide();
    }
}
