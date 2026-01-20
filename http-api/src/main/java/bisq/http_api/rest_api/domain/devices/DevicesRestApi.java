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

import bisq.http_api.push_notification.DeviceRegistrationService;
import bisq.http_api.rest_api.domain.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
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
    private final DeviceRegistrationService deviceRegistrationService;

    public DevicesRestApi(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    @POST
    @Path("/register")
    @Operation(
            summary = "Register a device for push notifications",
            description = "Registers a device to receive push notifications for trade events. " +
                    "The device token is obtained from APNs, and the public key is used to encrypt notification payloads.",
            requestBody = @RequestBody(
                    description = "Device registration details",
                    content = @Content(schema = @Schema(implementation = RegisterDeviceRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response registerDevice(RegisterDeviceRequest request) {
        try {
            log.info("Received device registration request");

            if (request == null) {
                log.warn("Registration failed: request is null");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: request body is required");
            }

            // Log request details (truncate sensitive data)
            String tokenPreview = request.getDeviceToken() != null && request.getDeviceToken().length() > 10
                    ? request.getDeviceToken().substring(0, 10) + "..."
                    : request.getDeviceToken();
            String publicKeyPreview = request.getPublicKey() != null && request.getPublicKey().length() > 20
                    ? request.getPublicKey().substring(0, 20) + "..."
                    : request.getPublicKey();

            log.info("Registration details - userProfileId: {}, deviceToken: {}, publicKey: {}, platform: {}",
                    request.getUserProfileId(), tokenPreview, publicKeyPreview, request.getPlatform());

            if (request.getUserProfileId() == null || request.getUserProfileId().isBlank()) {
                log.warn("Registration failed: userProfileId is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: userProfileId is required");
            }

            if (request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
                log.warn("Registration failed: deviceToken is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: deviceToken is required");
            }

            if (request.getPublicKey() == null || request.getPublicKey().isBlank()) {
                log.warn("Registration failed: publicKey is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: publicKey is required");
            }

            if (request.getPlatform() == null) {
                log.warn("Registration failed: platform is null");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: platform is required");
            }

            log.info("Registering device for user {} with platform {}", request.getUserProfileId(), request.getPlatform());
            boolean success = deviceRegistrationService.registerDevice(
                    request.getUserProfileId(),
                    request.getDeviceToken(),
                    request.getPublicKey(),
                    request.getPlatform()
            );

            if (success) {
                log.info("✓ Device registered successfully for user {} (token: {})",
                        request.getUserProfileId(), tokenPreview);
                return buildOkResponse("Device registered successfully");
            } else {
                log.error("✗ Failed to register device for user {} (token: {})",
                        request.getUserProfileId(), tokenPreview);
                return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to register device");
            }
        } catch (Exception e) {
            log.error("✗ Exception during device registration", e);
            return buildErrorResponse("Failed to register device: " + e.getMessage());
        }
    }

    @DELETE
    @Path("/unregister")
    @Operation(
            summary = "Unregister a device from push notifications",
            description = "Removes a device registration so it will no longer receive push notifications.",
            requestBody = @RequestBody(
                    description = "Device unregistration details",
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
        try {
            if (request == null || request.getUserProfileId() == null || request.getUserProfileId().isBlank() ||
                    request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: userProfileId and deviceToken are required");
            }

            boolean success = deviceRegistrationService.unregisterDevice(
                    request.getUserProfileId(),
                    request.getDeviceToken()
            );

            if (success) {
                log.info("Device unregistered for user {}", request.getUserProfileId());
                return buildOkResponse("Device unregistered successfully");
            } else {
                return buildResponse(Response.Status.NOT_FOUND, "Device not found");
            }
        } catch (Exception e) {
            log.error("Error unregistering device", e);
            return buildErrorResponse("Failed to unregister device: " + e.getMessage());
        }
    }
}

