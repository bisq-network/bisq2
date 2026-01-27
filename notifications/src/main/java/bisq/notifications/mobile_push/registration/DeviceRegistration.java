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

package bisq.notifications.mobile_push.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a registered device for push notifications.
 * Stores the device token, public key for encryption, and platform type.
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeviceRegistration {
    public enum Platform {
        IOS,
        ANDROID // For future use
    }

    private final String deviceToken;
    private final String publicKey; // Base64 encoded public key for encrypting notifications
    private final Platform platform;
    private final long registrationTimestamp;


    public DeviceRegistration(String deviceToken, String publicKey, Platform platform) {
        this(deviceToken, publicKey, platform, System.currentTimeMillis());
    }

    @JsonCreator
    public DeviceRegistration(@JsonProperty("deviceToken") String deviceToken,
                              @JsonProperty("publicKey") String publicKey,
                              @JsonProperty("platform") Platform platform,
                              @JsonProperty("registrationTimestamp") long registrationTimestamp) {
        this.deviceToken = deviceToken;
        this.publicKey = publicKey;
        this.platform = platform;
        this.registrationTimestamp = registrationTimestamp;
    }
}

