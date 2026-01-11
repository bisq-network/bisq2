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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BinaryDecodingUtilsTest {

    @Test
    void readByte_readsCorrectValue() throws Exception {
        byte[] data = {(byte) 0x42};
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        byte value = BinaryDecodingUtils.readByte(in);
        assertEquals((byte) 0x42, value);
    }

    @Test
    void readInt_readsCorrectValue() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(123);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(123, BinaryDecodingUtils.readInt(in));
    }

    @Test
    void readLong_readsCorrectValue() throws Exception {
        long value = 987654321L;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeLong(value);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(value, BinaryDecodingUtils.readLong(in));
    }

    @Test
    void readBytes_readsPayloadWithLengthPrefix() throws Exception {
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
    void readBytes_respectsMaxLength() throws Exception {
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
    void readString_readsUtf8String() throws Exception {
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
    void fullRoundtrip_stringIntLong() throws Exception {
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
