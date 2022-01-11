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

import bisq.common.data.Pair;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode
@ToString
public class NetworkId implements Serializable {
    // We avoid maps in serialized data as it is used in hashes and maps do not have deterministic order.
    // The list is sorted by Transport.Type.
    private final List<Pair<Transport.Type, Address>> addresses;
    @Getter
    private final PubKey pubKey;
    @Getter
    private final String nodeId;
    // Lazy init convenience field. With java serialisation its always null even if declared in field.
    @Nullable
    private transient Map<Transport.Type, Address> addressByNetworkType;

    public NetworkId(Map<Transport.Type, Address> addressByNetworkType, PubKey pubKey, String nodeId) {
        this.pubKey = pubKey;
        this.nodeId = nodeId;
        checkArgument(!addressByNetworkType.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        addresses = addressByNetworkType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Pair<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public Map<Transport.Type, Address> addressByNetworkType() {
        if (addressByNetworkType == null) {
            addressByNetworkType = addresses.stream().collect(Collectors.toMap(Pair::first, Pair::second));
        }
        return addressByNetworkType;
    }
}
