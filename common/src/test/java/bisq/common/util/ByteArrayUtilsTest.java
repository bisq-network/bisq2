package bisq.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ByteArrayUtilsTest {
    @Test
    @DisplayName("concat handles empty byte arrays")
    public void concat_handles_empty_byte_arrays() {
        assertArrayEquals(new byte[]{1, 2, 3}, ByteArrayUtils.concat(new byte[]{1, 2, 3}, new byte[]{}));
        assertArrayEquals(new byte[]{4, 5}, ByteArrayUtils.concat(new byte[]{}, new byte[]{4, 5}));
        assertArrayEquals(new byte[]{}, ByteArrayUtils.concat(new byte[]{}));
    }

    @Test
    @DisplayName("concat handles typical cases")
    public void concat_handles_typical_cases() {
        assertArrayEquals(new byte[]{}, ByteArrayUtils.concat());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteArrayUtils.concat(new byte[]{1, 2}, new byte[]{}, new byte[]{3, 4}));
    }

    @Test
    @DisplayName("integer to byte array handles sizes")
    public void integer_to_byte_array_handles_sizes() {
        assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78},
                ByteArrayUtils.integerToByteArray(0x12345678, 4));
        assertArrayEquals(new byte[]{0x56, 0x78},
                ByteArrayUtils.integerToByteArray(0x12345678, 2));
        assertArrayEquals(new byte[]{0x78},
                ByteArrayUtils.integerToByteArray(0x12345678, 1));
        assertArrayEquals(new byte[]{},
                ByteArrayUtils.integerToByteArray(0x12345678, 0));
    }

    @Test
    @DisplayName("byte array to integer handles typical cases")
    public void byte_array_to_integer_handles_typical_cases() {
        assertEquals(0x12345678, ByteArrayUtils.byteArrayToInteger(new byte[]{0x12, 0x34, 0x56, 0x78}));
        assertEquals(0, ByteArrayUtils.byteArrayToInteger(new byte[]{}));
        assertEquals(0xFFFFFFFF, ByteArrayUtils.byteArrayToInteger(new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        }));
    }

    @Test
    @DisplayName("copy right aligned handles padding and truncation")
    public void copy_right_aligned_handles_padding_and_truncation() {
        assertArrayEquals(new byte[]{0, 0, 1, 2}, ByteArrayUtils.copyRightAligned(new byte[]{1, 2}, 4));
        assertArrayEquals(new byte[]{3, 4, 5}, ByteArrayUtils.copyRightAligned(new byte[]{1, 2, 3, 4, 5}, 3));
        assertArrayEquals(new byte[]{7, 8}, ByteArrayUtils.copyRightAligned(new byte[]{7, 8}, 2));
    }

    @Test
    @DisplayName("copy of returns new array")
    public void copy_of_returns_new_array() {
        byte[] source = new byte[]{9, 10, 11};
        byte[] copy = ByteArrayUtils.copyOf(source);
        assertArrayEquals(source, copy);
        assertNotSame(source, copy);
    }

    @Test
    @DisplayName("integers to bytes be and back")
    public void integers_to_bytes_be_and_back() {
        int[] input = new int[]{0x01020304, 0xA0B0C0D0, 0x80000000};
        byte[] bytes = ByteArrayUtils.integersToBytesBE(input);
        assertArrayEquals(new byte[]{
                0x01, 0x02, 0x03, 0x04,
                (byte) 0xA0, (byte) 0xB0, (byte) 0xC0, (byte) 0xD0,
                (byte) 0x80, 0x00, 0x00, 0x00
        }, bytes);
        assertArrayEquals(input, ByteArrayUtils.bytesToIntegersBE(bytes));
        assertArrayEquals(new int[]{}, ByteArrayUtils.bytesToIntegersBE(new byte[]{}));
    }

    @Test
    @DisplayName("get random bytes returns requested length")
    public void get_random_bytes_returns_requested_length() {
        assertEquals(0, ByteArrayUtils.getRandomBytes(0).length);
        assertEquals(32, ByteArrayUtils.getRandomBytes(32).length);
    }
}
