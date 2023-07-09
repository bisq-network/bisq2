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

package bisq.desktop.main.content.user.bonded_roles.roles.tabs;

import bisq.bonded_roles.registration.BondedRoleType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.user.bonded_roles.roles.tabs.registration.RoleRegistrationController;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabController;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabView;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class RolesTabController extends BondedRolesTabController<RolesTabModel> {

    public RolesTabController(ServiceProvider serviceProvider) {
        super(new RolesTabModel(), NavigationTarget.ROLES_TABS, serviceProvider);
    }

    @Override
    protected BondedRolesTabView<RolesTabModel, RolesTabController> createAndGetView() {
        return new RolesTabView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case REGISTER_MEDIATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, BondedRoleType.MEDIATOR));
            }
            case REGISTER_ARBITRATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, BondedRoleType.ARBITRATOR));
            }
            case REGISTER_MODERATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, BondedRoleType.MODERATOR));
            }
            case REGISTER_SECURITY_MANAGER: {
                return Optional.of(new RoleRegistrationController(serviceProvider, BondedRoleType.SECURITY_MANAGER));
            }
            case REGISTER_RELEASE_MANAGER: {
                return Optional.of(new RoleRegistrationController(serviceProvider, BondedRoleType.RELEASE_MANAGER));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
