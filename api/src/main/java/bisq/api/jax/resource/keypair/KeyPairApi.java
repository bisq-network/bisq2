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
@Path("/key-pair")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Key Pair API")
public class KeyPairApi {
    public static final String DESC_KEY_ID = "The ID for identifying the key which we look up or create in case it does not exist.";
    private final KeyPairService keyPairService;

    public KeyPairApi(@Context Application app) {
        keyPairService = ((RestApplication) app).getApplicationService().getSecurityService().getKeyPairService();
    }

    /**
     * @param keyId The ID for identifying the key which we look up or create in case it does not exist.
     * @return The key pair.
     */
    @Operation(description = "find the key pair for given ID")
    @ApiResponse(responseCode = "200", description = "the created or existing key pair for the given key-id",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = KeyPairDTO.class)
                    )}
    )
    @GET
    @Path("get-or-create/{key-id}")
    public KeyPairDTO getOrCreateKeyPair(
            @Parameter(description = DESC_KEY_ID) @PathParam("key-id") String keyId) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return new KeyPairDTO(keyPair);
    }

    /**
     * @param keyId The ID for identifying the key which we look up.
     * @return The key pair if a key pair with that keyId exist, otherwise null.
     */

    @Operation(summary = "find the key pair for given ID")
    @ApiResponse(responseCode = "404", description = "key-id was not found")
    @ApiResponse(responseCode = "200", description = "key pair for the key-id",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KeyPairDTO.class)
                    )}
    )
    @GET
    @Path("get/{key-id}")
    public KeyPairDTO findKeyPair(@Parameter(description = DESC_KEY_ID) @PathParam("key-id") String keyId) {
        return new KeyPairDTO(keyPairService.findKeyPair(keyId)
                .orElseThrow(() -> new StatusException(Response.Status.NOT_FOUND, "Could not find the key pair for ID " + keyId)));
    }
}
