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

package bisq.desktop.main.content.network;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.network.bonded_roles.nodes.NodesController;
import bisq.desktop.main.content.network.bonded_roles.roles.RolesController;
import bisq.desktop.main.content.network.my_node.MyNetworkNodeController;
import bisq.desktop.main.content.network.p2p.P2PNetworkController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class NetworkController extends ContentTabController<NetworkModel> {
    @Getter
    private final NetworkView view;

    public NetworkController(ServiceProvider serviceProvider) {
        super(new NetworkModel(), NavigationTarget.NETWORK, serviceProvider);

        view = new NetworkView(model, this);
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case MY_NETWORK_NODE: {
                return Optional.of(new MyNetworkNodeController(serviceProvider));
            }
            case P2P_NETWORK: {
                return Optional.of(new P2PNetworkController(serviceProvider));
            }
            case ROLES: {
                return Optional.of(new RolesController(serviceProvider));
            }
            case NODES: {
                return Optional.of(new NodesController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
