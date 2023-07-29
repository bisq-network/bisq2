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

package bisq.desktop.main.content.user.user_profile.create.step1;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.user.user_profile.create.step2.CreateNewProfileStep2Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.onboarding.create_profile.CreateProfileController;
import bisq.desktop.overlay.onboarding.create_profile.CreateProfileModel;
import bisq.desktop.overlay.onboarding.create_profile.CreateProfileView;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateNewProfileStep1Controller extends CreateProfileController {
    private final ServiceProvider serviceProvider;

    public CreateNewProfileStep1Controller(ServiceProvider serviceProvider) {
        super(serviceProvider);
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected CreateProfileView getGenerateProfileView() {
        return new CreateNewProfileStep1View((CreateNewProfileStep1Model) model, this);
    }

    @Override
    protected CreateProfileModel getGenerateProfileModel() {
        return new CreateNewProfileStep1Model();
    }

    @Override
    protected void onCreateUserProfile() {
        if (model.getProofOfWork().isEmpty()) {
            log.error("proofOfWork is not present");
            return;
        }
        CreateNewProfileStep2Controller.InitData initData = new CreateNewProfileStep2Controller.InitData(
                model.getKeyPairAndId(),
                model.getPooledIdentity(),
                model.getProofOfWork().get(),
                model.getNickName().get(),
                model.getNym().get());
        Navigation.navigateTo(NavigationTarget.CREATE_PROFILE_STEP2, initData);
    }

    void onCancel() {
        OverlayController.hide();
    }

    void onQuit() {
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
    }
}