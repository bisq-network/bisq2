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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;

import java.security.KeyPair;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Path("/user-identity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserIdentityServiceApi {
    private final UserIdentityService userIdentityService;

    public UserIdentityServiceApi(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    /**
     * Generates prepared data for a new user identity.
     *
     * @return PreparedData object containing key pair, public key hash, ID, Nym, and Proof of Work.
     */
    @Operation(
            summary = "Generate Prepared Data",
            description = "Generates prepared data including a key pair, public key hash, Nym, and proof of work for a new user identity.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Prepared data generated successfully",
                            content = @Content(schema = @Schema(implementation = PreparedData.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GET
    @Path("prepared-data")
    public PreparedData getPreparedData() {
        return userIdentityService.getPreparedData();
    }


    /**
     * Retrieves the user identity for the specified profile ID.
     *
     * @param userProfileId the unique ID of the user profile.
     * @return UserIdentity object if found.
     * @throws RestApiException with HTTP 404 status if the profile ID is not found.
     */
    @Operation(
            summary = "Get User Identity",
            description = "Retrieves the user identity for the specified profile ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User identity retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserIdentity.class))),
                    @ApiResponse(responseCode = "404", description = "User identity not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GET
    @Path("{user-profile-id}")
    public UserIdentity getUserIdentity(@PathParam("user-profile-id") String userProfileId) {
        return userIdentityService.findUserIdentity(userProfileId)
                .orElseThrow(() -> new RestApiException(Response.Status.NOT_FOUND,
                        "Could not find user identity for userProfileId " + userProfileId));
    }


    /**
     * Creates a new user identity and publishes the user profile.
     *
     * @param request the CreateUserIdentityRequest object containing user details and prepared data.
     * @return Map with the created user profile ID.
     * @throws RestApiException with HTTP 400 if the input is invalid.
     * @throws RestApiException with HTTP 500 if an internal error occurs.
     */
    @Operation(
            summary = "Create and Publish User Identity",
            description = "Creates a new user identity and publishes the associated user profile.",
            requestBody = @RequestBody(
                    description = "Request payload containing user nickname, terms, statement, and prepared data.",
                    content = @Content(schema = @Schema(implementation = CreateUserIdentityRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User identity created successfully",
                            content = @Content(schema = @Schema(example = "{ \"userProfileId\": \"d22d7b62ef442b5df03378f134bc8f54a2171cba\" }"))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @POST
    @Path("user-identities")
    public Map<String, String> createUserIdentityAndPublishUserProfile2(CreateUserIdentityRequest request) {
        try {
            PreparedData preparedData = new ObjectMapper().readValue(request.preparedDataJson, PreparedData.class);
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
            return Collections.singletonMap("userProfileId", userIdentity.getId());
        } catch (JsonProcessingException e) {
            throw new RestApiException(Response.Status.BAD_REQUEST,
                    "Invalid input: Unable to process JSON");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RestApiException(Response.Status.REQUEST_TIMEOUT, "The request was interrupted or timed out");
        } catch (ExecutionException e) {
            throw new RestApiException(Response.Status.INTERNAL_SERVER_ERROR, "An error occurred while processing the request");
        }
    }


    /**
     * Request DTO for creating a new user identity.
     */
    @Data
    @Schema(description = "Request payload for creating a new user identity.")
    public static class CreateUserIdentityRequest {
        @Schema(description = "Nickname for the user", example = "JohnDoe", required = true)
        private String nickName;

        @Schema(description = "User terms and conditions", example = "I guarantee to complete the trade in 24 hours")
        private String terms = "";

        @Schema(description = "User statement", example = "I am Satoshi")
        private String statement = "";

        @Schema(description = "Prepared data in JSON format", required = true,
                example = "{ \"keyPair\": {}, \"proofOfWork\": {} }")
        private String preparedDataJson;
    }
}
