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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bisq.desktop.common.view.NavigationTarget.CREATE_PROFILE_STEP1;

@Slf4j
public class UserProfileController implements Controller {
    private final UserProfileModel model;
    @Getter
    private final UserProfileView view;
    private final UserIdentityService userIdentityService;
    private Pin selectedUserProfilePin;

    public UserProfileController(DefaultApplicationService applicationService) {
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);

        model = new UserProfileModel();
        view = new UserProfileView(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserProfile(),
                chatUserIdentity -> model.getUserProfileDisplayPane().set(
                        new UserProfileDisplay(userIdentityService, chatUserIdentity).getRoot())
        );
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }

    public void onAddNewChatUser() {
        Navigation.navigateTo(CREATE_PROFILE_STEP1);
    }
}
