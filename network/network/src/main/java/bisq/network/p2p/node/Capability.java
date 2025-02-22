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
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.ProtobufUtils;
import bisq.common.validation.NetworkDataValidation;
import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
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
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final List<Feature> features;
    private final String applicationVersion;

    public static Capability myCapability(Address address,
                                          List<TransportType> supportedTransportTypes,
                                          List<Feature> features) {
        return new Capability(VERSION, address, supportedTransportTypes, features, ApplicationVersion.getVersion().getVersionAsString());
    }

    @VisibleForTesting
    public Capability(int version,
                      Address address,
                      List<TransportType> supportedTransportTypes,
                      List<Feature> features,
                      String applicationVersion) {
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
        List<bisq.network.protobuf.Feature> featuresListProto = proto.getFeaturesList();
        List<Feature> features = ProtobufUtils.fromProtoEnumList(Feature.class, featuresListProto);
        if (featuresListProto.size() != features.size()) {
            log.warn("The size of the resolved feature list is not same as the protobuf list's size. " +
                    "This can happen if new, unrecognized protobuf elements have been received. Old clients ignore such new fields.");
        }
        return new Capability(proto.getVersion(),
                Address.fromProto(proto.getAddress()),
                supportedTransportTypes,
                features,
                proto.getApplicationVersion());
    }
}
