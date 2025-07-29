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
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Path("/reputation")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reputation API")
public class ReputationRestApi extends RestApiBase {
    private final ReputationService reputationService;
    private final UserService userService;

    public ReputationRestApi(ReputationService reputationService, UserService userService) {
        this.reputationService = reputationService;
        this.userService = userService;
    }

    @GET
    @Path("/score/{userProfileId}")
    @Operation(
            summary = "Get Reputation Score",
            description = "Retrieves the reputation score for a specific user profile.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reputation score retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ReputationScoreDto.class))),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getReputationScore(
            @Parameter(description = "User profile ID", required = true)
            @PathParam("userProfileId") String userProfileId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfileService().findUserProfile(userProfileId);
            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("User profile not found for ID: " + userProfileId);
            }

            var reputationScore = reputationService.getReputationScore(userProfileId);
            ReputationScoreDto reputationScoreDto = DtoMappings.ReputationScoreMapping.fromBisq2Model(reputationScore);
            return buildOkResponse(reputationScoreDto);
        } catch (Exception e) {
            log.error("Error getting reputation score for userProfileId: " + userProfileId, e);
            return buildErrorResponse("Error getting reputation score: " + e.getMessage());
        }
    }

    @GET
    @Path("/profile-age/{userProfileId}")
    @Operation(
            summary = "Get Profile Age",
            description = "Retrieves the profile creation timestamp for a specific user profile from the ProfileAgeService.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile age retrieved successfully",
                            content = @Content(schema = @Schema(implementation = Long.class, 
                                    description = "Profile creation timestamp in milliseconds, or null if not available"))),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getProfileAge(
            @Parameter(description = "User profile ID", required = true)
            @PathParam("userProfileId") String userProfileId) {
        try {
            Optional<UserProfile> userProfile = userService.getUserProfileService().findUserProfile(userProfileId);
            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("User profile not found for ID: " + userProfileId);
            }

            Optional<Long> profileAge = reputationService.getProfileAgeService().getProfileAge(userProfile.get());
            if (profileAge.isPresent()) {
                return buildOkResponse(profileAge.get());
            } else {
                // Return explicit null as JSON string to avoid empty response body
                return Response.status(Response.Status.OK)
                        .entity("null")
                        .type("application/json")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting profile age for userProfileId: " + userProfileId, e);
            return buildErrorResponse("Error getting profile age: " + e.getMessage());
        }
    }
}
