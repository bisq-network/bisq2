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

package bisq.http_api.rest_api.domain.devices;

import bisq.http_api.push_notification.MobileDevicePlatform;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Request to register a mobile device for push notifications.
 */
@Getter
@Schema(description = "Request to register a mobile device for push notifications")
public class RegisterDeviceRequest {
    @Schema(description = "Unique device identifier", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private final String deviceId;

    @Schema(description = "APNs/FCM device token", required = true, example = "abc123def456...")
    private final String deviceToken;

    @Schema(description = "Base64 encoded public key for encrypting notifications", required = true)
    private final String publicKeyBase64;

    @Schema(description = "Human-readable device description", required = true, example = "iPhone 15 Pro")
    private final String deviceDescriptor;

    @Schema(description = "Platform type", required = true, example = "IOS")
    private final MobileDevicePlatform platform;

    @JsonCreator
    public RegisterDeviceRequest(@JsonProperty("deviceId") String deviceId,
                                 @JsonProperty("deviceToken") String deviceToken,
                                 @JsonProperty("publicKeyBase64") String publicKeyBase64,
                                 @JsonProperty("deviceDescriptor") String deviceDescriptor,
                                 @JsonProperty("platform") MobileDevicePlatform platform) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.publicKeyBase64 = publicKeyBase64;
        this.deviceDescriptor = deviceDescriptor;
        this.platform = platform;
    }
}

