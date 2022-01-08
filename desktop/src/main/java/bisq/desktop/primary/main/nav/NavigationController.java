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

package bisq.desktop.primary.main.nav;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.StageType;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTargetController;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.content.markets.MarketsController;
import bisq.desktop.primary.main.content.offerbook.OfferbookController;
import bisq.desktop.primary.main.content.portfolio.PortfolioController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.social.SocialController;
import lombok.Getter;

public class NavigationController extends NavigationTargetController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final NavigationModel model;
    @Getter
    private final NavigationView view;

    public NavigationController(DefaultServiceProvider serviceProvider,
                                ContentController contentController,
                                OverlayController overlayController) {
        super(contentController, overlayController);

        this.serviceProvider = serviceProvider;
        model = new NavigationModel(serviceProvider);
        view = new NavigationView(model, this);
    }

    @Override
    protected NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget) {
        return resolveAsRootHost(navigationTarget);
    }

    @Override
    public void navigateTo(NavigationTarget navigationTarget) {
        NavigationTarget localTarget = resolveLocalTarget(navigationTarget);
        Controller controller = getOrCreateController(localTarget, navigationTarget);
        if (localTarget.getSink() == StageType.OVERLAY) {
            overlayController.show(controller);
        } else {
            contentController.navigateTo(navigationTarget, controller);
        }
        model.select(localTarget, controller.getView());
    }

    @Override
    protected Controller getController(NavigationTarget localTarget,NavigationTarget navigationTarget) {
        switch (localTarget) {
            case MARKETS -> {
                return new MarketsController(serviceProvider);
            }
            case SOCIAL -> {
                return new SocialController(serviceProvider, contentController, overlayController, navigationTarget);
            }
            case OFFERBOOK -> {
                return new OfferbookController(serviceProvider, this, overlayController);
            }
            case PORTFOLIO -> {
                return new PortfolioController(serviceProvider);
            }
            case SETTINGS -> {
                return new SettingsController(serviceProvider, contentController, overlayController, navigationTarget);
            }
            default -> throw new IllegalArgumentException("Invalid navigationTarget for this host. localTarget=" + localTarget);
        }
    }
}
