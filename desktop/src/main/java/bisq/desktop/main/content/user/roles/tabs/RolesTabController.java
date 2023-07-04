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

package bisq.desktop.main.content.user.roles.tabs;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.user.roles.tabs.registration.RoleRegistrationController;
import bisq.user.role.RoleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class RolesTabController extends TabController<RolesTabModel> {
    @Getter
    private final RolesTabView view;
    private final ServiceProvider serviceProvider;

    public RolesTabController(ServiceProvider serviceProvider) {
        super(new RolesTabModel(), NavigationTarget.ROLES_TABS);

        this.serviceProvider = serviceProvider;
        view = new RolesTabView(model, this);
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
            case REGISTER_MEDIATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, RoleType.MEDIATOR));
            }
            case REGISTER_ARBITRATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, RoleType.ARBITRATOR));
            }
            case REGISTER_MODERATOR: {
                return Optional.of(new RoleRegistrationController(serviceProvider, RoleType.MODERATOR));
            }
            case REGISTER_SECURITY_MANAGER: {
                return Optional.of(new RoleRegistrationController(serviceProvider, RoleType.SECURITY_MANAGER));
            }
            case REGISTER_RELEASE_MANAGER: {
                return Optional.of(new RoleRegistrationController(serviceProvider, RoleType.RELEASE_MANAGER));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
