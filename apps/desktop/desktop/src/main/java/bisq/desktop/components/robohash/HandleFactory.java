package bisq.desktop.components.robohash;

public class HandleFactory {

    public Handle calculateHandle(byte[] data) {
        return new Handle(calculateHandleValue(data));
    }

    static long calculateHandleValue(byte[] data) {
        if (data.length > 14) throw new IllegalArgumentException();
        long val = 0;
        for (int i = 0; i < data.length; i++) {

            int nibble = data[i];
            if (nibble > 0xf)
                throw new IllegalArgumentException(String.format("nibble to large @%d: %02X", i, nibble));
            val <<= 4;
            val |= nibble;
        }
        val |= ((long) data.length) << (14 * 4);
        return val;
    }

    static byte getNibbleAt(long value, int index) {

        if (index < 0 || index > 15) throw new IllegalArgumentException(String.format("index @%d", index));

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
