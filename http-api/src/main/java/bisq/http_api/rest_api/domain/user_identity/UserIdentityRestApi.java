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

package bisq.http_api.rest_api.domain.user_identity;

import bisq.common.encoding.Hex;
import bisq.dto.DtoMappings;
import bisq.dto.security.keys.KeyPairDto;
import bisq.dto.security.pow.ProofOfWorkDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.security.DigestUtil;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/user-identities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Identity API")
public class UserIdentityRestApi extends RestApiBase {
    private static final Set<String> MAIN_CURRENCIES = Set.of("usd", "eur", "gbp", "cad", "aud", "rub", "cny", "inr", "ngn");

    private final SecurityService securityService;
    private final UserIdentityService userIdentityService;

    public UserIdentityRestApi(SecurityService securityService, UserIdentityService userIdentityService) {
        this.securityService = securityService;
        this.userIdentityService = userIdentityService;
    }

    @GET
    @Path("/key-material")
    @Operation(
            summary = "Generate Prepared Data",
            description = "Generates a key pair, public key hash, Nym, and proof of work for a new user identity.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Prepared data generated successfully",
                            content = @Content(schema = @Schema(implementation = KeyMaterialResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void getKeyMaterial(@Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(5, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            KeyPair keyPair = securityService.getKeyBundleService().generateKeyPair();
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            String id = Hex.encode(pubKeyHash);
            ProofOfWork proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash);
            String nym = NymIdGenerator.generate(pubKeyHash, proofOfWork.getSolution());
            KeyPairDto keyPairDto = DtoMappings.KeyPairDtoMapping.from(keyPair);
            ProofOfWorkDto proofOfWorkDto = DtoMappings.ProofOfWorkDtoMapping.from(proofOfWork);
            KeyMaterialResponse keyMaterialResponse = KeyMaterialResponse.from(keyPairDto, id, nym, proofOfWorkDto);
            asyncResponse.resume(buildResponse(Response.Status.CREATED, keyMaterialResponse));
        } catch (Exception e) {
            log.error("Error generating prepared data", e);
            asyncResponse.resume(buildErrorResponse("Could not generate prepared data."));
        }
    }


    @POST
    @Operation(
            summary = "Create User Identity and Publish User Profile",
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
    public void createUserIdentity(CreateUserIdentityRequest request,
                                   @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });

        try {
            KeyMaterialResponse keyMaterialResponse = request.getKeyMaterialResponse();
            KeyPairDto keyPairDto = keyMaterialResponse.getKeyPair();
            KeyPair keyPair = DtoMappings.KeyPairDtoMapping.toPojo(keyPairDto);
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            int avatarVersion = 0;
            ProofOfWorkDto proofOfWorkDto = keyMaterialResponse.getProofOfWork();
            ProofOfWork proofOfWork = DtoMappings.ProofOfWorkDtoMapping.toPojo(proofOfWorkDto);
            UserIdentity userIdentity = userIdentityService.createAndPublishNewUserProfile(request.getNickName(),
                    keyPair,
                    pubKeyHash,
                    proofOfWork,
                    avatarVersion,
                    request.getTerms(),
                    request.getStatement()).get();

            asyncResponse.resume(buildResponse(Response.Status.CREATED, new CreateUserIdentityResponse(userIdentity.getId())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred."));
        }
    }

// Create UserIdentityDto
   /* @GET
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
            return buildNotFoundResponse("Could not find user identity for ID: " + id);
        }
        return buildOkResponse(userIdentity.get());
    }*/

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
        return buildOkResponse(ids);
    }

    @GET
    @Path("/selected/user-profile")
    @Operation(
            summary = "Get Selected User Profile",
            description = "Retrieves the selected user profile.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Selected user profile retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "404", description = "No selected user identity found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getSelectedUserProfile() {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            return buildNotFoundResponse("No selected user identity found.");
        }
        UserProfileDto userProfileDto = DtoMappings.UserProfileDtoMapping.from(selectedUserIdentity.getUserProfile());
        return buildOkResponse(userProfileDto);
    }
}
