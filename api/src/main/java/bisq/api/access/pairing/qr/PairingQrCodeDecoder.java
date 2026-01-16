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
import bisq.api.access.pairing.PairingQrDecoder;
import bisq.common.util.BinaryDecodingUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

public final class PairingQrCodeDecoder {
    public static PairingQrCodeData decode(String qrCodeAsBase64) {
        byte[] bytes = Base64.getUrlDecoder().decode(qrCodeAsBase64);
        return decode(bytes);
    }

    public static PairingQrCodeData decode(byte[] qrCodeBytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(qrCodeBytes))) {

            // ---- Version ----
            byte version = in.readByte();
            if (version != PairingQrCodeFormat.VERSION) {
                // In case of a new version we need to provide new decoder
                throw new IllegalArgumentException("Unsupported QR code version: " + version);
            }

            // ---- PairingCode ----
            byte[] pairingCodeBytes = BinaryDecodingUtils.readBytes(in, PairingQrCodeFormat.MAX_PAIRING_CODE_BYTES);
            PairingCode pairingCode = PairingQrDecoder.decode(pairingCodeBytes);

            // ---- Address ----
            String webSocketUrl = BinaryDecodingUtils.readString(in, PairingQrCodeFormat.MAX_WS_URL_BYTES);

            // ---- Flags ----
            byte flags = in.readByte();

            Optional<String> tlsFingerprint = Optional.empty();
            Optional<String> torClientAuthSecret = Optional.empty();

            // ---- Optional fields (order must match encoder) ----
            if ((flags & PairingQrCodeFormat.FLAG_TLS_FINGERPRINT) != 0) {
                tlsFingerprint = Optional.of(BinaryDecodingUtils.readString(in, PairingQrCodeFormat.MAX_TLS_FINGERPRINT_BYTES));
            }

            if ((flags & PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH) != 0) {
                torClientAuthSecret = Optional.of(BinaryDecodingUtils.readString(in, PairingQrCodeFormat.MAX_TOR_SECRET_BYTES));
            }

            return new PairingQrCodeData(version,
                    pairingCode,
                    webSocketUrl,
                    tlsFingerprint,
                    torClientAuthSecret);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode QR code data", e);
        }
    }
}


