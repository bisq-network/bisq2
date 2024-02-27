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

package bisq.desktop.components.cathash;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HandleFactory {

    public Handle calculateHandle(byte[] data) {
        return new Handle(calculateHandleValue(data));
    }

    /**
     * Encodes an array of bytes (data) into a single long value, which serves as a compact, unique identifier
     * (or "handle") for a set of values. Each byte in the array represents a "nibble" (a 4-bit value), and the function
     * ensures that these values, along with the length of the data, are packed into the returned long.
     *
     * @param data The distributed hash over the buckets
     * @return val The composite handle value, which encodes both the sequence of nibbles and the length of the data array
     */
    static long calculateHandleValue(byte[] data) {
        // Check if the input array exceeds the maximum length of 14 bytes.
        // This limit ensures that the data can be encoded into a 64-bit long value without overflow.
        // Since 8 bits are reserved for length encoding only 56 bits can be used (14 * 4)
        // This means that the maximum number of buckets that we can have is 14
        if (data.length > 14) {
            throw new IllegalArgumentException();
        }

        long val = 0;
        for (int i = 0; i < data.length; i++) {
            int nibble = data[i];

            // Validate that the current nibble does not exceed the maximum value of 15 (0xF), ensuring it's a
            // valid 4-bit value.
            // Each nibble uses 4 bits, therefore we can only encode 2^4 (0..15) possibilities
            // (i.e. max size per bucket is 15, which represent 16 images)
            if (nibble > 15) { // 0xf
                throw new IllegalArgumentException(String.format("nibble to large @%d: %02X", i, nibble));
            }

            // Shift the current handle value 4 bits to the left to make room for the new nibble. This operation
            // progressively builds up the handle value from its constituent nibbles.
            val <<= 4;

            // Incorporate the current nibble into the lowest 4 bits of the handle value.
            val |= nibble;
        }

        // After processing all nibbles, encode the length of the data array into the handle value.
        // This is achieved by shifting the length leftward by (14 * 4) bits (56 bits), which positions the length
        // information in the upper 8 bits of the 64-bit long value. This ensures the length can be retrieved from the
        // handle and also contributes to the uniqueness of the handle.
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
