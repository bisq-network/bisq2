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

package bisq.api.rest.controller;

import bisq.api.rest.ApiApplicationService;
import bisq.security.KeyPairService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
@RestController
class ApiKeyPairController extends ApiController {
    private final KeyPairService keyPairService;

    public ApiKeyPairController(ApiApplicationService apiApplicationService) {
        keyPairService = apiApplicationService.getSecurityService().getKeyPairService();
    }

    /**
     * @param keyId The ID for identifying the key which we look up or create in case it does not exist.
     * @return The key pair.
     */
    @GetMapping(path = "/api/keypair/get-or-create/{keyId}")
    public String getOrCreateKeyPair(@PathVariable("keyId") String keyId) {
        return keyPairAsJson(keyPairService.getOrCreateKeyPair(keyId));
    }

    /**
     * @param keyId The ID for identifying the key which we look up.
     * @return The key pair if a key pair with that keyId exist, otherwise null.
     */
    @GetMapping(path = "/api/keypair/get/{keyId}")
    public String findKeyPair(@PathVariable("keyId") String keyId) {
        Optional<KeyPair> optionalKeyPair = keyPairService.findKeyPair(keyId);
        if (optionalKeyPair.isPresent()) {
            return keyPairAsJson(optionalKeyPair.get());
        } else {
            return "null";
        }
    }
}
