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

import com.google.common.primitives.Ints;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArrayUtils {

    public static byte[] concat(byte[]... bytes) {
        int length = 0;
        for (byte[] aByte : bytes) {
            length += aByte.length;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        for (byte[] aByte : bytes) {
            byteBuffer.put(aByte);
        }
        return byteBuffer.array();
    }

    public static byte[] integerToByteArray(int intValue, int numBytes) {
        byte[] bytes = new byte[numBytes];
        for (int i = numBytes - 1; i >= 0; i--) {
            bytes[i] = ((byte) (intValue & 0xFF));
            intValue >>>= 8;
        }
        return bytes;
    }

    public static int byteArrayToInteger(byte[] bytes) {
        int result = 0;
        for (byte aByte : bytes) {
            result = result << 8 | aByte & 0xff;
        }
        return result;
    }

    public static byte[] copyRightAligned(byte[] src, int newLength) {
        byte[] destination = new byte[newLength];
        int srcPos = Math.max(src.length - newLength, 0);
        int destinationPosition = Math.max(newLength - src.length, 0);
        System.arraycopy(src, srcPos, destination, destinationPosition, newLength - destinationPosition);
        return destination;
    }

    public static byte[] copyOf(byte[] src) {
        return Arrays.copyOf(src, src.length);
    }

    public static byte[] integersToBytesBE(int[] integers) {
        byte[] bytes = new byte[integers.length * 4];
        int i = 0;
        for (int v : integers) {
            bytes[i++] = (byte) (v >> 24);
            bytes[i++] = (byte) (v >> 16);
            bytes[i++] = (byte) (v >> 8);
            bytes[i++] = (byte) v;
        }
        return bytes;
    }

    public static int[] bytesToIntegersBE(byte[] bytes) {
        int[] integers = new int[bytes.length / 4];
        for (int i = 0, j = 0; i < bytes.length / 4; i++) {
            integers[i] = Ints.fromBytes(bytes[j++], bytes[j++], bytes[j++], bytes[j++]);
        }
        return integers;
    }
}
