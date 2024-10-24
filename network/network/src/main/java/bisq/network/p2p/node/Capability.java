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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.ApplicationVersion;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.ProtobufUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class Capability implements NetworkProto {
    public static final int VERSION = 1;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final Address address;
    private final List<TransportType> supportedTransportTypes;
    // ExcludeForHash from version 1 on to not break hash for pow check or version 0. We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final List<Feature> features;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final String applicationVersion;

    public static Capability myCapability(Address address, List<TransportType> supportedTransportTypes, List<Feature> features) {
        return new Capability(VERSION, address, supportedTransportTypes, features, ApplicationVersion.getVersion().getVersionAsString());
    }

    public static Capability withVersion(Capability capability, int version) {
        return new Capability(version,
                capability.getAddress(),
                new ArrayList<>(capability.getSupportedTransportTypes()),
                new ArrayList<>(capability.getFeatures()),
                capability.getApplicationVersion());
    }

    @VisibleForTesting
    public Capability(int version, Address address, List<TransportType> supportedTransportTypes, List<Feature> features, String applicationVersion) {
        this.version = version;
        this.address = address;
        this.supportedTransportTypes = supportedTransportTypes;
        this.features = features;
        this.applicationVersion = applicationVersion;

        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.supportedTransportTypes);
        Collections.sort(this.features);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(supportedTransportTypes.size() <= TransportType.values().length);
        checkArgument(features.size() <= Feature.values().length);
        if (version > 0) {
            NetworkDataValidation.validateVersion(applicationVersion);
        }
    }

    @Override
    public bisq.network.protobuf.Capability.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.Capability.newBuilder()
                .setVersion(version)
                .setAddress(address.toProto(serializeForHash))
                .addAllSupportedTransportTypes(supportedTransportTypes.stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .addAllFeatures(features.stream()
                        .map(Feature::toProtoEnum)
                        .collect(Collectors.toList()))
                .setApplicationVersion(applicationVersion);
    }

    @Override
    public bisq.network.protobuf.Capability toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Capability fromProto(bisq.network.protobuf.Capability proto) {
        List<TransportType> supportedTransportTypes = proto.getSupportedTransportTypesList().stream()
                .map(e -> ProtobufUtils.enumFromProto(TransportType.class, e))
                .collect(Collectors.toList());
        return new Capability(proto.getVersion(),
                Address.fromProto(proto.getAddress()),
                supportedTransportTypes,
                ProtobufUtils.fromProtoEnumList(Feature.class, proto.getFeaturesList()),
                proto.getApplicationVersion());
    }
}
