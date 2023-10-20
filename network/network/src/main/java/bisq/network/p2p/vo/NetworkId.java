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

package bisq.network.p2p.vo;

import bisq.common.proto.Proto;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public final class NetworkId implements Proto {
    private final PubKey pubKey;
    private final String nodeId;
    private final AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();

    public NetworkId(AddressByTransportTypeMap addressByTransportTypeMap, PubKey pubKey, String nodeId) {
        this.pubKey = pubKey;
        this.nodeId = nodeId;
        checkArgument(!addressByTransportTypeMap.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        this.addressByTransportTypeMap.putAll(addressByTransportTypeMap);

        NetworkDataValidation.validateId(nodeId);
    }

    public bisq.network.protobuf.NetworkId toProto() {
        return bisq.network.protobuf.NetworkId.newBuilder()
                .setAddressByNetworkTypeMap(addressByTransportTypeMap.toProto())
                .setPubKey(pubKey.toProto())
                .setNodeId(nodeId)
                .build();
    }

    public static NetworkId fromProto(bisq.network.protobuf.NetworkId proto) {
        return new NetworkId(AddressByTransportTypeMap.fromProto(proto.getAddressByNetworkTypeMap()),
                PubKey.fromProto(proto.getPubKey()),
                proto.getNodeId());
    }

    public String getId() {
        return pubKey.getId();
    }


    @Override
    public String toString() {
        return "NetworkId(" +
                "nodeId='" + nodeId + '\'' +
                ", addressByTransportTypeMap=" + addressByTransportTypeMap +
                ", pubKey=" + pubKey +
                ")";
    }
}
