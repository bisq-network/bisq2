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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileController implements Controller {

    private final UserProfileSelection userProfileSelection;
    private final CreateUserProfile createUserProfile;
    @Getter
    private final UserProfileModel model;
    @Getter
    private final UserProfileView view;
    private final UserProfileDisplay userProfileDisplay;

    public UserProfileController(DefaultApplicationService applicationService) {
        userProfileSelection = new UserProfileSelection(applicationService.getUserProfileService());
        userProfileDisplay = new UserProfileDisplay(applicationService.getUserProfileService());
        createUserProfile = new CreateUserProfile(applicationService.getUserProfileService(), applicationService.getKeyPairService());
        model = new UserProfileModel(applicationService);
        view = new UserProfileView(model, this, userProfileSelection.getView(), userProfileDisplay.getView(), createUserProfile.getView());
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    public void onViewDetached() {
    }
}
