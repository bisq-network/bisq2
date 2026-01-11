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

package bisq.common.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class BinaryDecodingUtils {
    public static byte[] readBytes(DataInputStream in) throws IOException {
        return readBytes(in, Integer.MAX_VALUE);
    }

    public static byte[] readBytes(DataInputStream in, int maxLength) throws IOException {
        int length = in.readUnsignedShort();
        if (length > maxLength) {
            throw new IllegalArgumentException("Byte array exceeds max length: " + length);
        }

        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    public static String readString(DataInputStream in) throws IOException {
        return readString(in, Integer.MAX_VALUE);
    }

    public static String readString(DataInputStream in, int maxLength) throws IOException {
        byte[] bytes = readBytes(in, maxLength);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int readInt(DataInputStream in) throws IOException {
        return in.readInt();
    }

    public static long readLong(DataInputStream in) throws IOException {
        return in.readLong();
    }

    public static byte readByte(DataInputStream in) throws IOException {
        return in.readByte();
    }
}

