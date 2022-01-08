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

import bisq.desktop.StageType;
import bisq.desktop.NavigationTarget;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.overlay.OverlayController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NavigationTargetController implements Controller {
    protected final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();
    protected final ContentController contentController;
    protected final OverlayController overlayController;

    public NavigationTargetController(ContentController contentController, OverlayController overlayController) {
        this.contentController = contentController;
        this.overlayController = overlayController;
    }

    public void navigateTo(NavigationTarget navigationTarget) {
        NavigationTarget localTarget = resolveLocalTarget(navigationTarget);
        Controller controller = getOrCreateController(localTarget, navigationTarget);
        if (localTarget.getSink() == StageType.OVERLAY) {
            overlayController.show(controller);
        } else {
            contentController.navigateTo(navigationTarget, controller);
        }
    }

    protected Controller getOrCreateController(NavigationTarget localTarget, NavigationTarget navigationTarget) {
        if (controllerCache.containsKey(localTarget)) {
            return controllerCache.get(localTarget);
        } else {
            Controller controller = getController(localTarget, navigationTarget);
            controllerCache.put(localTarget, controller);
            return controller;
        }
    }

    protected abstract Controller getController(NavigationTarget localTarget, NavigationTarget navigationTarget);

    protected abstract NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget);

    protected NavigationTarget resolveAsRootHost(NavigationTarget navigationTarget) {
        List<NavigationTarget> path = navigationTarget.getPath();
        return path.isEmpty() ? navigationTarget : path.get(0);
    }

    protected NavigationTarget resolveAsLevel1Host(NavigationTarget navigationTarget) {
        List<NavigationTarget> path = navigationTarget.getPath();
        return path.size() == 1 ? navigationTarget : path.get(1);
    }

    protected NavigationTarget resolveAsLevel2Host(NavigationTarget navigationTarget) {
        List<NavigationTarget> path = navigationTarget.getPath();
        return path.size() == 2 ? navigationTarget : path.get(2);
    }
}
