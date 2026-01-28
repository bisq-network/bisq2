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

import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.common.util.StringUtils;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.notifications.mobile.registration.MobileDevicePlatform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for managing mobile device registrations for push notifications.
 */
@Slf4j
@Path("/mobile-devices/registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(
        name = "Mobile Device Registrations API",
        description = "API for registering and unregistering mobile devices for push notifications"
)
public class DevicesRestApi extends RestApiBase {
    private static final String APNS_HEX_REGEX = "^[0-9a-fA-F]+$";

    private final DeviceRegistrationService deviceRegistrationService;

    public DevicesRestApi(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    @POST
    @Operation(
            summary = "Register a mobile device for push notifications",
            description = "Creates or updates a mobile device registration for receiving push notifications.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterDeviceRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device registered or updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response registerDevice(RegisterDeviceRequest request) {
        if (!isValid(request)) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "deviceId, deviceToken, publicKeyBase64, deviceDescriptor and platform are required"
            );
        }

        String deviceId = request.getDeviceId();
        String deviceToken = request.getDeviceToken();
        String publicKeyBase64 = request.getPublicKeyBase64();
        String deviceDescriptor = request.getDeviceDescriptor();
        MobileDevicePlatform platform = request.getPlatform();

        log.debug(
                "Register device request: deviceId={}, descriptor={}, tokenLength={}, platform={}",
                deviceId, deviceDescriptor, deviceToken.length(), platform
        );

        // Platform-specific token validation
        if (platform == MobileDevicePlatform.IOS && !deviceToken.matches(APNS_HEX_REGEX)) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "APNs device token must be a hex-encoded string"
            );
        }

        try {
            deviceRegistrationService.register(
                    deviceId,
                    deviceToken,
                    publicKeyBase64,
                    deviceDescriptor,
                    platform
            );

            log.info("Device registered: deviceId={}, platform={}", deviceId, platform);
            return buildOkResponse("Device registered successfully");

        } catch (Exception e) {
            log.error("Exception during device registration", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to register device");
        }
    }

    @DELETE
    @Path("/{deviceId}")
    @Operation(
            summary = "Unregister a mobile device from push notifications",
            description = "Removes an existing mobile device registration.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Device unregistered"),
                    @ApiResponse(responseCode = "404", description = "Device not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response unregisterDevice(@PathParam("deviceId") String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return buildResponse(Response.Status.BAD_REQUEST, "deviceId is required");
        }

        try {
            boolean hadValue = deviceRegistrationService.unregister(deviceId);

            if (!hadValue) {
                return buildResponse(Response.Status.NOT_FOUND, "Device not found");
            }

            log.info("Device unregistered: deviceId={}", deviceId);
            return buildNoContentResponse();

        } catch (Exception e) {
            log.error("Exception during device unregistration", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to unregister device");
        }
    }

    private boolean isValid(RegisterDeviceRequest request) {
        return request != null
                && StringUtils.isNotEmpty(request.getDeviceId())
                && StringUtils.isNotEmpty(request.getDeviceToken())
                && StringUtils.isNotEmpty(request.getPublicKeyBase64())
                && StringUtils.isNotEmpty(request.getDeviceDescriptor())
                && request.getPlatform() != null;
    }
}

