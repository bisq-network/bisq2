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
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.content.ContentController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TabController<M extends TabModel> extends NavigationController {

    public TabController(ContentController contentController,
                         OverlayController overlayController) {
        super(contentController, overlayController);
    }

    protected abstract M getModel();

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
