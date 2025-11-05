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

package bisq.http_api.rest_api.domain.user_profile;

import bisq.dto.DtoMappings;
import bisq.dto.user.profile.UserProfileDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.support.moderator.ModerationRequestService;
import bisq.user.RepublishUserProfileService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Path("/user-profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Profile API", description = "API for managing user profiles")
public class UserProfileRestApi extends RestApiBase {
    private final UserProfileService userProfileService;
    private final ModerationRequestService moderationRequestService;
    private final RepublishUserProfileService republishUserProfileService;

    public UserProfileRestApi(UserProfileService userProfileService,
                              ModerationRequestService moderationRequestService,
                              RepublishUserProfileService republishUserProfileService) {
        this.userProfileService = userProfileService;
        this.moderationRequestService = moderationRequestService;
        this.republishUserProfileService = republishUserProfileService;
    }

    @POST
    @Path("/ignore/{profileId}")
    @Operation(
            summary = "Ignore User Profile",
            description = "Add a user profile to the ignored list",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User profile ignored successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response ignoreUserProfile(@PathParam("profileId") String profileId) {
        try {
            Optional<UserProfile> userProfile = userProfileService.findUserProfile(profileId);
            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("User profile not found with ID: " + profileId);
            }
            userProfileService.ignoreUserProfile(userProfile.get());
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error ignoring user profile", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @DELETE
    @Path("/ignore/{profileId}")
    @Operation(
            summary = "Undo Ignore User Profile",
            description = "Remove a user profile from the ignored list",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User profile un-ignored successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response undoIgnoreUserProfile(@PathParam("profileId") String profileId) {
        try {
            Optional<UserProfile> userProfile = userProfileService.findUserProfile(profileId);

            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("User profile not found with ID: " + profileId);
            }

            userProfileService.undoIgnoreUserProfile(userProfile.get());
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error un-ignoring user profile", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Path("/ignored")
    @Operation(
            summary = "Get Ignored User Profile IDs",
            description = "Retrieve a list of all ignored user profile IDs",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ignored user profile IDs retrieved successfully",
                            content = @Content(schema = @Schema(type = "array", implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getIgnoredUserProfileIds() {
        try {
            List<String> ignoredIds = new ArrayList<>(userProfileService.getIgnoredUserProfileIds());
            return buildOkResponse(ignoredIds);
        } catch (Exception e) {
            log.error("Error retrieving ignored user profile IDs", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Path("")
    @Operation(
            summary = "Get Multiple User Profiles by IDs",
            description = "Fetches a list of user profiles given a comma-separated list of profile IDs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User profiles retrieved successfully",
                            content = @Content(schema = @Schema(type = "array", implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getUserProfilesByIds(@QueryParam("ids") String profileIds) {
        try {
            if (profileIds == null || profileIds.isBlank()) {
                return buildResponse(Response.Status.BAD_REQUEST, "Profile IDs must be provided.");
            }

            List<String> ids = List.of(profileIds.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            List<UserProfileDto> profiles = ids.stream()
                    .map(userProfileService::findUserProfile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(DtoMappings.UserProfileMapping::fromBisq2Model)
                    .toList();

            return buildOkResponse(profiles);
        } catch (Exception e) {
            log.error("Error retrieving user profiles by IDs", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Path("/report/{profileId}")
    @Operation(
            summary = "Report User Profile",
            description = "Report a user profile to moderators for review",
            requestBody = @RequestBody(
                    description = "Report details including the reason message",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReportUserProfileRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "User profile reported successfully"),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response reportUserProfile(
            @PathParam("profileId") String profileId, @Valid ReportUserProfileRequest request
    ) {
        try {
            if (request == null || request.message() == null || request.message().isBlank()) {
                return buildResponse(Response.Status.BAD_REQUEST, "Message must be provided and not empty");
            }

            Optional<UserProfile> userProfile = userProfileService.findUserProfile(profileId);
            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("User profile not found with ID: " + profileId);
            }

            moderationRequestService.reportUserProfile(userProfile.get(), request.message());
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error reporting user profile", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Path("/activity")
    @Operation(
            summary = "Trigger User Activity Detection",
            description = "Triggers user activity detection which will republish or refresh the user profile if the minimum pause time has elapsed",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User activity detection triggered successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response triggerUserActivityDetection() {
        try {
            republishUserProfileService.userActivityDetected();
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error triggering user activity detection", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}