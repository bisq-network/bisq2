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

import bisq.bisq_easy.BisqEasyService;
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
import bisq.user.profile.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
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
    private final BisqEasyService bisqEasyService;

    public UserIdentityRestApi(SecurityService securityService,
                               UserIdentityService userIdentityService,
                               BisqEasyService bisqEasyService) {
        this.securityService = securityService;
        this.userIdentityService = userIdentityService;
        this.bisqEasyService = bisqEasyService;
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
            KeyPairDto keyPairDto = DtoMappings.KeyPairMapping.fromBisq2Model(keyPair);
            ProofOfWorkDto proofOfWorkDto = DtoMappings.ProofOfWorkMapping.fromBisq2Model(proofOfWork);
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
            KeyPair keyPair = DtoMappings.KeyPairMapping.toBisq2Model(keyPairDto);
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            int avatarVersion = 0;
            ProofOfWorkDto proofOfWorkDto = keyMaterialResponse.getProofOfWork();
            ProofOfWork proofOfWork = DtoMappings.ProofOfWorkMapping.toBisq2Model(proofOfWorkDto);
            UserIdentity userIdentity = userIdentityService.createAndPublishNewUserProfile(request.getNickName(),
                    keyPair,
                    pubKeyHash,
                    proofOfWork,
                    avatarVersion,
                    request.getTerms(),
                    request.getStatement()).get();

            UserProfile userProfile = userIdentity.getUserProfile();
            UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(userProfile);
            asyncResponse.resume(buildResponse(Response.Status.CREATED, new CreateUserIdentityResponse(userProfileDto)));
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at createUserIdentity method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PATCH
    @Operation(
            summary = "Update User Identity and Publish User Profile",
            description = "Updates user identity trade & terms and publishes the associated user profile changes.",
            requestBody = @RequestBody(
                    description = "Request payload containing user nickname, terms, statement, and prepared data.",
                    content = @Content(schema = @Schema(implementation = UpdateUserIdentityRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User identity updated successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void updateAndPublishUserProfile(UpdateUserIdentityRequest request,
                                            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            asyncResponse.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            if (selectedUserIdentity == null) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "No selected user identity found"));
            } else {
                userIdentityService.editUserProfile(selectedUserIdentity, request.getTerms(), request.getStatement())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Error editing user profile", ex);
                                asyncResponse.resume(buildErrorResponse("Failed to edit user profile: " + ex.getMessage()));
                                return;
                            }
                            UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
                            UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(userIdentity.getUserProfile());
                            asyncResponse.resume(buildResponse(Response.Status.OK, new UpdateUserIdentityResponse(userProfileDto)));
                        });
            }
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PATCH
    @Path("/profile")
    @Operation(
            summary = "Update User Identity and Publish User Profile",
            description = "Updates user identity trade & terms and publishes the associated user profile changes.",
            requestBody = @RequestBody(
                    description = "Request payload containing profile id, user nickname, terms, statement, and prepared data.",
                    content = @Content(schema = @Schema(implementation = UpdateUserIdentityV2Request.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User identity updated successfully",
                            content = @Content(schema = @Schema(implementation = UpdateUserIdentityResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void updateAndPublishUserProfileV2(UpdateUserIdentityV2Request request,
                                              @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            asyncResponse.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Optional<UserIdentity> selectedUserIdentity = userIdentityService.findUserIdentity(request.getProfileId());
            if (selectedUserIdentity.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "No user identity with id " + request.getProfileId() + " found"));
            } else {
                userIdentityService.editUserProfile(selectedUserIdentity.get(), request.getTerms(), request.getStatement())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Error editing user profile", ex);
                                asyncResponse.resume(buildErrorResponse("Failed to edit user profile: " + ex.getMessage()));
                                return;
                            }
                            Optional<UserIdentity> userIdentity = userIdentityService.findUserIdentity(request.getProfileId());
                            userIdentity.ifPresentOrElse((identity) -> {
                                UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(identity.getUserProfile());
                                asyncResponse.resume(buildResponse(Response.Status.OK, new UpdateUserIdentityResponse(userProfileDto)));
                            }, () -> {
                                asyncResponse.resume(buildErrorResponse("Failed to fetch updated profile after edit"));
                            });

                        });
            }
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
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
        return buildOkResponse(ids);
    }

    @GET
    @Path("/owned-profiles")
    @Operation(
            summary = "Get Owned User Profiles",
            description = "Retrieves a list of owned user profiles",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Owned user profiles retrieved successfully",
                            content = @Content(schema = @Schema(type = "array", implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getUserProfiles() {
        List<UserProfileDto> profiles = userIdentityService.getUserIdentities()
                .stream()
                .map(userIdentity -> DtoMappings.UserProfileMapping.fromBisq2Model(userIdentity.getUserProfile()))
                .collect(Collectors.toList());
        return buildOkResponse(profiles);
    }

    @POST
    @Path("/select")
    @Operation(
            summary = "Select User Profile",
            description = "Selects a user profile (by id) as the currently active/selected profile.",
            requestBody = @RequestBody(
                    description = "Request containing the user profile id to select",
                    content = @Content(schema = @Schema(implementation = SelectUserProfileRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User profile selected successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response selectUserProfile(SelectUserProfileRequest request) {
        if (request == null || request.getUserProfileId() == null || request.getUserProfileId().isBlank()) {
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid input: userProfileId is required");
        }
        Optional<UserIdentity> opt = userIdentityService.findUserIdentity(request.getUserProfileId());
        if (opt.isEmpty()) {
            return buildNotFoundResponse("User identity not found.");
        }
        userIdentityService.selectChatUserIdentity(opt.get());
        UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(opt.get().getUserProfile());
        return buildOkResponse(userProfileDto);
    }

    @POST
    @Path("/delete")
    @Operation(
            summary = "Delete User Profile",
            description = "Deletes a user profile (by id) and returns the active/selected profile after its delete.",
            requestBody = @RequestBody(
                    description = "Request containing the user profile id to delete",
                    content = @Content(schema = @Schema(implementation = DeleteUserProfileRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User profile deleted successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "User profile not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void deleteUserProfile(DeleteUserProfileRequest request, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            asyncResponse.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            if (request.getUserProfileId() == null || request.getUserProfileId().isBlank()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: userProfileId is required"));
            } else {
                Optional<UserIdentity> opt = userIdentityService.findUserIdentity(request.getUserProfileId());
                if (opt.isEmpty()) {
                    asyncResponse.resume(buildNotFoundResponse("User identity not found."));
                } else {
                    bisqEasyService.deleteUserIdentity(opt.get()).whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Error deleting user identity", ex);
                            asyncResponse.resume(buildErrorResponse("Could not delete user identity: " + ex.getMessage()));
                        } else {
                            asyncResponse.resume(getSelectedUserProfile());
                        }
                    });
                }
            }
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
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
        UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(selectedUserIdentity.getUserProfile());
        return buildOkResponse(userProfileDto);
    }
}
