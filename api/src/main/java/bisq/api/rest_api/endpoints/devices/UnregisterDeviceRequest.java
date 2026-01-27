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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Request to unregister a device from push notifications.
 *
 * <p>
 * Removes an existing device registration so the device will no longer
 * receive push notifications.
 * </p>
 */
@Getter
@Schema(description = "Request payload for unregistering a device from push notifications")
public class UnregisterDeviceRequest {

    @Schema(
            description = "User profile identifier",
            required = true,
            example = "d22d7b62ef442b5df03378f134bc8f54a2171cba"
    )
    private final String userProfileId;

    @Schema(
            description = "Platform-specific device token to unregister",
            required = true,
            example = "4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e1d8b7a6c5e4f3c2a9e"
    )
    private final String deviceToken;

    @JsonCreator
    public UnregisterDeviceRequest(
            @JsonProperty("userProfileId") String userProfileId,
            @JsonProperty("deviceToken") String deviceToken
    ) {
        this.userProfileId = userProfileId;
        this.deviceToken = deviceToken;
    }
}
