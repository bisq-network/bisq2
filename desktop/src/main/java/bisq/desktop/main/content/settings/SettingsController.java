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

package bisq.desktop.main.content.settings;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTargetController;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.content.settings.networkinfo.NetworkInfoController;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsController extends NavigationTargetController implements Controller {
    private final SettingsModel model;
    private final NavigationTarget childNavigationTarget;
    @Getter
    private final SettingsView view;
    private final DefaultServiceProvider serviceProvider;

    private Controller selectedController;
    private final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();

    public SettingsController(DefaultServiceProvider serviceProvider,
                              ContentController contentController,
                              OverlayController overlayController,
                              NavigationTarget navigationTarget) {
        super(contentController, overlayController);
        this.serviceProvider = serviceProvider;
        model = new SettingsModel(serviceProvider);
        this.childNavigationTarget = navigationTarget;
        view = new SettingsView(model, this);

        List<NavigationTarget> path = navigationTarget.getPath();
        NavigationTarget child = path.size() > 1 ? path.get(1) :
                path.size() > 0 ? navigationTarget :
                        model.getSelectedNavigationTarget();
        switch (child) {
            case PREFERENCES -> {
                //todo
            }
            case ABOUT -> {
                //todo
            }
            case NETWORK_INFO -> {
                selectedController = getNetworkInfoController(navigationTarget);
                model.selectView(navigationTarget, selectedController.getView());
            }
        }
    }

    protected Controller getNetworkInfoController(NavigationTarget navigationTarget) {
        return new NetworkInfoController(serviceProvider);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onTabSelected(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case PREFERENCES -> {
            }
            case NETWORK_INFO -> {
                selectedController = new NetworkInfoController(serviceProvider);
                model.selectView(navigationTarget, selectedController.getView());
            }
            case ABOUT -> {
            }
        }
    }
}
