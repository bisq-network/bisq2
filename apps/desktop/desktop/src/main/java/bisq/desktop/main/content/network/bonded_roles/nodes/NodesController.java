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

package bisq.desktop.main.content.network.bonded_roles.nodes;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesController;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesModel;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesView;
import bisq.desktop.main.content.network.bonded_roles.nodes.tabs.NodesTabController;
import bisq.desktop.main.content.network.bonded_roles.tabs.BondedRolesTabController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodesController extends BondedRolesController {

    public NodesController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected BondedRolesTabController<?> createAndGetNodesTabController() {
        return new NodesTabController(serviceProvider);
    }

    @Override
    protected BondedRolesModel createAndGetModel() {
        return new NodesModel();
    }

    @Override
    protected BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView() {
        return new NodesView((NodesModel) model, this, bondedRolesTabController.getView().getRoot());
    }

    @Override
    protected void handleNavigationTargetChange(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case REGISTER_SEED_NODE -> model.getSelectedBondedRoleType().set(BondedRoleType.SEED_NODE);
            case REGISTER_ORACLE_NODE -> model.getSelectedBondedRoleType().set(BondedRoleType.ORACLE_NODE);
            case REGISTER_EXPLORER_NODE -> model.getSelectedBondedRoleType().set(BondedRoleType.EXPLORER_NODE);
            case REGISTER_MARKET_PRICE_NODE -> model.getSelectedBondedRoleType().set(BondedRoleType.MARKET_PRICE_NODE);
        }
    }
}
