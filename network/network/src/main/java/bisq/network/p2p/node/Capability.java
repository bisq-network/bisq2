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

package bisq.network.p2p.node;

import bisq.common.proto.Proto;
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.node.transport.Transport;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class Capability implements Proto {
    private final Address address;
    private final Set<Transport.Type> supportedTransportTypes;

    public Capability(Address address, Set<Transport.Type> supportedTransportTypes) {
        this.address = address;
        this.supportedTransportTypes = supportedTransportTypes;
    }

    public bisq.network.protobuf.Capability toProto() {
        return bisq.network.protobuf.Capability.newBuilder()
                .setAddress(address.toProto())
                .addAllSupportedTransportTypes(supportedTransportTypes.stream()
                        .sorted(Enum::compareTo)
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Capability fromProto(bisq.network.protobuf.Capability proto) {
        Set<Transport.Type> supportedTransportTypes = proto.getSupportedTransportTypesList().stream()
                .map(e -> ProtobufUtils.enumFromProto(Transport.Type.class, e))
                .collect(Collectors.toSet());
        return new Capability(Address.fromProto(proto.getAddress()), supportedTransportTypes);
    }
}
