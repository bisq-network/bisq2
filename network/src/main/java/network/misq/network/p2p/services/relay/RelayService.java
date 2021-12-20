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

package network.misq.network.p2p.services.relay;

import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

//TODO
public class RelayService {
    public RelayService(Node node) {

    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Connection> relay(Message message, NetworkId networkId, KeyPair myKeyPair) {
       /*   Set<Connection> connections = getConnectionsWithSupportedNetwork(peerAddress.getNetworkType());
      Connection outboundConnection = CollectionUtil.getRandomElement(connections);
        if (outboundConnection != null) {
            //todo we need 2 diff. pub keys for encryption here
            // ConfidentialMessage inner = seal(message);
            // RelayMessage relayMessage = new RelayMessage(inner, peerAddress);
            // ConfidentialMessage confidentialMessage = seal(relayMessage);
            // return node.send(confidentialMessage, outboundConnection);
        }*/
        return CompletableFuture.failedFuture(new Exception("No connection supporting that network type found."));
    }
}