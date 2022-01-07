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

package bisq.desktop.common.view;

import bisq.desktop.NavigationSink;
import bisq.desktop.NavigationTarget;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.overlay.OverlayController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NavigationTargetController {
    protected final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();
    protected final ContentController contentController;
    protected final OverlayController overlayController;

    public <contentController> NavigationTargetController(ContentController contentController, OverlayController overlayController) {
        this.contentController = contentController;
        this.overlayController = overlayController;
    }

    public void navigateTo(NavigationTarget navigationTarget) {
        Controller controller = getOrCreateController(navigationTarget);
        if (navigationTarget.getSink() == NavigationSink.OVERLAY) {
            overlayController.show(controller);
        } else {
            contentController.navigateTo(controller);
        }
    }

    protected Controller getOrCreateController(NavigationTarget navigationTarget) {
        if (controllerCache.containsKey(navigationTarget)) {
            return controllerCache.get(navigationTarget);
        } else {
            Controller controller = getController(navigationTarget);
            controllerCache.put(navigationTarget, controller);
            return controller;
        }
    }


    protected Controller getController(NavigationTarget navigationTarget) {
        //  TRANSPORT_TYPE(SETTINGS, NETWORK_INFO);
        List<NavigationTarget> path = navigationTarget.getPath();
        NavigationTarget root = path.isEmpty() ? navigationTarget : path.get(0);
        return switch (root) {
            // Root NavigationTargets
            case MARKETS -> getMarketsController(navigationTarget);
            case CREATE_OFFER -> getCreateOfferController(navigationTarget);
            case OFFERBOOK -> getOfferbookController(navigationTarget);
            case SETTINGS -> getSettingsController(navigationTarget);
            // Children not handled here but on defined root inside the path 
            case PREFERENCES -> null; //todo
            case ABOUT -> null; //todo
            case NETWORK_INFO -> getNetworkInfoController(navigationTarget);
            case TRANSPORT_TYPE -> null; //todo
        };
    }

    protected Controller getMarketsController(NavigationTarget navigationTarget) {
        throw new RuntimeException("Need to be implemented by concrete controller");
    }

    protected Controller getCreateOfferController(NavigationTarget navigationTarget) {
        throw new RuntimeException("Need to be implemented by concrete controller");
    }

    protected Controller getOfferbookController(NavigationTarget navigationTarget) {
        throw new RuntimeException("Need to be implemented by concrete controller");
    }

    protected Controller getSettingsController(NavigationTarget navigationTarget) {
        throw new RuntimeException("Need to be implemented by concrete controller");
    }

    protected Controller getNetworkInfoController(NavigationTarget navigationTarget) {
        throw new RuntimeException("Need to be implemented by concrete controller");
    }
}
