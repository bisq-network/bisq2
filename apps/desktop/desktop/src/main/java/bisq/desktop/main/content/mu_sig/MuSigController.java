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

package bisq.desktop.main.content.mu_sig;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.mu_sig.history.MuSigHistoryController;
import bisq.desktop.main.content.mu_sig.my_offers.MuSigMyOffersController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigOfferbookController;
import bisq.desktop.main.content.mu_sig.open_trades.MuSigOpenTradesController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;

import java.util.Optional;

public class MuSigController extends ContentTabController<MuSigModel> {
    @Getter
    private final MuSigView view;

    public MuSigController(ServiceProvider serviceProvider) {
        super(new MuSigModel(), NavigationTarget.MU_SIG, serviceProvider);

        view = new MuSigView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_OFFERBOOK -> Optional.of(new MuSigOfferbookController(serviceProvider));
            case MU_SIG_MY_OFFERS -> Optional.of(new MuSigMyOffersController(serviceProvider));
            case MU_SIG_OPEN_TRADES -> Optional.of(new MuSigOpenTradesController(serviceProvider));
            case MU_SIG_HISTORY -> Optional.of(new MuSigHistoryController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
