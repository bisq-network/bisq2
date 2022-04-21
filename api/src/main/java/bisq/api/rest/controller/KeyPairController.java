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
import bisq.api.rest.dao.JsonKeyPair;
import bisq.common.encoding.Hex;
import bisq.security.KeyPairService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
@RestController
class KeyPairController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final KeyPairService keyPairService;

    public KeyPairController(ApiApplicationService apiApplicationService) {
        keyPairService = apiApplicationService.getSecurityService().getKeyPairService();
    }

    @GetMapping(path = "/api/keypair/get-or-create/{keyId}")
    public String getOrCreateKeyPair(@PathVariable("keyId") String keyId) throws JsonProcessingException {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return mapper.writeValueAsString(new JsonKeyPair(Hex.encode(keyPair.getPrivate().getEncoded()), Hex.encode(keyPair.getPublic().getEncoded())));
    }

    @GetMapping(path = "/api/keypair/get/{keyId}")
    public String findKeyPair(@PathVariable("keyId") String keyId) throws JsonProcessingException {
        Optional<KeyPair> optionalKeyPair = keyPairService.findKeyPair(keyId);
        if (optionalKeyPair.isPresent()) {
            KeyPair keyPair = optionalKeyPair.get();
            JsonKeyPair value = new JsonKeyPair(Hex.encode(keyPair.getPrivate().getEncoded()), Hex.encode(keyPair.getPublic().getEncoded()));
            return mapper.writeValueAsString(value);
        } else {
            return "null";
        }
    }
}
