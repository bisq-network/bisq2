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

package bisq.desktop.primary.main.content.portfolio;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.FxTabController;
import bisq.desktop.primary.main.content.portfolio.closed.ClosedTradesController;
import bisq.desktop.primary.main.content.portfolio.openoffers.OpenOffersController;
import bisq.desktop.primary.main.content.portfolio.pending.PendingTradesController;
import lombok.Getter;

import java.util.Optional;

public class PortfolioController extends FxTabController {

    private final DefaultApplicationService applicationService;
    @Getter
    private final PortfolioModel model;
    @Getter
    private final PortfolioView view;

    public PortfolioController(DefaultApplicationService applicationService) {
        super(NavigationTarget.PORTFOLIO);

        this.applicationService = applicationService;
        model = new PortfolioModel();
        view = new PortfolioView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case OPEN_OFFERS -> {
                return Optional.of(new OpenOffersController(applicationService));
            }
            case PENDING_TRADES -> {
                return Optional.of(new PendingTradesController(applicationService));
            }
            case CLOSED_TRADES -> {
                return Optional.of(new ClosedTradesController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}