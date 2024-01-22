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

import bisq.common.proto.NetworkProto;
import bisq.common.util.ProtobufUtils;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class Capability implements NetworkProto {
    private final Address address;
    private final List<TransportType> supportedTransportTypes;
    private final List<Feature> features;

    public Capability(Address address, List<TransportType> supportedTransportTypes, List<Feature> features) {
        this.address = address;
        this.supportedTransportTypes = supportedTransportTypes;
        this.features = features;

        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.supportedTransportTypes);
        Collections.sort(this.features);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(supportedTransportTypes.size() <= TransportType.values().length);
        checkArgument(features.size() <= Feature.values().length);
    }

    @Override
    public bisq.network.protobuf.Capability toProto() {
        return bisq.network.protobuf.Capability.newBuilder()
                .setAddress(address.toProto())
                .addAllSupportedTransportTypes(supportedTransportTypes.stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .addAllFeatures(features.stream()
                        .map(Feature::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Capability fromProto(bisq.network.protobuf.Capability proto) {
        List<TransportType> supportedTransportTypes = proto.getSupportedTransportTypesList().stream()
                .map(e -> ProtobufUtils.enumFromProto(TransportType.class, e))
                .collect(Collectors.toList());
        List<Feature> features = proto.getFeaturesList().stream()
                .map(Feature::fromProto)
                .collect(Collectors.toList());
        return new Capability(Address.fromProto(proto.getAddress()),
                supportedTransportTypes,
                features);
    }
}
