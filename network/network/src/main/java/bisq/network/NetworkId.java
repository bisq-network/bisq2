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
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.PubKey;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
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

        NetworkDataValidation.validateId(nodeId);
    }

    public bisq.network.protobuf.NetworkId toProto() {
        return bisq.network.protobuf.NetworkId.newBuilder()
                .addAllAddressNetworkTypeTuple(AddressTransportTypeTuple.mapToProtoList(addressByNetworkType))
                .setPubKey(pubKey.toProto())
                .setNodeId(nodeId)
                .build();
    }

    public static NetworkId fromProto(bisq.network.protobuf.NetworkId proto) {
        return new NetworkId(AddressTransportTypeTuple.protoListToMap(proto.getAddressNetworkTypeTupleList()),
                PubKey.fromProto(proto.getPubKey()),
                proto.getNodeId());
    }


    public String getId() {
        return pubKey.getId();
    }

    private List<AddressTransportTypeTuple> getAddressByNetworkTypeAsList() {
        return addressByNetworkType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AddressTransportTypeTuple(e.getKey(), e.getValue()))
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
        return "NetworkId(" +
                "nodeId='" + nodeId + '\'' +
                ", addressByNetworkType=" + addressByNetworkType +
                ", pubKey=" + pubKey +
                ")";
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class AddressTransportTypeTuple implements Proto {
        private final Transport.Type transportType;
        private final Address address;

        public AddressTransportTypeTuple(Transport.Type transportType, Address address) {
            this.transportType = transportType;
            this.address = address;
        }

        public static String addressByNetworkTypeToString(Map<Transport.Type, Address> addressByNetworkType) {
            Set<String> addressByTypeSet = addressByNetworkType.entrySet().stream()
                    .map(e -> e.getKey().name() + "#" + e.getValue().getFullAddress())
                    .collect(Collectors.toSet());
            return Joiner.on("|").join(addressByTypeSet);
        }

        public static Map<Transport.Type, Address> setToAddressesByTypeMap(String addressAsString) {
            return Stream.of(addressAsString.split("\\|"))
                    .map(e -> List.of(e.split("#")))
                    .collect(Collectors.toMap(tokens -> Transport.Type.valueOf(tokens.get(0)),
                            list -> new Address(list.get(1))));
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

        public static List<bisq.network.protobuf.AddressTransportTypeTuple> mapToProtoList(Map<Transport.Type, Address> addressByNetworkType) {
            return addressByNetworkType.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new AddressTransportTypeTuple(e.getKey(), e.getValue()).toProto())
                    .collect(Collectors.toList());
        }

        public static Map<Transport.Type, Address> protoListToMap(List<bisq.network.protobuf.AddressTransportTypeTuple> addressNetworkTypeTupleList) {
            return addressNetworkTypeTupleList.stream()
                    .map(AddressTransportTypeTuple::fromProto)
                    .collect(Collectors.toMap(AddressTransportTypeTuple::getTransportType, AddressTransportTypeTuple::getAddress));
        }

    }
}
