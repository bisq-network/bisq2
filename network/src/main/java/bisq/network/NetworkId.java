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

package bisq.network;

import bisq.common.proto.Proto;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode
@ToString
@Getter
public class NetworkId implements Proto {
    private final PubKey pubKey;
    private final String nodeId;
    // NetworkId is used in objects which gets hashed, so we need to have a deterministic order in the map. 
    // A treeMap guarantees that. 
    private final TreeMap<Transport.Type, Address> addressByNetworkType = new TreeMap<>();

    public NetworkId(Map<Transport.Type, Address> addressByNetworkType, PubKey pubKey, String nodeId) {
        this.pubKey = pubKey;
        this.nodeId = nodeId;
        checkArgument(!addressByNetworkType.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        this.addressByNetworkType.putAll(addressByNetworkType);
    }
}
