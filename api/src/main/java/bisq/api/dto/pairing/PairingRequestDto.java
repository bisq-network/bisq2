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

package bisq.api.dto.pairing;

import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;

public record PairingRequestDto(
        PairingRequestPayloadDto payload,
        String signatureBase64
) {

    public byte[] signatureBytes() {
        checkNotNull(signatureBase64, "signatureBase64 must not be null");
        try {
            return Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("signatureBase64 is not valid Base64", e);
        }
    }
}

