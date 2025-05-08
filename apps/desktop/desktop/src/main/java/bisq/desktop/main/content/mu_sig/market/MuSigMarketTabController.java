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

package bisq.desktop.main.content.mu_sig.market;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.market.chart.MuSigMarketChartController;
import bisq.desktop.main.content.mu_sig.market.currency.MuSigMarketByCurrencyController;
import bisq.desktop.main.content.mu_sig.market.payment_methods.MuSigMarketByPaymentMethodController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabView;
import bisq.desktop.navigation.NavigationTarget;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigMarketTabController extends MuSigLevel2TabController<MuSigMarketTabModel> {

    public MuSigMarketTabController(ServiceProvider serviceProvider) {
        super(new MuSigMarketTabModel(), NavigationTarget.MU_SIG_MARKET, serviceProvider);
    }

    @Override
    protected MuSigLevel2TabView<MuSigMarketTabModel, MuSigMarketTabController> createAndGetView() {
        return new MuSigMarketTabView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_MARKET_CHART -> Optional.of(new MuSigMarketChartController(serviceProvider));
            case MU_SIG_MARKET_BY_CURRENCY -> Optional.of(new MuSigMarketByCurrencyController(serviceProvider));
            case MU_SIG_MARKET_BY_PAYMENT_METHODS ->
                    Optional.of(new MuSigMarketByPaymentMethodController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
