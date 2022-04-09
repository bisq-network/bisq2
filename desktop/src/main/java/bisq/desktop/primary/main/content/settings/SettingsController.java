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

package bisq.desktop.primary.main.content.settings;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.networkInfo.NetworkInfoController;
import bisq.desktop.primary.main.content.settings.about.AboutController;
import bisq.desktop.primary.main.content.settings.preferences.PreferencesController;
import bisq.desktop.primary.main.content.settings.userProfile.UserProfileController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SettingsController extends TabController<SettingsModel> {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SettingsView view;

    public SettingsController(DefaultApplicationService applicationService) {
        super(new SettingsModel(applicationService), NavigationTarget.SETTINGS);

        this.applicationService = applicationService;
        view = new SettingsView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case USER_PROFILE -> {
                return Optional.of(new UserProfileController(applicationService));
            }
            case PREFERENCES -> {
                return Optional.of(new PreferencesController(applicationService));
            }
            case NETWORK_INFO -> {
                return Optional.of(new NetworkInfoController(applicationService));
            }
            case ABOUT -> {
                return Optional.of(new AboutController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
