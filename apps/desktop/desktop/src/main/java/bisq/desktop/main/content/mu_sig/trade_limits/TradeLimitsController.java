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

package bisq.desktop.main.content.mu_sig.trade_limits;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.mu_sig.trade_limits.tab1.TradeLimitsTab1Controller;
import bisq.desktop.main.content.mu_sig.trade_limits.tab2.TradeLimitsTab2Controller;
import bisq.desktop.main.content.mu_sig.trade_limits.tab3.TradeLimitsTab3Controller;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeLimitsController extends TabController<TradeLimitsModel> {
    @Getter
    private final TradeLimitsView view;
    private final ServiceProvider serviceProvider;

    public TradeLimitsController(ServiceProvider serviceProvider) {
        super(new TradeLimitsModel(), NavigationTarget.MU_SIG_TRADE_LIMITS);

        this.serviceProvider = serviceProvider;
        view = new TradeLimitsView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_TRADE_LIMITS_TAB_1 -> Optional.of(new TradeLimitsTab1Controller(serviceProvider));
            case MU_SIG_TRADE_LIMITS_TAB_2 -> Optional.of(new TradeLimitsTab2Controller(serviceProvider));
            case MU_SIG_TRADE_LIMITS_TAB_3 -> Optional.of(new TradeLimitsTab3Controller(serviceProvider));
            default -> Optional.empty();
        };
    }

    void onClose() {
        OverlayController.hide();
    }
}
