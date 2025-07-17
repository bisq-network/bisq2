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
import bisq.user.UserService;
import bisq.user.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/network")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Network Statistics API")
public class NetworkStatsRestApi extends RestApiBase {
    private final UserProfileService userProfileService;

    public NetworkStatsRestApi(UserService userService) {
        this.userProfileService = userService.getUserProfileService();
    }

    @GET
    @Path("/stats")
    @Operation(
            summary = "Get Network Statistics",
            description = "Retrieves current network statistics including total published profiles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Network statistics retrieved successfully",
                            content = @Content(schema = @Schema(implementation = NetworkStatsResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getNetworkStats() {
        try {
            int totalPublishedProfiles = userProfileService.getUserProfiles().size();
            NetworkStatsResponse response = new NetworkStatsResponse(totalPublishedProfiles);

            return buildOkResponse(response);
        } catch (Exception e) {
            log.error("Error retrieving network statistics", e);
            return buildErrorResponse("Could not retrieve network statistics: " + e.getMessage());
        }
    }
}
