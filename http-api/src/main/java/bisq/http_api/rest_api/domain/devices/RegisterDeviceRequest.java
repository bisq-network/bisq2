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

import bisq.http_api.push_notification.DeviceRegistration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Request to register a device for push notifications.
 */
@Getter
@Schema(description = "Request to register a device for push notifications")
public class RegisterDeviceRequest {
    @Schema(description = "User profile ID", required = true, example = "d22d7b62ef442b5df03378f134bc8f54a2171cba")
    private final String userProfileId;

    @Schema(description = "APNs device token", required = true, example = "abc123def456...")
    private final String deviceToken;

    @Schema(description = "Base64 encoded public key for encrypting notifications", required = true)
    private final String publicKey;

    @Schema(description = "Platform type", required = true, example = "IOS")
    private final DeviceRegistration.Platform platform;

    @JsonCreator
    public RegisterDeviceRequest(@JsonProperty("userProfileId") String userProfileId,
                                 @JsonProperty("deviceToken") String deviceToken,
                                 @JsonProperty("publicKey") String publicKey,
                                 @JsonProperty("platform") DeviceRegistration.Platform platform) {
        this.userProfileId = userProfileId;
        this.deviceToken = deviceToken;
        this.publicKey = publicKey;
        this.platform = platform;
    }
}

