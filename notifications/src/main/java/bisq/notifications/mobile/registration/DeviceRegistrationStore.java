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

package bisq.notifications.mobile.registration;

import bisq.common.observable.map.ObservableHashMap;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public final class DeviceRegistrationStore implements PersistableStore<DeviceRegistrationStore> {
    @Getter
    private final ObservableHashMap<String, MobileDeviceProfile> deviceByDeviceId = new ObservableHashMap<>();

    private DeviceRegistrationStore(Map<String, MobileDeviceProfile> deviceByDeviceId) {
        this.deviceByDeviceId.putAll(deviceByDeviceId);
    }

    @Override
    public bisq.notifications.protobuf.DeviceRegistrationStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.notifications.protobuf.DeviceRegistrationStore.newBuilder()
                .putAllDeviceByDeviceId(deviceByDeviceId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))));
    }

    @Override
    public bisq.notifications.protobuf.DeviceRegistrationStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static DeviceRegistrationStore fromProto(bisq.notifications.protobuf.DeviceRegistrationStore proto) {
        return new DeviceRegistrationStore(
                proto.getDeviceByDeviceIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> MobileDeviceProfile.fromProto(e.getValue())
                        )));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.notifications.protobuf.DeviceRegistrationStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public DeviceRegistrationStore getClone() {
        return new DeviceRegistrationStore(Map.copyOf(deviceByDeviceId));
    }

    @Override
    public void applyPersisted(DeviceRegistrationStore persisted) {
        deviceByDeviceId.clear();
        deviceByDeviceId.putAll(persisted.deviceByDeviceId);
    }
}