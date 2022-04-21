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
import bisq.security.KeyPairProtoUtil;
import bisq.security.KeyPairService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
@RestController
class KeyPairController {
    private final KeyPairService keyPairService;

    public KeyPairController(ApiApplicationService apiApplicationService) {
        keyPairService = apiApplicationService.getSecurityService().getKeyPairService();
    }

    @GetMapping(path = "/api/keypair/get-or-create/{keyId}")
    public String getOrCreateKeyPair(@PathVariable("keyId") String keyId) throws InvalidProtocolBufferException {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return JsonFormat.printer().print(KeyPairProtoUtil.toProto(keyPair));
    }

    @GetMapping(path = "/api/keypair/get/{keyId}")
    public String findKeyPair(@PathVariable("keyId") String keyId) throws InvalidProtocolBufferException {
        Optional<KeyPair> optionalKeyPair = keyPairService.findKeyPair(keyId);
        if (optionalKeyPair.isPresent()) {
            KeyPair keyPair = optionalKeyPair.get();
            return JsonFormat.printer().print(KeyPairProtoUtil.toProto(keyPair));
        } else {
            return "null";
        }
    }
}
