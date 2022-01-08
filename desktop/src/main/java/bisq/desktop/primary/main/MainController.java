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

package bisq.desktop.primary.main;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.nav.NavigationController;
import bisq.desktop.primary.main.top.TopPanelController;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;

public class MainController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final MainModel model = new MainModel();
    private final ContentController contentController;
    private final NavigationController navigationController;
    private final TopPanelController topPanelController;
    @Getter
    private final MainView view;

    public MainController(DefaultServiceProvider serviceProvider, OverlayController overlayController) {
        this.serviceProvider = serviceProvider;

        contentController = new ContentController(serviceProvider, overlayController);
        navigationController = new NavigationController(serviceProvider, contentController, overlayController);
        topPanelController = new TopPanelController();

        view = new MainView(model,
                this,
                contentController.getView(),
                navigationController.getView(),
                topPanelController.getView());
    }

    public void onViewAttached() {
        navigationController.navigateTo(NavigationTarget.TRADE_INTENT);
    }

    public void onViewDetached() {
    }

    public void onDomainInitialized() {

    }
}
