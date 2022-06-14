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

package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import javafx.beans.property.ReadOnlyStringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CreateProfileController implements Controller {
    private final CreateProfileModel model;
    @Getter
    private final CreateProfileView view;
    private Subscription nickNameSubscription;


    public CreateProfileController(DefaultApplicationService applicationService) {
        model = new CreateProfileModel();
        view = new CreateProfileView(model, this);
    }

    public ReadOnlyStringProperty getNickName() {
        return model.getNickName();
    }

    public ReadOnlyStringProperty getBio() {
        return model.getBio();
    }

    public ReadOnlyStringProperty getTerms() {
        return model.getTerms();
    }

    @Override
    public void onActivate() {
        nickNameSubscription = EasyBind.subscribe(model.nickName,
                nickName -> model.createProfileButtonDisabled.set(nickName == null || nickName.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        nickNameSubscription.unsubscribe();
    }

    void onCreateUserProfile() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
    }
}
