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
import bisq.http_api.push_notification.MobileDevicePlatform;
import bisq.http_api.rest_api.domain.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST API for managing mobile device registrations for push notifications.
 */
@Slf4j
@Path("/mobile-devices/registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Mobile Devices API", description = "API for managing mobile device registrations for push notifications")
public class DevicesRestApi extends RestApiBase {
    private final DeviceRegistrationService deviceRegistrationService;

    public DevicesRestApi(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    @POST
    @Operation(
            summary = "Register a mobile device for push notifications",
            description = "Registers a mobile device to receive push notifications for trade events. " +
                    "The device token is obtained from APNs (iOS) or FCM (Android), and the public key is used to encrypt notification payloads.",
            requestBody = @RequestBody(
                    description = "Device registration details",
                    content = @Content(schema = @Schema(implementation = RegisterDeviceRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Device registered successfully"),
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

            // Validate deviceId
            if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
                log.warn("Registration failed: deviceId is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: deviceId is required");
            }

            // Validate deviceToken
            if (request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
                log.warn("Registration failed: deviceToken is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: deviceToken is required");
            }

            // Validate publicKeyBase64
            if (request.getPublicKeyBase64() == null || request.getPublicKeyBase64().isBlank()) {
                log.warn("Registration failed: publicKeyBase64 is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: publicKeyBase64 is required");
            }

            // Validate deviceDescriptor
            if (request.getDeviceDescriptor() == null || request.getDeviceDescriptor().isBlank()) {
                log.warn("Registration failed: deviceDescriptor is null or blank");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: deviceDescriptor is required");
            }

            // Validate platform
            if (request.getPlatform() == null) {
                log.warn("Registration failed: platform is null");
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: platform is required");
            }

            // Platform-specific token validation
            String deviceToken = request.getDeviceToken();
            MobileDevicePlatform platform = request.getPlatform();

            if (platform == MobileDevicePlatform.IOS) {
                // APNs tokens are 64-character hex strings
                if (!deviceToken.matches("^[a-fA-F0-9]{64}$")) {
                    log.warn("Registration failed: iOS deviceToken is not a valid 64-char hex string (length: {})",
                            deviceToken.length());
                    return buildResponse(Response.Status.BAD_REQUEST,
                            "Invalid request: iOS deviceToken must be a 64-character hexadecimal string");
                }
            } else if (platform == MobileDevicePlatform.ANDROID) {
                // FCM tokens are typically 100-300 characters
                if (deviceToken.length() < 100 || deviceToken.length() > 300) {
                    log.warn("Registration failed: Android deviceToken has invalid length: {} (expected 100-300)",
                            deviceToken.length());
                    return buildResponse(Response.Status.BAD_REQUEST,
                            "Invalid request: Android deviceToken must be between 100 and 300 characters");
                }
            }

            // Log request details (only non-sensitive metadata)
            log.info("Registration details - deviceIdLength: {}, deviceTokenLength: {}, platform: {}, descriptor: {}",
                    request.getDeviceId().length(), deviceToken.length(), platform, request.getDeviceDescriptor());

            deviceRegistrationService.register(
                    request.getDeviceId(),
                    request.getDeviceToken(),
                    request.getPublicKeyBase64(),
                    request.getDeviceDescriptor(),
                    request.getPlatform()
            );

            log.info("✓ Device registered successfully (platform: {}, descriptor: {})",
                    request.getPlatform(), request.getDeviceDescriptor());
            return buildNoContentResponse();
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("✗ Exception during device registration", e);
            return buildErrorResponse("Failed to register device");
        }
    }

    @DELETE
    @Path("/{deviceId}")
    @Operation(
            summary = "Unregister a mobile device from push notifications",
            description = "Removes a device registration so it will no longer receive push notifications.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Device unregistered successfully"),
                    @ApiResponse(responseCode = "404", description = "Device not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response unregisterDevice(
            @Parameter(description = "The device ID to unregister", required = true)
            @PathParam("deviceId") String deviceId) {
        try {
            if (deviceId == null || deviceId.isBlank()) {
                return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: deviceId is required");
            }

            boolean success = deviceRegistrationService.unregister(deviceId);

            if (success) {
                log.info("✓ Device unregistered (deviceIdLength: {})", deviceId.length());
                return buildNoContentResponse();
            } else {
                return buildResponse(Response.Status.NOT_FOUND, "Device not found");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unregister failed: {}", e.getMessage());
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error unregistering device", e);
            return buildErrorResponse("Failed to unregister device");
        }
    }
}

