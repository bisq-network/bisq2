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

package network.misq.desktop.main.left;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.NavigationSink;
import network.misq.desktop.NavigationTarget;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentController;
import network.misq.desktop.main.content.createoffer.CreateOfferController;
import network.misq.desktop.main.content.markets.MarketsController;
import network.misq.desktop.main.content.networkinfo.NetworkInfoController;
import network.misq.desktop.main.content.offerbook.OfferbookController;
import network.misq.desktop.main.content.settings.SettingsController;
import network.misq.desktop.overlay.OverlayController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NavigationController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final ContentController contentController;
    private final OverlayController overlayController;
    private final NavigationModel model;
    @Getter
    private final NavigationView view;
    private final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();

    public NavigationController(DefaultServiceProvider serviceProvider,
                                ContentController contentController,
                                OverlayController overlayController) {
        this.serviceProvider = serviceProvider;
        this.contentController = contentController;
        this.overlayController = overlayController;

        model = new NavigationModel(serviceProvider);
        view = new NavigationView(model, this);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }


    public void navigateTo(NavigationTarget navigationTarget) {
        Controller controller = getOrCreate(navigationTarget);
        if (navigationTarget.getSink() == NavigationSink.OVERLAY) {
            overlayController.show(controller);
        } else {
            contentController.navigateTo(controller);
        }
    }

    private Controller getOrCreate(NavigationTarget navigationTarget) {
        if (controllerCache.containsKey(navigationTarget)) {
            Controller controller = controllerCache.get(navigationTarget);
            controller.activate();
            return controller;
        } else {
            Controller controller = getController(navigationTarget);
            controller.initialize();
            controllerCache.put(navigationTarget, controller);
            return controller;
        }
    }

    private Controller getController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MARKETS -> new MarketsController(serviceProvider);
            case CREATE_OFFER -> new CreateOfferController(serviceProvider);
            case OFFERBOOK -> new OfferbookController(serviceProvider, this, overlayController);
            case SETTINGS -> new SettingsController(serviceProvider, contentController, overlayController);
            case NETWORK_INFO -> new NetworkInfoController(serviceProvider);
        };
    }
}
