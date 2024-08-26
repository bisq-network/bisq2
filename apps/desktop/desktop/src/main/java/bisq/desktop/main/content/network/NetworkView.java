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
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;

public class NetworkView extends ContentTabView<NetworkModel, NetworkController> {
    public NetworkView(NetworkModel model, NetworkController controller) {
        super(model, controller);

        addTab(Res.get("network.myNetworkNode"), NavigationTarget.MY_NETWORK_NODE);
        addTab(Res.get("network.p2pNetwork"), NavigationTarget.P2P_NETWORK);
        addTab(Res.get("network.roles"), NavigationTarget.ROLES);
        addTab(Res.get("network.nodes"), NavigationTarget.NODES);
    }
}
