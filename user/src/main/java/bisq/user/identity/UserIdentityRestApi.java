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

package bisq.user.identity;

import bisq.common.rest_api.error.RestApiException;
import bisq.security.DigestUtil;
import bisq.user.profile.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Path("/user-identities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Identity API")
public class UserIdentityRestApi {
    private static final Set<String> MAIN_CURRENCIES = Set.of("usd", "eur", "gbp", "cad", "aud", "rub", "cny", "inr", "ngn");

    private final UserIdentityService userIdentityService;

    public UserIdentityRestApi(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    @GET
    @Path("/prepared-data")
    @Operation(
            summary = "Generate Prepared Data",
            description = "Generates a key pair, public key hash, Nym, and proof of work for a new user identity.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Prepared data generated successfully",
                            content = @Content(schema = @Schema(implementation = PreparedData.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response createPreparedData() {
        try {
            PreparedData preparedData = userIdentityService.createPreparedData();
            return buildResponse(Response.Status.CREATED, preparedData);
        } catch (Exception e) {
            log.error("Error generating prepared data", e);
            throw new RestApiException(Response.Status.INTERNAL_SERVER_ERROR, "Could not generate prepared data.");
        }
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get User Identity",
            description = "Retrieves the user identity for the specified ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User identity retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserIdentity.class))),
                    @ApiResponse(responseCode = "404", description = "User identity not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getUserIdentity(@PathParam("id") String id) {
        Optional<UserIdentity> userIdentity = userIdentityService.findUserIdentity(id);
        if (userIdentity.isEmpty()) {
            throw new RestApiException(Response.Status.NOT_FOUND,
                    "Could not find user identity for ID: " + id);
        }
        return buildResponse(Response.Status.OK, userIdentity.get());
    }

    @GET
    @Path("/ids")
    @Operation(
            summary = "Get All User Identity IDs",
            description = "Retrieves a list of all user identity IDs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User identity IDs retrieved successfully",
                            content = @Content(schema = @Schema(type = "array", implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getUserIdentityIds() {
        List<String> ids = userIdentityService.getUserIdentities()
                .stream()
                .map(UserIdentity::getId)
                .collect(Collectors.toList());
        return buildResponse(Response.Status.OK, ids);
    }

    @GET
    @Path("/selected")
    @Operation(
            summary = "Get Selected User Profile",
            description = "Retrieves the selected user profile.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Selected user profile retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserProfile.class))),
                    @ApiResponse(responseCode = "404", description = "No selected user identity found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getSelectedUserProfile() {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            throw new RestApiException(Response.Status.NOT_FOUND, "No selected user identity found.");
        }
        UserProfile userProfile = selectedUserIdentity.getUserProfile();
        return buildResponse(Response.Status.OK, userProfile);
    }

    @POST
    @Operation(
            summary = "Create and Publish User Identity",
            description = "Creates a new user identity and publishes the associated user profile.",
            requestBody = @RequestBody(
                    description = "Request payload containing user nickname, terms, statement, and prepared data.",
                    content = @Content(schema = @Schema(implementation = CreateUserIdentityRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User identity created successfully",
                            content = @Content(schema = @Schema(example = "{ \"userProfileId\": \"d22d7b62ef442b5df03378f134bc8f54a2171cba\" }"))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response createUserIdentityAndPublishUserProfile(CreateUserIdentityRequest request) {
        try {
            PreparedData preparedData = request.preparedData;
            KeyPair keyPair = preparedData.getKeyPair();
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            int avatarVersion = 0;
            UserIdentity userIdentity = userIdentityService.createAndPublishNewUserProfile(request.nickName,
                    keyPair,
                    pubKeyHash,
                    preparedData.getProofOfWork(),
                    avatarVersion,
                    request.terms,
                    request.statement).get();
            return buildResponse(Response.Status.CREATED, new UserProfileResponse(userIdentity.getId()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestApiException(Response.Status.INTERNAL_SERVER_ERROR, "Thread was interrupted.");
        } catch (IllegalArgumentException e) {
            throw new RestApiException(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating user identity", e);
            throw new RestApiException(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    private Response buildResponse(Response.Status status, Object entity) {
        return Response.status(status).entity(entity).build();
    }

    @Data
    @Schema(description = "Request payload for creating a new user identity.")
    public static class CreateUserIdentityRequest {
        @Schema(description = "Nickname for the user", example = "Satoshi", required = true)
        private String nickName;

        @Schema(description = "User terms and conditions", example = "I guarantee to complete the trade in 24 hours")
        private String terms = "";

        @Schema(description = "User statement", example = "I am Satoshi")
        private String statement = "";

        @Schema(description = "Prepared data as JSON object", required = true)
        private PreparedData preparedData;
    }

    @Getter
    @Schema(name = "UserProfileResponse", description = "Response payload containing the user profile ID.")
    public static class UserProfileResponse {
        private final String userProfileId;

        public UserProfileResponse(String userProfileId) {
            this.userProfileId = userProfileId;
        }
    }
}
