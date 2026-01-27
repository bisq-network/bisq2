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
import bisq.notifications.mobile_push.registration.DeviceRegistration;
import bisq.notifications.mobile_push.registration.DeviceRegistrationService;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for managing device registrations for push notifications.
 */
@Slf4j
@Path("/devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Devices API", description = "API for managing device registrations for push notifications")
public class DevicesRestApi extends RestApiBase {

    private static final int APNS_TOKEN_LENGTH = 64;
    private static final String ALPHANUMERIC_REGEX = "^[a-zA-Z0-9]+$";

    private final DeviceRegistrationService deviceRegistrationService;

    public DevicesRestApi(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    @POST
    @Path("/register")
    @Operation(
            summary = "Register a device for push notifications",
            description = "Registers a device to receive push notifications for trade events.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterDeviceRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response registerDevice(RegisterDeviceRequest request) {
        if (isRegisterDeviceRequestInvalid(request)) {
            return buildResponse(Response.Status.BAD_REQUEST, "userProfileId, deviceToken, publicKey and platform are required");
        }

        String userProfileId = request.getUserProfileId();
        String deviceToken = request.getDeviceToken();
        String publicKey = request.getPublicKey();
        DeviceRegistration.Platform platform = request.getPlatform();

        log.debug("Register device request: userProfileId={}, tokenLength={}, platform={}",
                userProfileId, deviceToken.length(), platform);

        if (!deviceToken.matches(ALPHANUMERIC_REGEX)) {
            return buildResponse(Response.Status.BAD_REQUEST, "deviceToken must be alphanumeric");
        }

        if (platform == DeviceRegistration.Platform.IOS && deviceToken.length() != APNS_TOKEN_LENGTH) {
            log.warn("Unexpected APNs token length: {}", deviceToken.length());
        }

        try {
            boolean registered = deviceRegistrationService.registerDevice(
                    userProfileId,
                    deviceToken,
                    publicKey,
                    platform
            );

            if (!registered) {
                log.error("Device registration failed for user {}", userProfileId);
                return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to register device");
            }

            log.info("Device registered: userProfileId={}, platform={}", userProfileId, platform);
            return buildOkResponse("Device registered successfully");

        } catch (Exception e) {
            log.error("Exception during device registration", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to register device");
        }
    }

    @DELETE
    @Path("/unregister")
    @Operation(
            summary = "Unregister a device from push notifications",
            description = "Removes a device registration so it will no longer receive push notifications.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UnregisterDeviceRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device unregistered successfully"),
                    @ApiResponse(responseCode = "404", description = "Device not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response unregisterDevice(UnregisterDeviceRequest request) {
        if (isUnregisterDeviceRequestInvalid(request)) {
            return buildResponse(Response.Status.BAD_REQUEST, "userProfileId and deviceToken are required");
        }

        try {
            boolean removed = deviceRegistrationService.unregisterDevice(
                    request.getUserProfileId(),
                    request.getDeviceToken()
            );

            if (!removed) {
                return buildResponse(Response.Status.NOT_FOUND, "Device not found");
            }

            log.info("Device unregistered: userProfileId={}", request.getUserProfileId());
            return buildOkResponse("Device unregistered successfully");

        } catch (Exception e) {
            log.error("Exception during device unregistration", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to unregister device");
        }
    }

    private boolean isRegisterDeviceRequestInvalid(RegisterDeviceRequest request) {
        return request == null ||
                StringUtils.isEmpty(request.getUserProfileId()) ||
                StringUtils.isEmpty(request.getDeviceToken()) ||
                StringUtils.isEmpty(request.getPublicKey()) ||
                request.getPlatform() == null;
    }

    private boolean isUnregisterDeviceRequestInvalid(UnregisterDeviceRequest request) {
        return request == null ||
                StringUtils.isEmpty(request.getUserProfileId()) ||
                StringUtils.isEmpty(request.getDeviceToken());
    }
}

