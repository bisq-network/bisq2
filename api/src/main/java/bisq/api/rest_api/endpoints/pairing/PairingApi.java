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

package bisq.api.rest_api.endpoints.pairing;

import bisq.api.access.AllowUnauthenticated;
import bisq.api.access.pairing.InvalidPairingRequestException;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingRequestHandler;
import bisq.api.access.session.SessionToken;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.dto.DtoMappings;
import bisq.api.dto.pairing.PairingRequestDto;
import bisq.api.dto.pairing.PairingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Path("/pairing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(
        name = "Pairing",
        description = "Endpoints for pairing a new client device and establishing an authenticated session"
)
public class PairingApi extends RestApiBase {
    private final PairingRequestHandler pairingRequestHandler;

    public PairingApi(PairingRequestHandler pairingRequestHandler) {
        this.pairingRequestHandler = pairingRequestHandler;
    }

    @POST
    @AllowUnauthenticated
    @Operation(
            summary = "Request device pairing",
            description = """
                    Performs cryptographic pairing of a client device.
                    
                    The client submits a signed pairing request containing:
                    - pairing code ID
                    - device public key
                    - device name
                    - timestamp
                    
                    On success, a short-lived session token is created and returned.
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
    @ApiResponse(responseCode = "401", description = "Pairing request failed")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response requestPairing(PairingRequestDto request) {
        try {
            PairingRequest pairingRequest = DtoMappings.PairingRequestMapper.toBisq2Model(request);
            SessionToken sessionToken = pairingRequestHandler.handle(pairingRequest);
            long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
            String sessionId = sessionToken.getSessionId();
            PairingResponseDto response = new PairingResponseDto(sessionId, expiresAt);
            return buildOkResponse(response);
        } catch (InvalidPairingRequestException e) {
            log.error("Pairing request failed", e);
            return buildErrorResponse(Response.Status.UNAUTHORIZED, "Pairing request failed");
        } catch (Exception e) {
            log.error("Pairing request failed", e);
            return buildErrorResponse("Pairing request failed");
        }
    }
}
