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

package bisq.desktop.main.content.network.bonded_roles.roles;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesController;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesModel;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesView;
import bisq.desktop.main.content.network.bonded_roles.roles.tabs.RolesTabController;
import bisq.desktop.main.content.network.bonded_roles.tabs.BondedRolesTabController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolesController extends BondedRolesController {

    public RolesController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected BondedRolesTabController<?> createAndGetNodesTabController() {
        return new RolesTabController(serviceProvider);
    }

    @Override
    protected BondedRolesModel createAndGetModel() {
        return new RolesModel();
    }

    @Override
    protected BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView() {
        return new RolesView((RolesModel) model, this, bondedRolesTabController.getView().getRoot());
    }

    @Override
    protected void handleNavigationTargetChange(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case REGISTER_MEDIATOR -> model.getSelectedBondedRoleType().set(BondedRoleType.MEDIATOR);
            case REGISTER_ARBITRATOR -> model.getSelectedBondedRoleType().set(BondedRoleType.ARBITRATOR);
            case REGISTER_MODERATOR -> model.getSelectedBondedRoleType().set(BondedRoleType.MODERATOR);
            case REGISTER_SECURITY_MANAGER -> model.getSelectedBondedRoleType().set(BondedRoleType.SECURITY_MANAGER);
            case REGISTER_RELEASE_MANAGER -> model.getSelectedBondedRoleType().set(BondedRoleType.RELEASE_MANAGER);
        }
    }
}
