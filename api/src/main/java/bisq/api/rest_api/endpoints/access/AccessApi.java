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

package bisq.api.rest_api.endpoints.access;

import bisq.api.access.AllowUnauthenticated;
import bisq.api.access.ApiAccessService;
import bisq.api.access.pairing.InvalidPairingRequestException;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingResponse;
import bisq.api.access.session.InvalidSessionRequestException;
import bisq.api.access.session.SessionRequest;
import bisq.api.access.session.SessionResponse;
import bisq.api.dto.DtoMappings;
import bisq.api.dto.access.pairing.PairingRequestDto;
import bisq.api.dto.access.pairing.PairingResponseDto;
import bisq.api.dto.access.session.SessionRequestDto;
import bisq.api.dto.access.session.SessionResponseDto;
import bisq.api.rest_api.endpoints.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/access")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(
        name = "Access",
        description = "Endpoints for pairing a client device and establishing " +
                "authenticated sessions"
)
public class AccessApi extends RestApiBase {

    private final ApiAccessService apiAccessService;

    public AccessApi(ApiAccessService apiAccessService) {
        this.apiAccessService = apiAccessService;
    }

    @POST
    @Path("/pairing")
    @AllowUnauthenticated
    @Operation(
            summary = "Request device pairing",
            description = """
                    Performs cryptographic pairing of a client device.
                    
                    The client submits a signed pairing request containing:
                    - pairing code identifier
                    - device public key
                    - device name
                    - timestamp
                    
                    On success, a short-lived session ID is created and returned.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Pairing successful",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PairingResponseDto.class)
            )
    )
    @ApiResponse(responseCode = "401", description = "Pairing request rejected")
    @ApiResponse(responseCode = "400", description = "Invalid pairing request payload")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response requestPairing(
            @RequestBody(required = true)
            PairingRequestDto request
    ) {
        log.error("requestPairing {}",request);
        try {
            PairingRequest pairingRequest =
                    DtoMappings.PairingRequestMapper.toBisq2Model(request);

            PairingResponse pairingResponse =
                    apiAccessService.handlePairingRequest(pairingRequest);

            PairingResponseDto response =
                    DtoMappings.PairingResponseMapper.fromBisq2Model(pairingResponse);

            log.error("requestPairing was successful: {}",response);
            return buildResponse(Response.Status.CREATED, response);

        } catch (InvalidPairingRequestException e) {
            log.warn("Pairing request rejected", e);
            return buildErrorResponse(Response.Status.UNAUTHORIZED,
                    "Pairing request failed");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid pairing request payload", e);
            return buildErrorResponse(Response.Status.BAD_REQUEST,
                    "Invalid pairing request payload");

        } catch (Exception e) {
            log.error("Unexpected error during pairing request", e);
            return buildErrorResponse("Pairing request failed");
        }
    }

    @POST
    @Path("/session")
    @AllowUnauthenticated
    @Operation(
            summary = "Create a new device session",
            description = """
                    Creates a new short-lived session for a client device using a
                    client identifier and shared device secret.
                    
                    This endpoint is intentionally unauthenticated and is used during
                    initial client bootstrap or re-sessioning.
                    
                    If the credentials are valid, a new session ID is issued together
                    with its expiration timestamp. The session ID must be supplied in
                    subsequent authenticated requests.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Session successfully created",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SessionResponseDto.class)
            )
    )
    @ApiResponse(responseCode = "401", description = "Session request rejected")
    @ApiResponse(responseCode = "400", description = "Invalid session request payload")
    @ApiResponse(responseCode = "500", description = "Unexpected internal server error")
    public Response requestSession(
            SessionRequestDto request
    ) { log.error("requestSession {}",request);
        try {
            if (request == null ||
                    request.clientId() == null ||
                    request.clientSecret() == null) {
                throw new IllegalArgumentException("Missing client credentials");
            }

            SessionRequest sessionRequest =
                    new SessionRequest(request.clientId(), request.clientSecret());

            SessionResponse sessionResponse =
                    apiAccessService.handleSessionRequest(sessionRequest);

            SessionResponseDto response =
                    new SessionResponseDto(
                            sessionResponse.getSessionId(),
                            sessionResponse.getExpiresAt()
                    );
            log.error("requestSession was successful: {}",response);
            return buildResponse(Response.Status.CREATED, response);

        } catch (InvalidSessionRequestException e) {
            log.warn("Session request rejected", e);
            return buildErrorResponse(Response.Status.UNAUTHORIZED,
                    "Session request failed");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid session request payload", e);
            return buildErrorResponse(Response.Status.BAD_REQUEST,
                    "Invalid session request payload");

        } catch (Exception e) {
            log.error("Unexpected error during session request", e);
            return buildErrorResponse("Session request failed");
        }
    }
}
