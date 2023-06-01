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

package bisq.desktop.primary.main.content.user.user_profile.create.step1;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.user.user_profile.create.step2.GenerateNewProfileStep2Controller;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileController;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileModel;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileView;
import javafx.application.Platform;

public class GenerateNewProfileStep1Controller extends GenerateProfileController {
    private final DefaultApplicationService applicationService;

    public GenerateNewProfileStep1Controller(DefaultApplicationService applicationService) {
        super(applicationService);
        this.applicationService = applicationService;
    }

    @Override
    protected GenerateProfileView getGenerateProfileView() {
        return new GenerateNewProfileStep1View((GenerateNewProfileStep1Model) model, this);
    }

    @Override
    protected GenerateProfileModel getGenerateProfileModel() {
        return new GenerateNewProfileStep1Model();
    }

    @Override
    protected void onCreateUserProfile() {
        GenerateNewProfileStep2Controller.InitData initData = new GenerateNewProfileStep2Controller.InitData(
                model.getKeyPairAndId(),
                model.getPooledIdentity(),
                model.getProofOfWork().orElseThrow(),
                model.getNickName().get(),
                model.getNym().get());
        Navigation.navigateTo(NavigationTarget.CREATE_PROFILE_STEP2, initData);
    }

    void onCancel() {
        OverlayController.hide();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }
}