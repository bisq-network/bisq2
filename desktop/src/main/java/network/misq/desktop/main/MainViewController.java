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
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.main.content.settings.SettingsController;
import network.misq.desktop.main.left.NavigationViewController;
import network.misq.desktop.main.top.TopPanelController;
import network.misq.desktop.overlay.OverlayController;

public class MainViewController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final MainViewModel model = new MainViewModel();
    private final ContentViewController contentViewController;
    private final NavigationViewController navigationViewController;
    private final TopPanelController topPanelController;
    @Getter
    private final MainView view;

    public MainViewController(DefaultServiceProvider serviceProvider, OverlayController overlayController) {
         this.serviceProvider = serviceProvider;

        contentViewController = new ContentViewController(serviceProvider, overlayController);
        navigationViewController = new NavigationViewController(serviceProvider, contentViewController, overlayController);
        topPanelController = new TopPanelController();

        view = new MainView(model,
                this,
                contentViewController.getView(),
                navigationViewController.getView(),
                topPanelController.getView());
    }

    public void initialize() {
        try {
            contentViewController.initialize();
            navigationViewController.initialize();
            topPanelController.initialize();

            contentViewController.onNavigationRequest(SettingsController.class);
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
