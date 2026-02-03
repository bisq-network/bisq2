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

package bisq.http_api.push_notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Represents a registered mobile device for push notifications.
 * Stores the device ID, token, public key for encryption, descriptor, and platform type.
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MobileDeviceProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String deviceId;
    private final String deviceToken;
    private final String publicKeyBase64;
    private final String deviceDescriptor;
    private final MobileDevicePlatform platform;
    private final long registrationTimestamp;

    public MobileDeviceProfile(String deviceId,
                               String deviceToken,
                               String publicKeyBase64,
                               String deviceDescriptor,
                               MobileDevicePlatform platform) {
        this(deviceId, deviceToken, publicKeyBase64, deviceDescriptor, platform, System.currentTimeMillis());
    }

    @JsonCreator
    public MobileDeviceProfile(@JsonProperty("deviceId") String deviceId,
                               @JsonProperty("deviceToken") String deviceToken,
                               @JsonProperty("publicKeyBase64") String publicKeyBase64,
                               @JsonProperty("deviceDescriptor") String deviceDescriptor,
                               @JsonProperty("platform") MobileDevicePlatform platform,
                               @JsonProperty("registrationTimestamp") long registrationTimestamp) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.publicKeyBase64 = publicKeyBase64;
        this.deviceDescriptor = deviceDescriptor;
        this.platform = platform;
        this.registrationTimestamp = registrationTimestamp;
    }
}

