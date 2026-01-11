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

import bisq.api.access.permissions.Permission;
import bisq.common.util.BinaryEncodingUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public final class PairingQrEncoder {
    public static byte[] encode(PairingCode code) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            BinaryEncodingUtils.writeByte(out, PairingCode.VERSION);

            BinaryEncodingUtils.writeString(out, code.getId());

            BinaryEncodingUtils.writeLong(out, code.getExpiresAt().toEpochMilli());

            List<Permission> permissions = code.getGrantedPermissions().stream()
                    .sorted(Comparator.comparing(Permission::getId))
                    .toList();
            int numPermissions = permissions.size();
            BinaryEncodingUtils.writeInt(out, numPermissions);

            for (Permission permission : permissions) {
                int id = permission.getId();
                BinaryEncodingUtils.writeInt(out, id);
            }

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode pairing QR", e);
        }
    }
}

