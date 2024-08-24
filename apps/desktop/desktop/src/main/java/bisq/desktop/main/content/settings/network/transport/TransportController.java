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

package bisq.desktop.main.content.settings.network.transport;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransportController implements Controller {
    private final TransportModel model;
    @Getter
    private final TransportView view;


    public TransportController(ServiceProvider serviceProvider, TransportType transportType) {
        ServiceNode serviceNode = serviceProvider.getNetworkService().findServiceNode(transportType).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();

        Traffic traffic = new Traffic(serviceProvider, transportType);
        SystemLoad systemLoad = new SystemLoad(serviceProvider, transportType);
        ConnectionsAndNodes connectionsAndNodes = new ConnectionsAndNodes(serviceProvider, transportType);
        model = new TransportModel(transportType, defaultNode);
        view = new TransportView(model, this,
                traffic.getViewRoot(),
                systemLoad.getViewRoot(),
                connectionsAndNodes.getViewRoot());
    }

    @Override
    public void onActivate() {
        model.setMyDefaultNodeAddress(model.getDefaultNode().findMyAddress()
                .map(Address::getFullAddress)
                .orElseGet(() -> Res.get("data.na")));
    }

    @Override
    public void onDeactivate() {
    }
}
