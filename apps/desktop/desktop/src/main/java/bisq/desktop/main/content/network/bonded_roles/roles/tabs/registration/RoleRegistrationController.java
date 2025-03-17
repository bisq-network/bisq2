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

package bisq.desktop.main.content.network.bonded_roles.roles.tabs.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.main.content.network.bonded_roles.tabs.registration.BondedRolesRegistrationController;
import bisq.desktop.main.content.network.bonded_roles.tabs.registration.BondedRolesRegistrationModel;
import bisq.desktop.main.content.network.bonded_roles.tabs.registration.BondedRolesRegistrationView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoleRegistrationController extends BondedRolesRegistrationController {
    public RoleRegistrationController(ServiceProvider serviceProvider, BondedRoleType bondedRoleType) {
        super(serviceProvider, bondedRoleType);
    }

    @Override
    protected BondedRolesRegistrationModel createAndGetModel() {
        return new RoleRegistrationModel(bondedRoleType);
    }

    @Override
    protected BondedRolesRegistrationView<RoleRegistrationModel, RoleRegistrationController> createAndGetView() {
        UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider, 20, true);
        return new RoleRegistrationView((RoleRegistrationModel) model, this, userProfileSelection);
    }
}
