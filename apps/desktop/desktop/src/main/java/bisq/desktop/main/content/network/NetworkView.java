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

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.network.peers.NetworkPeersView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import javafx.scene.Parent;

public class NetworkView extends ContentTabView<NetworkModel, NetworkController> {
    public NetworkView(NetworkModel model, NetworkController controller) {
        super(model, controller);

        addTab(Res.get("network.p2pNetwork"), NavigationTarget.NETWORK_P2P);
        addTab(Res.get("network.peers"), NavigationTarget.NETWORK_PEERS);
        addTab(Res.get("network.roles"), NavigationTarget.ROLES);
        addTab(Res.get("network.nodes"), NavigationTarget.NODES);
    }

    @Override
    protected boolean useFitToHeight(View<? extends Parent, ? extends Model, ? extends Controller> childView) {
        return childView instanceof NetworkPeersView;
    }
}
