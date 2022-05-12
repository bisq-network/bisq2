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

package bisq.desktop.primary.main.content.trade;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.closedTrades.GlobalClosedTradesController;
import bisq.desktop.primary.main.content.trade.offerbook.GlobalOfferbookController;
import bisq.desktop.primary.main.content.trade.openoffers.GlobalOpenOffersController;
import bisq.desktop.primary.main.content.trade.overview.TradeOverviewListController;
import bisq.desktop.primary.main.content.trade.overview.TradeOverviewGridController;
import bisq.desktop.primary.main.content.trade.pendingTrades.GlobalPendingTradesController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeController extends TabController<TradeModel> {
    private final DefaultApplicationService applicationService;
    @Getter
    private final TradeView view;

    public TradeController(DefaultApplicationService applicationService) {
        super(new TradeModel(), NavigationTarget.TRADE);

        this.applicationService = applicationService;

        view = new TradeView(model, this);
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
            case TRADE_OVERVIEW_LIST -> {
                return Optional.of(new TradeOverviewListController(applicationService));
            }
            case TRADE_OVERVIEW_GRID -> {
                return Optional.of(new TradeOverviewGridController(applicationService));
            }
            case OFFERBOOK -> {
                return Optional.of(new GlobalOfferbookController(applicationService));
            }
            case OPEN_OFFERS -> {
                return Optional.of(new GlobalOpenOffersController(applicationService));
            }
            case PENDING_TRADES -> {
                return Optional.of(new GlobalPendingTradesController(applicationService));
            }
            case CLOSED_TRADES -> {
                return Optional.of(new GlobalClosedTradesController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
