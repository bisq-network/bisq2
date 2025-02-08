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
package bisq.http_api.rest_api.domain.network;

import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.http_api.rest_api.domain.user_identity.PongResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/network")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Network API", description = "API for network related operations")
public class NetworkRestApi extends RestApiBase {
    @GET
    @Path("/ping")
    @Operation(
            summary = "Respond to ping requests",
            description = "Responds a ping request with its correponding pong message",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pong response OK",
                            content = @Content(schema = @Schema(implementation = PongResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response pingPong() {
        try {
//            uncomment to test slow response
//            Thread.sleep(3000);
            return buildOkResponse(new PongResponse());
        } catch (Exception e) {
            log.error("Error responding to pong", e);
            return buildErrorResponse("Failed to respond ACK");
        }
    }
}