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

package network.misq.network.p2p;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.security.PubKey;

import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode
@ToString
public class NetworkId implements Serializable {
    private final Map<Transport.Type, Address> addressByNetworkType;
    private final PubKey pubKey;
    private final String nodeId;

    public NetworkId(Map<Transport.Type, Address> addressByNetworkType, PubKey pubKey, String nodeId) {
        this.addressByNetworkType = addressByNetworkType;
        this.pubKey = pubKey;
        this.nodeId = nodeId;
    }

    public Map<Transport.Type, Address> addressByNetworkType() {
        return addressByNetworkType;
    }

    public PubKey pubKey() {
        return pubKey;
    }

    public String nodeId() {
        return nodeId;
    }
}
