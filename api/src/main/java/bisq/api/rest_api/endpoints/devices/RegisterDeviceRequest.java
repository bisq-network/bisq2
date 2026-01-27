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

import bisq.notifications.mobile.registration.MobileDevicePlatform;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
/**
 * Request payload for registering a mobile device for push notifications.
 *
 * <p>
 * The device token is platform-specific (e.g. APNs for iOS, FCM for Android).
 * The public key is used to encrypt push notification payloads end-to-end.
 * </p>
 */
@Getter
@Schema(description = "Request payload for registering a mobile device for push notifications")
public class RegisterDeviceRequest {

    @Schema(
            description = "Client-generated stable device identifier",
            required = true,
            example = "3c2a9e1d8b7a6c5e4f3c2a4f3c2a9e1d8b7a6c5e4f9e1d8b7a6c5e4f3c2a9e"
    )
    private final String deviceId;

    @Schema(
            description = "Platform-specific push token used to deliver notifications",
            required = true,
            example = "4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e"
    )
    private final String deviceToken;

    @Schema(
            description = "Base64-encoded public key used to encrypt push notification payloads",
            required = true,
            example = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtX..."
    )
    private final String publicKeyBase64;

    @Schema(
            description = "Human-readable device descriptor (model, OS, or nickname)",
            required = true,
            example = "iPhone 11 Pro Max (iOS 17.2)"
    )
    private final String deviceDescriptor;

    @Schema(
            description = "Target platform of the device",
            required = true,
            example = "IOS"
    )
    private final MobileDevicePlatform platform;

    @JsonCreator
    public RegisterDeviceRequest(
            @JsonProperty("deviceId") String deviceId,
            @JsonProperty("deviceToken") String deviceToken,
            @JsonProperty("publicKeyBase64") String publicKeyBase64,
            @JsonProperty("deviceDescriptor") String deviceDescriptor,
            @JsonProperty("platform") MobileDevicePlatform platform
    ) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.publicKeyBase64 = publicKeyBase64;
        this.deviceDescriptor = deviceDescriptor;
        this.platform = platform;
    }
}
