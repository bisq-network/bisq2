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

package bisq.api.jax.resource.keypair;

import bisq.api.jax.RestApplication;
import bisq.api.jax.StatusException;
import bisq.security.KeyPairService;
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
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
@Path("/KeyPair")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Key Pair API")
public class KeyPairApi {
    public static final String DESC_KEYID = "The ID for identifying the key which we look up or create in case it does not exist.";
    private final KeyPairService keyPairService;

    public KeyPairApi(@Context Application app) {
        keyPairService = ((RestApplication) app).getApplicationService().getSecurityService().getKeyPairService();
    }

    /**
     * @param keyId The ID for identifying the key which we look up or create in case it does not exist.
     * @return The key pair.
     */
    @Operation(description = "find the private and public key for given ID")
    @ApiResponse(responseCode = "200", description = "the created or existing privat and public key for the given keyId",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = KeyPairDTO.class)
                    )}
    )
    @GET
    @Path("get-or-create/{keyId}")
    public KeyPairDTO getOrCreateKeyPair(
            @Parameter(description = DESC_KEYID) @PathParam("keyId") String keyId) {
        KeyPair k = keyPairService.getOrCreateKeyPair(keyId);
        return new KeyPairDTO(k);
    }

    /**
     * @param keyId The ID for identifying the key which we look up.
     * @return The key pair if a key pair with that keyId exist, otherwise null.
     */

    @Operation(summary = "find the private and public key for given ID")
    @ApiResponse(responseCode = "404", description = "keyId was not found")
    @ApiResponse(responseCode = "200", description = "privat and public key for the keyId",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KeyPairDTO.class)
                    )}
    )
    @GET
    @Path("get/{keyId}")
    public KeyPairDTO findKeyPair(@Parameter(description = DESC_KEYID) @PathParam("keyId") String keyId) {
        return new KeyPairDTO(keyPairService.findKeyPair(keyId)
                .orElseThrow(() -> new StatusException(Response.Status.NOT_FOUND, "Could not find the key " + keyId)));
    }
}
