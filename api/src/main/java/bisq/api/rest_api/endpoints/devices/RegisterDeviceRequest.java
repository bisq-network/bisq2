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

package bisq.api.rest_api.endpoints.devices;

import bisq.notifications.mobile.registration.DeviceRegistrationPlatform;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Request to register a device for push notifications.
 *
 * <p>
 * The device token is platform-specific (e.g. APNs for iOS, FCM for Android).
 * The public key is used to encrypt notification payloads end-to-end.
 * </p>
 */
@Getter
@Schema(description = "Request payload for registering a device for push notifications")
public class RegisterDeviceRequest {

    @Schema(
            description = "User profile identifier",
            required = true,
            example = "d22d7b62ef442b5df03378f134bc8f54a2171cba"
    )
    private final String userProfileId;

    @Schema(
            description = "Platform-specific device token (e.g. APNs for iOS)",
            required = true,
            example = "4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e"
    )
    private final String deviceToken;

    @Schema(
            description = "Base64-encoded public key used to encrypt push notification payloads",
            required = true,
            example = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtX..."
    )
    private final String publicKey;

    @Schema(
            description = "Target platform for the device",
            required = true,
            example = "IOS"
    )
    private final DeviceRegistrationPlatform platform;

    @JsonCreator
    public RegisterDeviceRequest(
            @JsonProperty("userProfileId") String userProfileId,
            @JsonProperty("deviceToken") String deviceToken,
            @JsonProperty("publicKey") String publicKey,
            @JsonProperty("platform") DeviceRegistrationPlatform platform
    ) {
        this.userProfileId = userProfileId;
        this.deviceToken = deviceToken;
        this.publicKey = publicKey;
        this.platform = platform;
    }
}
