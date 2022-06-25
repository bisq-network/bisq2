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

package bisq.desktop.primary.main.content.settings.userProfile.create;

import bisq.application.DefaultApplicationService;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileController;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileModel;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileView;

public class GenerateNewProfileController extends GenerateProfileController {
    public GenerateNewProfileController(DefaultApplicationService applicationService) {
        super(applicationService);
    }

    @Override
    protected GenerateProfileView getGenerateProfileView() {
        return new GenerateNewProfileView((GenerateNewProfileModel) model, this);
    }

    @Override
    protected GenerateProfileModel getGenerateProfileModel() {
        return new GenerateNewProfileModel();
    }

    @Override
    protected void navigateNext() {
        //todo
        OverlayController.hide();
    }
}