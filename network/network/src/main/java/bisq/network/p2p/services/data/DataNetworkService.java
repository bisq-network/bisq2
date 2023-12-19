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

package bisq.network.p2p.services.data;

import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import lombok.extern.slf4j.Slf4j;

/**
 * The network service for each transport used for the dataService to delegate received messages from any transport
 * and to add the Broadcaster for any transport to the data service.
 */
@Slf4j
public class DataNetworkService implements Node.Listener {
    private final Node node;
    private final DataService dataService;
    private final Broadcaster broadcaster;

    public DataNetworkService(Node node, DataService dataService) {
        this.node = node;
        this.dataService = dataService;
        broadcaster = new Broadcaster(node);
        node.addListener(this);
        dataService.addBroadcaster(broadcaster);
    }

    public void shutdown() {
        node.removeListener(this);
        dataService.removeBroadcaster(broadcaster);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof AddDataRequest) {
            dataService.processAddDataRequest((AddDataRequest) envelopePayloadMessage, true);
        } else if (envelopePayloadMessage instanceof RemoveDataRequest) {
            dataService.processRemoveDataRequest((RemoveDataRequest) envelopePayloadMessage, true);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }
}
