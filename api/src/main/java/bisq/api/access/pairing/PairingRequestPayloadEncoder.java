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

package bisq.api.access.pairing;

import bisq.common.util.BinaryEncodingUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class PairingRequestPayloadEncoder {
    private static final int MAX_PAIRING_CODE_ID_LENGTH = 36;
    private static final int MAX_PUBLIC_KEY_BYTES = 128;  // EC pub key has about 90 bytes
    private static final int MAX_DEVICE_NAME_LENGTH = 128; // 32 chars - 128 chars depending on char

    public static byte[] encode(PairingRequestPayload pairingRequestPayload) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(PairingRequestPayload.VERSION);


            checkArgument(pairingRequestPayload.getPairingCodeId().length() == 36, "PairingCodeId is expected to be 36 chars long");
            checkArgument(pairingRequestPayload.getDeviceName().length() >= 4, "DeviceName must have at least 4 chars");
            checkArgument(pairingRequestPayload.getDeviceName().length() <= 32, "DeviceName must not be longer than 32 chars");

            BinaryEncodingUtils.writeString(out, pairingRequestPayload.getPairingCodeId(), MAX_PAIRING_CODE_ID_LENGTH);
            BinaryEncodingUtils.writeBytes(out, pairingRequestPayload.getDevicePublicKey().getEncoded(), MAX_PUBLIC_KEY_BYTES);
            BinaryEncodingUtils.writeString(out, pairingRequestPayload.getDeviceName(), MAX_DEVICE_NAME_LENGTH);

            out.writeLong(pairingRequestPayload.getTimestamp().toEpochMilli());

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode PairingRequest", e);
        }
    }
}
