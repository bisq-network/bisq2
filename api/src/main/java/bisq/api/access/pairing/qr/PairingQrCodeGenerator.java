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
import bisq.api.access.transport.TlsContext;
import bisq.api.access.transport.TorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Optional;

@Slf4j
public class PairingQrCodeGenerator {

    public static String generateQrCode(PairingCode pairingCode,
                                 String webSocketUrl,
                                 Optional<TlsContext> tlsContext,
                                 Optional<TorContext> torContext) {
        PairingQrCodeData pairingQrCodeData = new PairingQrCodeData(PairingQrCodeFormat.VERSION,
                pairingCode,
                webSocketUrl,
                tlsContext.map(TlsContext::getTlsFingerprint),
                torContext.map(TorContext::getClientAuthSecret));
        byte[] encoded = PairingQrCodeEncoder.encode(pairingQrCodeData);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(encoded);
    }
}
