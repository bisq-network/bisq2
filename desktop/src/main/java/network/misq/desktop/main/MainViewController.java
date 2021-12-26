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

package network.misq.desktop.main;

import lombok.Getter;
import network.misq.api.DefaultApi;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.main.content.offerbook.OfferbookController;
import network.misq.desktop.main.left.NavigationViewController;
import network.misq.desktop.main.top.TopPanelController;
import network.misq.desktop.overlay.OverlayController;

public class MainViewController implements Controller {
    private final DefaultApi api;
    private final OverlayController overlayController;
    private MainViewModel model;
    @Getter
    private MainView view;

    public MainViewController(DefaultApi api, OverlayController overlayController) {
        this.api = api;
        this.overlayController = overlayController;
    }

    public void initialize() {
        try {
            this.model = new MainViewModel();

            ContentViewController contentViewController = new ContentViewController(api, overlayController);
            contentViewController.initialize();
            NavigationViewController navigationViewController = new NavigationViewController(api,
                    contentViewController,
                    overlayController);
            navigationViewController.initialize();
            TopPanelController topPanelController = new TopPanelController();
            topPanelController.initialize();

            this.view = new MainView(model,
                    this,
                    contentViewController.getView(),
                    navigationViewController.getView(),
                    topPanelController.getView());

            contentViewController.onNavigationRequest(OfferbookController.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewAdded() {

    }

    @Override
    public void onViewRemoved() {

    }
}
