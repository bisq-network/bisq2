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

package bisq.api.access.pairing.qr;

import bisq.api.access.pairing.PairingCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

@Getter
@EqualsAndHashCode
public final class PairingQrCodeData {
    private final byte version;
    private final PairingCode pairingCode;
    private final String webSocketUrl;
    private final Optional<String> tlsFingerprint;
    private final Optional<String> torClientAuthSecret;

    public PairingQrCodeData(byte version,
                             PairingCode pairingCode,
                             String webSocketUrl,
                             Optional<String> tlsFingerprint,
                             Optional<String> torClientAuthSecret) {
        this.version = version;
        this.pairingCode = pairingCode;
        this.webSocketUrl = webSocketUrl;
        this.tlsFingerprint = tlsFingerprint;
        this.torClientAuthSecret = torClientAuthSecret;
    }
}
