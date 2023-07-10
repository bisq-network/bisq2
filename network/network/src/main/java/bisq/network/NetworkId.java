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
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public final class NetworkId implements Proto {
    private final PubKey pubKey;
    private final String nodeId;
    private final Map<Transport.Type, Address> addressByNetworkType = new HashMap<>();

    public NetworkId(Map<Transport.Type, Address> addressByNetworkType, PubKey pubKey, String nodeId) {
        this.pubKey = pubKey;
        this.nodeId = nodeId;
        checkArgument(!addressByNetworkType.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        this.addressByNetworkType.putAll(addressByNetworkType);
    }

    public bisq.network.protobuf.NetworkId toProto() {
        List<bisq.network.protobuf.AddressTransportTypeTuple> tuple = addressByNetworkType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AddressTransportTypeTuple(e.getKey(), e.getValue()).toProto())
                .collect(Collectors.toList());
        return bisq.network.protobuf.NetworkId.newBuilder()
                .addAllAddressNetworkTypeTuple(tuple)
                .setPubKey(pubKey.toProto())
                .setNodeId(nodeId)
                .build();
    }

    public static NetworkId fromProto(bisq.network.protobuf.NetworkId proto) {
        Map<Transport.Type, Address> addressByNetworkType = proto.getAddressNetworkTypeTupleList().stream()
                .map(AddressTransportTypeTuple::fromProto)
                .collect(Collectors.toMap(e -> e.transportType, e -> e.address));
        return new NetworkId(addressByNetworkType, PubKey.fromProto(proto.getPubKey()), proto.getNodeId());
    }

    public String getId() {
        return pubKey.getId();
    }

    private List<AddressTransportTypeTuple> getAddressByNetworkTypeAsList() {
        return addressByNetworkType.entrySet().stream()
                .map(e -> new AddressTransportTypeTuple(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AddressTransportTypeTuple::getAddress))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkId networkId = (NetworkId) o;

        if (pubKey != null ? !pubKey.equals(networkId.pubKey) : networkId.pubKey != null) return false;
        if (nodeId != null ? !nodeId.equals(networkId.nodeId) : networkId.nodeId != null) return false;
        List<AddressTransportTypeTuple> list1 = getAddressByNetworkTypeAsList();
        List<AddressTransportTypeTuple> list2 = networkId.getAddressByNetworkTypeAsList();
        return list1 != null ? list1.equals(list2) : list2 == null;
    }

    @Override
    public int hashCode() {
        int result = pubKey != null ? pubKey.hashCode() : 0;
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        List<AddressTransportTypeTuple> list = getAddressByNetworkTypeAsList();
        result = 31 * result + (list != null ? list.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkId{" +
                "\r\n     pubKey=" + pubKey +
                ",\r\n     nodeId='" + nodeId + '\'' +
                ",\r\n     addressByNetworkType=" + addressByNetworkType +
                "\r\n}";
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    private static final class AddressTransportTypeTuple implements Proto {
        private final Transport.Type transportType;
        private final Address address;

        private AddressTransportTypeTuple(Transport.Type transportType, Address address) {
            this.transportType = transportType;
            this.address = address;
        }

        public bisq.network.protobuf.AddressTransportTypeTuple toProto() {
            return bisq.network.protobuf.AddressTransportTypeTuple.newBuilder()
                    .setTransportType(transportType.name())
                    .setAddress(address.toProto())
                    .build();
        }

        public static AddressTransportTypeTuple fromProto(bisq.network.protobuf.AddressTransportTypeTuple proto) {
            Transport.Type transportType = ProtobufUtils.enumFromProto(Transport.Type.class, proto.getTransportType());
            return new AddressTransportTypeTuple(transportType, Address.fromProto(proto.getAddress()));
        }
    }
}
