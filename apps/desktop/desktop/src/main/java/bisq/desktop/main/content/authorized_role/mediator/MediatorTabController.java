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

package bisq.desktop.main.content.authorized_role.mediator;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.authorized_role.mediator.bisq_easy.BisqEasyMediatorController;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediatorController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MediatorTabController extends TabController<MediatorTabModel> {
    @Getter
    protected final MediatorTabView view;
    private final ServiceProvider serviceProvider;

    public MediatorTabController(ServiceProvider serviceProvider) {
        super(new MediatorTabModel(), NavigationTarget.MEDIATOR);

        this.serviceProvider = serviceProvider;

        view = new MediatorTabView(model, this);
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
            case BISQ_EASY_MEDIATOR -> Optional.of(new BisqEasyMediatorController(serviceProvider));
            case MU_SIG_MEDIATOR -> Optional.of(new MuSigMediatorController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
