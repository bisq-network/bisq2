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

package bisq.desktop.primary.main.content.social.profile;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.components.UserProfileDisplay;
import bisq.desktop.primary.main.content.social.profile.components.ChannelAdmin;
import bisq.desktop.primary.main.content.social.profile.components.CreateUserProfile;
import bisq.desktop.primary.main.content.social.profile.components.UserProfileSelection;
import bisq.social.userprofile.Entitlement;
import bisq.social.userprofile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileController implements Controller {

    private final UserProfileSelection userProfileSelection;
    private final CreateUserProfile createUserProfile;
    @Getter
    private final UserProfileModel model;
    @Getter
    private final UserProfileView view;
    private final UserProfileDisplay userProfileDisplay;
    private final ChannelAdmin channelAdmin;
    private final UserProfileService userProfileService;
    private Subscription selectedUserProfileSubscription;

    public UserProfileController(DefaultApplicationService applicationService) {
        userProfileService = applicationService.getUserProfileService();
        userProfileSelection = new UserProfileSelection(userProfileService);
        userProfileDisplay = new UserProfileDisplay(userProfileService);
        createUserProfile = new CreateUserProfile(userProfileService, applicationService.getKeyPairService());
        channelAdmin = new ChannelAdmin(userProfileService, applicationService.getChatService());
        model = new UserProfileModel(applicationService);
        view = new UserProfileView(model,
                this,
                userProfileSelection.getRoot(),
                userProfileDisplay.getRoot(),
                channelAdmin.getRoot(),
                createUserProfile.getRoot());
    }

    @Override
    public void onViewAttached() {
        model.createUserProfileVisible.set(false);
        model.channelAdminVisible.set(true);
        selectedUserProfileSubscription = EasyBind.subscribe(userProfileSelection.getSelectedUserProfile(),
                userProfile -> {
                    model.channelAdminVisible.set(userProfile.hasEntitlementType(Entitlement.Type.CHANNEL_ADMIN));
                });
    }

    @Override
    public void onViewDetached() {
        selectedUserProfileSubscription.unsubscribe();
    }

    public void showCreateUserProfile() {
        model.createUserProfileVisible.set(true);
    }
}
