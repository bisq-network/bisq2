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

package bisq.desktop.main.content.user.bonded_roles.nodes.tabs;

import bisq.bonded_roles.registration.BondedRoleType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.user.bonded_roles.nodes.tabs.registration.NodeRegistrationController;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabController;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabView;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class NodesTabController extends BondedRolesTabController<NodesTabModel> {

    public NodesTabController(ServiceProvider serviceProvider) {
        super(new NodesTabModel(), NavigationTarget.NODES_TABS, serviceProvider);
    }

    @Override
    protected BondedRolesTabView<NodesTabModel, NodesTabController> createAndGetView() {
        return new NodesTabView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case REGISTER_SEED_NODE: {
                return Optional.of(new NodeRegistrationController(serviceProvider, BondedRoleType.SEED_NODE));
            }
            case REGISTER_ORACLE_NODE: {
                return Optional.of(new NodeRegistrationController(serviceProvider, BondedRoleType.ORACLE_NODE));
            }
            case REGISTER_EXPLORER_NODE: {
                return Optional.of(new NodeRegistrationController(serviceProvider, BondedRoleType.EXPLORER_NODE));
            }
            case REGISTER_MARKET_PRICE_NODE: {
                return Optional.of(new NodeRegistrationController(serviceProvider, BondedRoleType.MARKET_PRICE_NODE));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
