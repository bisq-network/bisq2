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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BinaryDecodingUtilsTest {

    @Test
    @DisplayName("read byte reads correct value")
    void read_byte_reads_correct_value() throws Exception {
        byte[] data = {(byte) 0x42};
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        byte value = BinaryDecodingUtils.readByte(in);
        assertEquals((byte) 0x42, value);
    }

    @Test
    @DisplayName("read int reads correct value")
    void read_int_reads_correct_value() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(123);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(123, BinaryDecodingUtils.readInt(in));
    }

    @Test
    @DisplayName("read long reads correct value")
    void read_long_reads_correct_value() throws Exception {
        long value = 987654321L;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeLong(value);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(value, BinaryDecodingUtils.readLong(in));
    }

    @Test
    @DisplayName("read bytes reads payload with length prefix")
    void read_bytes_reads_payload_with_length_prefix() throws Exception {
        byte[] payload = {10, 20, 30};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeShort(payload.length);
        out.write(payload);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        byte[] result = BinaryDecodingUtils.readBytes(in);

        assertArrayEquals(payload, result);
    }

    @Test
    @DisplayName("read bytes respects max length")
    void read_bytes_respects_max_length() throws Exception {
        byte[] payload = new byte[10];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeShort(payload.length);
        out.write(payload);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        assertThrows(IllegalArgumentException.class, () ->
                BinaryDecodingUtils.readBytes(in, 5)
        );
    }

    @Test
    @DisplayName("read string reads utf8 string")
    void read_string_reads_utf8_string() throws Exception {
        String value = "hello";
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeShort(utf8.length);
        out.write(utf8);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(value, BinaryDecodingUtils.readString(in));
    }

    @Test
    @DisplayName("full roundtrip string int long")
    void full_roundtrip_string_int_long() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeString(out, "abc");
        BinaryEncodingUtils.writeInt(out, 42);
        BinaryEncodingUtils.writeLong(out, 99L);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        assertEquals("abc", BinaryDecodingUtils.readString(in));
        assertEquals(42, BinaryDecodingUtils.readInt(in));
        assertEquals(99L, BinaryDecodingUtils.readLong(in));
    }
}
