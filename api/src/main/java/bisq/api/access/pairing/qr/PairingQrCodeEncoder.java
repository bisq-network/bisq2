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

import bisq.api.access.pairing.PairingQrEncoder;
import bisq.common.util.BinaryEncodingUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PairingQrCodeEncoder {
    public static byte[] encode(PairingQrCodeData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            // ---- Version ----
            byte version = data.getVersion();
            if (version != PairingQrCodeFormat.VERSION) {
                // In case of a new version we need to provide a new encoder
                throw new IllegalArgumentException("Unsupported QR code version: " + version);
            }

            out.writeByte(version);

            // ---- PairingCode ----

            BinaryEncodingUtils.writeBytes(out, PairingQrEncoder.encode(data.getPairingCode()), PairingQrCodeFormat.MAX_PAIRING_CODE_BYTES);

            // ---- WS URL ----
            BinaryEncodingUtils.writeBytes(out, data.getWebSocketUrl().getBytes(StandardCharsets.UTF_8), PairingQrCodeFormat.MAX_WS_URL_BYTES);

            // ---- Flags ----
            byte flags = 0;
            if (data.getTlsFingerprint().isPresent()) {
                flags |= PairingQrCodeFormat.FLAG_TLS_FINGERPRINT;
            }
            if (data.getTorClientAuthSecret().isPresent()) {
                flags |= PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH;
            }
            out.writeByte(flags);

            // ---- Optional fields (order matters) ----
            if (data.getTlsFingerprint().isPresent()) {
                BinaryEncodingUtils.writeBytes(out, data.getTlsFingerprint().get().getBytes(StandardCharsets.UTF_8), PairingQrCodeFormat.MAX_TLS_FINGERPRINT_BYTES);
            }

            if (data.getTorClientAuthSecret().isPresent()) {
                BinaryEncodingUtils.writeBytes(out, data.getTorClientAuthSecret().get().getBytes(StandardCharsets.UTF_8), PairingQrCodeFormat.MAX_TOR_SECRET_BYTES);
            }

            out.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to encode QR code data", e);
        }
    }
}


