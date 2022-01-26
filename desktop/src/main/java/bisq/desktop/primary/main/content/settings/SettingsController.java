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
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.networkinfo.NetworkInfoController;
import bisq.desktop.primary.main.content.settings.networkinfo.about.AboutController;
import bisq.desktop.primary.main.content.settings.networkinfo.preferences.PreferencesController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SettingsController extends TabController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SettingsModel model;
    @Getter
    private final SettingsView view;

    public SettingsController(DefaultApplicationService applicationService) {
        super(NavigationTarget.SETTINGS);

        this.applicationService = applicationService;
        model = new SettingsModel(applicationService);
        view = new SettingsView(model, this);
    }

    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
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
