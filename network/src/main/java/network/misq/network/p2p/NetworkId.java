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
import network.misq.common.data.Pair;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.security.PubKey;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode
@ToString
public class NetworkId implements Serializable {
    private final List<Pair<Transport.Type, Address>> addresses;
    private final PubKey pubKey;
    private final String nodeId;
    @Nullable
    private transient Map<Transport.Type, Address> addressByNetworkType;

    public NetworkId(Map<Transport.Type, Address> addressByNetworkType, PubKey pubKey, String nodeId) {
        checkArgument(!addressByNetworkType.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        addresses = addressByNetworkType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Pair<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        this.pubKey = pubKey;
        this.nodeId = nodeId;
    }

    public Map<Transport.Type, Address> addressByNetworkType() {
        if (addressByNetworkType == null) {
            addressByNetworkType = new HashMap<>();
            addresses.forEach(pair -> {
                addressByNetworkType.put(pair.first(), pair.second());
            });
        }
        return addressByNetworkType;
    }

    public PubKey pubKey() {
        return pubKey;
    }

    public String nodeId() {
        return nodeId;
    }
}
