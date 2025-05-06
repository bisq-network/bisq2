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

package bisq.desktop.main.content.mu_sig.offerbook.sell;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabView;
import bisq.desktop.main.content.mu_sig.offerbook.btc.MuSigOfferbookBtcController;
import bisq.desktop.main.content.mu_sig.offerbook.other.MuSigOfferbookOtherController;
import bisq.desktop.main.content.mu_sig.offerbook.xmr.MuSigOfferbookXmrController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.offer.Direction;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigSellOfferbookTabController extends MuSigLevel2TabController<MuSigSellOfferbookTabModel> {

    public MuSigSellOfferbookTabController(ServiceProvider serviceProvider) {
        super(new MuSigSellOfferbookTabModel(), NavigationTarget.MU_SIG_OFFERBOOK_SELL, serviceProvider);
    }

    @Override
    protected MuSigLevel2TabView<MuSigSellOfferbookTabModel, MuSigSellOfferbookTabController> createAndGetView() {
        return new MuSigSellOfferbookTabView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_OFFERBOOK_SELL_BTC ->
                    Optional.of(new MuSigOfferbookBtcController(serviceProvider, Direction.SELL));
            case MU_SIG_OFFERBOOK_SELL_XMR ->
                    Optional.of(new MuSigOfferbookXmrController(serviceProvider, Direction.SELL));
            case MU_SIG_OFFERBOOK_SELL_OTHER ->
                    Optional.of(new MuSigOfferbookOtherController(serviceProvider, Direction.SELL));
            default -> Optional.empty();
        };
    }
}
