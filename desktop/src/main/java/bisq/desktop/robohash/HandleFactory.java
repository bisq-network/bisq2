package bisq.desktop.robohash;

public class HandleFactory {

    /**
     * @param data max 14 bytes
     * @return A value 0..13
     */
    static long calculateHandleValue(byte[] data) {
        if (data.length > 14) throw new IllegalArgumentException();
        long value = 0;
        for (int i = 0; i < data.length; i++) {

            int nibble = data[i];
            if (nibble > 0xf)
                throw new IllegalArgumentException(String.format("nibble to large @%d: %02X", i, nibble));
            value <<= 4;
            value |= nibble;
        }
        value |= ((long) data.length) << (14 * 4);
        return value;
    }

    /**
     * @param index 0..13
     * @return A value 0..15
     */
    static byte getNibbleAt(long handle, int index) {
        if (index < 0 || index > 15) throw new IllegalArgumentException(String.format("index @%d", index));

        long mask = (long) 0xf << (index * 4);
        long maskedValue = (handle & mask);

        return (byte) (maskedValue >> index * 4);
    }

    static int getSize(long value) {
        return getNibbleAt(value, 14);
    }

    public static byte[] bucketValues(long value) {
        int buckets = getSize(value);
        byte[] values = new byte[buckets];
        for (int i = 0; i < buckets; i++) {
            values[buckets - i - 1] = getNibbleAt(value, i);
        }
        return values;
    }
}
