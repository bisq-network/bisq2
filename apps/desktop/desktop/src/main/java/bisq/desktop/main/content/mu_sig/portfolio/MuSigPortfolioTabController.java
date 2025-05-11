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

package bisq.desktop.main.content.mu_sig.portfolio;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabView;
import bisq.desktop.main.content.mu_sig.portfolio.history.MuSigHistoryController;
import bisq.desktop.main.content.mu_sig.portfolio.offers.MuSigOpenOffersController;
import bisq.desktop.main.content.mu_sig.portfolio.trades.MuSigOpenTradesController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigPortfolioTabController extends MuSigLevel2TabController<MuSigPortfolioTabModel> {

    public MuSigPortfolioTabController(ServiceProvider serviceProvider) {
        super(new MuSigPortfolioTabModel(), NavigationTarget.MU_SIG_PORTFOLIO, serviceProvider);
    }

    @Override
    protected MuSigLevel2TabView<MuSigPortfolioTabModel, MuSigPortfolioTabController> createAndGetView() {
        return new MuSigPortfolioTabView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_OPEN_OFFERS ->
                    Optional.of(new MuSigOpenOffersController(serviceProvider));
            case MU_SIG_OPEN_TRADES ->
                    Optional.of(new MuSigOpenTradesController(serviceProvider));
            case MU_SIG_HISTORY ->
                    Optional.of(new MuSigHistoryController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
