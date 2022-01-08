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

import bisq.desktop.NavigationTarget;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.overlay.OverlayController;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class TabController<M extends TabModel> extends NavigationTargetController {
    private final NavigationTarget navigationTarget;

    public TabController(ContentController contentController,
                         OverlayController overlayController,
                         NavigationTarget navigationTarget) {
        super(contentController, overlayController);

        this.navigationTarget = navigationTarget;
    }

    protected abstract M getModel();

    @Override
    public void onViewAttached() {
        List<NavigationTarget> path = navigationTarget.getPath();
        NavigationTarget child = path.size() > 1 ? path.get(1) :
                path.size() > 0 ? navigationTarget :
                        getModel().getDefaultNavigationTarget();
        navigateTo(child);
    }

    @Override
    public void navigateTo(NavigationTarget navigationTarget) {
        NavigationTarget localTarget = resolveLocalTarget(navigationTarget);
        Controller controller = getOrCreateController(localTarget, navigationTarget);
        getModel().select(localTarget, controller.getView());
    }

    public void onTabSelected(NavigationTarget navigationTarget) {
        navigateTo(navigationTarget);
    }
}
