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

package bisq.api.access.http.dto;

import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingRequestPayload;
import bisq.common.encoding.Base64;
import bisq.security.keys.KeyGeneration;

import java.security.PublicKey;
import java.time.Instant;

public final class PairingRequestMapper {
    public static PairingRequest toBisq2Model(PairingRequestDto dto) {
        PairingRequestPayload payload = toBisq2Model(dto.payload());
        return new PairingRequest(
                payload,
                dto.signatureBytes()
        );
    }

    private static PairingRequestPayload toBisq2Model(PairingRequestPayloadDto dto) {
        return new PairingRequestPayload(
                dto.pairingCodeId(),
                decodePublicKey(dto.devicePublicKeyBase64()),
                dto.deviceName(),
                Instant.ofEpochMilli(dto.timestampEpochMillis())
        );
    }

    private static PublicKey decodePublicKey(String base64) {
        try {
            byte[] bytes = Base64.decode(base64);
            return KeyGeneration.generatePublic(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid device public key", e);
        }
    }
}

