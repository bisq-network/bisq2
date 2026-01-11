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
import bisq.common.util.BinaryDecodingUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public final class PairingQrDecoder {

    public static PairingCode decode(String base64) {
        byte[] bytes = Base64.getUrlDecoder().decode(base64);
        return decode(bytes);
    }

    public static PairingCode decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            byte version = BinaryDecodingUtils.readByte(in);
            if (version != PairingCode.VERSION) {
                throw new IllegalArgumentException("Unsupported version");
            }

            String id = BinaryDecodingUtils.readString(in);
            Instant expiresAt = Instant.ofEpochMilli(BinaryDecodingUtils.readLong(in));

            int numPermissions = BinaryDecodingUtils.readInt(in);
            if (numPermissions < 0 || numPermissions > Permission.values().length) {
                throw new IllegalArgumentException("Invalid number of permissions: " + numPermissions);
            }
            Set<Permission> permissions = EnumSet.noneOf(Permission.class);
            for (int i = 0; i < numPermissions; i++) {
                permissions.add(Permission.fromId(BinaryDecodingUtils.readInt(in)));
            }

            return new PairingCode(id, expiresAt, permissions);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode pairing QR", e);
        }
    }
}
