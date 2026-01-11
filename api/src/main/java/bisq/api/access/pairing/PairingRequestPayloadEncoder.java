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

public class PairingRequestPayloadEncoder {
    public static byte[] encode(PairingRequestPayload pairingRequestPayload) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(PairingRequestPayload.VERSION);

            BinaryEncodingUtils.writeString(out, pairingRequestPayload.getPairingCodeId());
            BinaryEncodingUtils.writeBytes(out, pairingRequestPayload.getDevicePublicKey().getEncoded());
            BinaryEncodingUtils.writeString(out, pairingRequestPayload.getDeviceName());

            out.writeLong(pairingRequestPayload.getTimestamp().toEpochMilli());

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode PairingRequest", e);
        }
    }
}
