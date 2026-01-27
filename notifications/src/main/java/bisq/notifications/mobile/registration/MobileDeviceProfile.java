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

import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class MobileDeviceProfile implements PersistableProto {
    private final String deviceId;
    private final String deviceToken;
    private final String publicKeyBase64;
    private final String deviceDescriptor;
    private final MobileDevicePlatform platform;

    public MobileDeviceProfile(String deviceId,
                               String deviceToken,
                               String publicKeyBase64,
                               String deviceDescriptor,
                               MobileDevicePlatform platform
    ) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.publicKeyBase64 = publicKeyBase64;
        this.deviceDescriptor = deviceDescriptor;
        this.platform = platform;
    }

    @Override
    public bisq.notifications.protobuf.MobileDeviceProfile toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.notifications.protobuf.MobileDeviceProfile.Builder getBuilder(boolean serializeForHash) {
        return bisq.notifications.protobuf.MobileDeviceProfile.newBuilder()
                .setDeviceId(deviceId)
                .setDeviceToken(deviceToken)
                .setPublicKeyBase64(publicKeyBase64)
                .setDeviceDescriptor(deviceDescriptor)
                .setPlatform(platform.toProtoEnum());
    }

    public static MobileDeviceProfile fromProto(bisq.notifications.protobuf.MobileDeviceProfile proto) {
        return new MobileDeviceProfile(proto.getDeviceId(),
                proto.getDeviceToken(),
                proto.getPublicKeyBase64(),
                proto.getDeviceDescriptor(),
                MobileDevicePlatform.fromProto(proto.getPlatform()));
    }
}

