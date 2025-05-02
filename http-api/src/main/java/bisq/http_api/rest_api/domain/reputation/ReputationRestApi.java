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

package bisq.http_api.rest_api.domain.reputation;

import bisq.dto.DtoMappings;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Path("/reputation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reputation API", description = "API for fetching User reputation")
public class ReputationRestApi extends RestApiBase {
    private final ReputationService reputationService;

    public ReputationRestApi(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @GET
    @Operation(
            summary = "Retrieve reputation score for a specific user",
            description = "Fetches the reputation score associated with the given user profile ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User reputation retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ReputationScoreDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/scores/{userProfileId}")
    public Response getReputationForUser(@PathParam("userProfileId") String userProfileId) {
        try {
            if (userProfileId == null || userProfileId.trim().isEmpty()) {
                return buildErrorResponse("User profile ID must not be empty.");
            }
            ReputationScore reputationScore = reputationService.getReputationScore(userProfileId);
            ReputationScoreDto dto = DtoMappings.ReputationScoreMapping.fromBisq2Model(reputationScore);
            return buildOkResponse(dto);
        } catch (Exception e) {
            log.error("Failed to retrieve reputation for userProfileId={}", userProfileId, e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Operation(
            summary = "Retrieve reputation scores for all users",
            description = "Returns a mapping of user profile IDs to their corresponding reputation scores.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile IDs and reputations retrieved successfully",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/scores")
    public Response getScoreByUserProfileId() {
        try {
            Map<String, Long> map = reputationService.getScoreByUserProfileId();

            return buildOkResponse(map);
        } catch (Exception e) {
            log.error("Failed to retrieve profile IDs with reputation", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

}