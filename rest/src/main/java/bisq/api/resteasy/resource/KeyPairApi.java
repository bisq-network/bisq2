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

package bisq.api.resteasy.resource;

import bisq.api.resteasy.RestApplication;
import bisq.security.KeyPairService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
@Path("/api/v1/KeyPair")
@Produces(MediaType.APPLICATION_JSON)
class KeyPairApi {
    private final KeyPairService keyPairService;

    public KeyPairApi(@Context RestApplication app) {
        keyPairService = app.getApplicationService().getSecurityService().getKeyPairService();
    }

    /**
     * @param keyId The ID for identifying the key which we look up or create in case it does not exist.
     * @return The key pair.
     */
    @GET
    @Path("get-or-create/{keyId}")
    public KeyPair getOrCreateKeyPair(@PathParam("keyId") String keyId) {
        return keyPairService.getOrCreateKeyPair(keyId);
    }

    /**
     * @param keyId The ID for identifying the key which we look up.
     * @return The key pair if a key pair with that keyId exist, otherwise null.
     */
    @GET
    @Path("get/{keyId}")
    public KeyPair findKeyPair(@PathParam("keyId") String keyId) {
        return keyPairService.findKeyPair(keyId).orElse(null);
    }
}
