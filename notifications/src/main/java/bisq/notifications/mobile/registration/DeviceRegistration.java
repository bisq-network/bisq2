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
public final class DeviceRegistration implements PersistableProto {
    private final String deviceToken;
    private final String publicKeyBase64; // Base64 encoded public key for encrypting notifications
    private final DeviceRegistrationPlatform platform;
    private final long registrationTimestamp;

    public DeviceRegistration(String deviceToken, String publicKeyBase64, DeviceRegistrationPlatform platform) {
        this(deviceToken, publicKeyBase64, platform, System.currentTimeMillis());
    }

    public DeviceRegistration(String deviceToken,
                              String publicKeyBase64,
                              DeviceRegistrationPlatform platform,
                              long registrationTimestamp) {
        this.deviceToken = deviceToken;
        this.publicKeyBase64 = publicKeyBase64;
        this.platform = platform;
        this.registrationTimestamp = registrationTimestamp;
    }

    @Override
    public bisq.notifications.protobuf.DeviceRegistration toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.notifications.protobuf.DeviceRegistration.Builder getBuilder(boolean serializeForHash) {
        return bisq.notifications.protobuf.DeviceRegistration.newBuilder()
                .setDeviceToken(deviceToken)
                .setPublicKeyBase64(publicKeyBase64)
                .setPlatform(platform.toProtoEnum())
                .setRegistrationTimestamp(registrationTimestamp);
    }

    public static DeviceRegistration fromProto(bisq.notifications.protobuf.DeviceRegistration proto) {
        return new DeviceRegistration(proto.getDeviceToken(),
                proto.getPublicKeyBase64(),
                DeviceRegistrationPlatform.fromProto(proto.getPlatform()),
                proto.getRegistrationTimestamp());
    }
}

