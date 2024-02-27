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

package bisq.desktop.components.robohash;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HandleFactory {

    public Handle calculateHandle(byte[] data) {
        return new Handle(calculateHandleValue(data));
    }

    static long calculateHandleValue(byte[] data) {
        if (data.length > 14) throw new IllegalArgumentException();
        long val = 0;
        for (int i = 0; i < data.length; i++) {
            int nibble = data[i];
            if (nibble > 15) { // 0xf
                throw new IllegalArgumentException(String.format("nibble to large @%d: %02X", i, nibble));
            }
            val <<= 4;
            val |= nibble;
        }
        val |= ((long) data.length) << (14 * 4);
        return val;
    }

    static byte getNibbleAt(long value, int index) {
        if (index < 0 || index > 15) {
            throw new IllegalArgumentException(String.format("index @%d", index));
        }

        long mask = (long) 0xf << (index * 4);
        long maskedValue = (value & mask);

        return (byte) (maskedValue >> index * 4);
    }

    static int getSize(long value) {
        return getNibbleAt(value, 14);
    }

    public static byte[] bucketValues(long handle) {
        int buckets = getSize(handle);
        byte[] values = new byte[buckets];
        for (int i = 0; i < buckets; i++) {
            values[buckets - i - 1] = getNibbleAt(handle, i);
        }
        return values;
    }
}
