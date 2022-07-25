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

package bisq.desktop.primary.main.content.settings.reputation.bond;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.reputation.bond.tab1.BondedReputationTab1Controller;
import bisq.desktop.primary.main.content.settings.reputation.bond.tab2.BondedReputationTab2Controller;
import bisq.desktop.primary.main.content.settings.reputation.bond.tab3.BondedReputationTab3Controller;
import bisq.desktop.primary.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BondedReputationController extends TabController<BondedReputationModel> {
    @Getter
    private final BondedReputationView view;
    private final DefaultApplicationService applicationService;

    public BondedReputationController(DefaultApplicationService applicationService) {
        super(new BondedReputationModel(), NavigationTarget.BSQ_BOND);

        this.applicationService = applicationService;
        view = new BondedReputationView(model, this);
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
            case BSQ_BOND_TAB_1: {
                return Optional.of(new BondedReputationTab1Controller(applicationService));
            }
            case BSQ_BOND_TAB_2: {
                return Optional.of(new BondedReputationTab2Controller(applicationService));
            }
            case BSQ_BOND_TAB_3: {
                return Optional.of(new BondedReputationTab3Controller(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }
}
