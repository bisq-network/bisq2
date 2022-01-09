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

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.content.settings.networkinfo.NetworkInfoController;
import bisq.desktop.primary.main.content.settings.networkinfo.about.AboutController;
import bisq.desktop.primary.main.content.settings.networkinfo.preferences.PreferencesController;
import lombok.Getter;

public class SettingsController extends TabController<SettingsModel> implements Controller {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final SettingsModel model;
    @Getter
    private final SettingsView view;

    public SettingsController(DefaultServiceProvider serviceProvider,
                              ContentController contentController,
                              OverlayController overlayController) {
        super(contentController, overlayController);

        this.serviceProvider = serviceProvider;
        model = new SettingsModel(serviceProvider);
        view = new SettingsView(model, this);
    }

    @Override
    protected NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget) {
        return resolveAsLevel1Host(navigationTarget);
    }

    @Override
    protected Controller getController(NavigationTarget localTarget, NavigationTarget navigationTarget) {
        switch (localTarget) {
            case PREFERENCES -> {
                return new PreferencesController(serviceProvider);
            }
            case NETWORK_INFO -> {
                return new NetworkInfoController(serviceProvider, contentController, overlayController);
            }
            case ABOUT -> {
                return new AboutController(serviceProvider);
            }
            default -> throw new IllegalArgumentException("Invalid navigationTarget for this host. localTarget=" + localTarget);
        }
    }
}
