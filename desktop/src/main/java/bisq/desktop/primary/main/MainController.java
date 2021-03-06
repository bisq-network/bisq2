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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.left.LeftNavController;
import bisq.desktop.primary.main.top.TopPanelController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MainController extends NavigationController {
    @Getter
    private final MainModel model = new MainModel();
    @Getter
    private final MainView view;
    private final DefaultApplicationService applicationService;
    private final LeftNavController leftNavController;

    public MainController(DefaultApplicationService applicationService) {
        super(NavigationTarget.MAIN);

        this.applicationService = applicationService;

        leftNavController = new LeftNavController(applicationService);
        TopPanelController topPanelController = new TopPanelController(applicationService);

        view = new MainView(model,
                this,
                leftNavController.getView().getRoot(),
                topPanelController.getView().getRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CONTENT: {
                return Optional.of(new ContentController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        leftNavController.setNavigationTarget(navigationTarget);
    }
}
