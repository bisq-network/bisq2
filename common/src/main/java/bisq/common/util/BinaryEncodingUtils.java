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

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class BinaryEncodingUtils {
    public static void writeString(DataOutputStream out, String value, int maxStringLength) throws IOException {
        if (value.length() > maxStringLength) {
            throw new IllegalArgumentException("String value too long.");
        }
        writeString(out, value);
    }

    public static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeBytes(out, bytes);
    }

    public static void writeInt(DataOutputStream out, int value) throws IOException {
        out.writeInt(value);
    }

    public static void writeBytes(DataOutputStream out, byte[] value, int maxLength) throws IOException {
        if (value.length > maxLength) {
            throw new IllegalArgumentException("Byte array too long.");
        }
        writeBytes(out, value);
    }

    public static void writeBytes(DataOutputStream out, byte[] value) throws IOException {
        out.writeShort(value.length);
        out.write(value);
    }

    public static void writeLong(DataOutputStream out, long value) throws IOException {
        out.writeLong(value);
    }

    public static void writeByte(DataOutputStream out, byte value) throws IOException {
        out.writeByte(value);
    }
}

