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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BinaryEncodingUtilsTest {

    @Test
    void writeByte_writesSingleByte() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeByte(out, (byte) 0x7F);
        out.flush();

        byte[] bytes = baos.toByteArray();
        assertEquals(1, bytes.length);
        assertEquals((byte) 0x7F, bytes[0]);
    }

    @Test
    void writeInt_writesFourBytes() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeInt(out, 42);
        out.flush();

        assertEquals(4, baos.toByteArray().length);
    }

    @Test
    void writeLong_writesEightBytes() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeLong(out, 123456789L);
        out.flush();

        assertEquals(8, baos.toByteArray().length);
    }

    @Test
    void writeBytes_prefixesLength() throws Exception {
        byte[] data = {1, 2, 3};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeBytes(out, data);
        out.flush();

        byte[] result = baos.toByteArray();

        // 2 bytes length + payload
        assertEquals(2 + data.length, result.length);
    }

    @Test
    void writeBytes_respectsMaxLength() {
        byte[] data = new byte[5];

        assertThrows(IllegalArgumentException.class, () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEncodingUtils.writeBytes(out, data, 4);
        });
    }

    @Test
    void writeString_encodesUtf8WithLengthPrefix() throws Exception {
        String value = "hello";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        BinaryEncodingUtils.writeString(out, value);
        out.flush();

        byte[] bytes = baos.toByteArray();
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);

        assertEquals(2 + utf8.length, bytes.length);
    }

    @Test
    void writeString_respectsMaxLength() {
        String value = "abcd";

        assertThrows(IllegalArgumentException.class, () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            BinaryEncodingUtils.writeString(out, value, 3);
        });
    }
}
