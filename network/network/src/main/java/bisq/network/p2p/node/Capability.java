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
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class Capability implements Proto {
    private final Address address;
    private final List<TransportType> supportedTransportTypes;

    public Capability(Address address, List<TransportType> supportedTransportTypes) {
        this.address = address;
        this.supportedTransportTypes = supportedTransportTypes;
        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.supportedTransportTypes);
    }

    public bisq.network.protobuf.Capability toProto() {
        return bisq.network.protobuf.Capability.newBuilder()
                .setAddress(address.toProto())
                .addAllSupportedTransportTypes(supportedTransportTypes.stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Capability fromProto(bisq.network.protobuf.Capability proto) {
        List<TransportType> supportedTransportTypes = proto.getSupportedTransportTypesList().stream()
                .map(e -> ProtobufUtils.enumFromProto(TransportType.class, e))
                .collect(Collectors.toList());
        return new Capability(Address.fromProto(proto.getAddress()), supportedTransportTypes);
    }
}
