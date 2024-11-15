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

package bisq.rest_api.endpoints;

import bisq.common.encoding.Hex;
import bisq.rest_api.JaxRsApplication;
import bisq.rest_api.RestApiApplicationService;
import bisq.rest_api.dto.UserProfileDto;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.identity.NymIdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
@Path("/user-profile")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "User profile API")
public class UserProfileApi {
    private final KeyBundleService keyBundleService;
    private final UserService userService;

    public UserProfileApi(@Context Application application) {
        RestApiApplicationService applicationService = ((JaxRsApplication) application).getApplicationService().get();
        keyBundleService = applicationService.getSecurityService().getKeyBundleService();
        userService = applicationService.getUserService();
    }

    @Operation(summary = "create user profile")
    @ApiResponse(responseCode = "404", description = "could not create user profile")
    @ApiResponse(responseCode = "200", description = "created user profile",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileDto.class)
                    )}
    )
    @GET
    @Path("create")
    public UserProfileDto create() {
        KeyPair keyPair = keyBundleService.generateKeyPair();
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        String id = Hex.encode(pubKeyHash);
        ProofOfWork proofOfWork = userService.getUserIdentityService().mintNymProofOfWork(pubKeyHash);
        byte[] powSolution = proofOfWork.getSolution();
        String nym = NymIdGenerator.generate(pubKeyHash, powSolution);
        return UserProfileDto.from(keyPair, id, nym, powSolution);
    }
}
