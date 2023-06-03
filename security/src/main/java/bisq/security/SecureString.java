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

package bisq.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

// Borrowed from https://github.com/sparrowwallet/drongo

/**
 * This is not a string but a CharSequence that can be cleared of its memory.
 * Important for handling passwords. Represents text that should be kept
 * confidential, such as by deleting it from computer memory when no longer
 * needed or garbaged collected.
 */
public class SecureString implements CharSequence {

    private final int[] chars;
    private final int[] pad;

    public SecureString(final CharSequence original) {
        this(0, original.length(), original);
    }

    public SecureString(final int start, final int end, final CharSequence original) {
        final int length = end - start;
        pad = new int[length];
        chars = new int[length];
        scramble(start, length, original);
    }

    @Override
    public char charAt(final int i) {
        return (char) (pad[i] ^ chars[i]);
    }

    @Override
    public int length() {
        return chars.length;
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return new SecureString(start, end, this);
    }

    /**
     * Convert array back to String but not using toString(). See toString() docs
     * below.
     */
    public String asString() {
        final char[] value = new char[chars.length];
        for (int i = 0; i < value.length; i++) {
            value[i] = charAt(i);
        }
        return new String(value);
    }

    /**
     * Manually clear the underlying array holding the characters
     */
    public void clear() {
        Arrays.fill(chars, '0');
        Arrays.fill(pad, 0);
    }

    /**
     * Protect against using this class in log statements.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SecureString does not support the 'toString' method.";
    }

    /**
     * Randomly pad the characters to not store the real character in memory.
     *
     * @param start      start of the {@code CharSequence}
     * @param length     length of the {@code CharSequence}
     * @param characters the {@code CharSequence} to scramble
     */
    private void scramble(final int start, final int length, final CharSequence characters) {
        final SecureRandom random = new SecureRandom();
        for (int i = start; i < length; i++) {
            final char charAt = characters.charAt(i);
            pad[i] = random.nextInt();
            chars[i] = pad[i] ^ charAt;
        }
    }

    public static byte[] toBytesUTF8(CharSequence charSequence) {
        CharBuffer charBuffer = CharBuffer.wrap(charSequence);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static SecureString fromBytesUTF8(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
        SecureString secureString = new SecureString(charBuffer);
        Arrays.fill(charBuffer.array(), (char) 0);
        return secureString;
    }

    public static byte[] toBytesUTF16(CharSequence charSequence) {
        byte[] byteArray = new byte[charSequence.length() << 1];
        for (int i = 0; i < charSequence.length(); i++) {
            int bytePosition = i << 1;
            byteArray[bytePosition] = (byte) ((charSequence.charAt(i) & 0xFF00) >> 8);
            byteArray[bytePosition + 1] = (byte) (charSequence.charAt(i) & 0x00FF);
        }
        return byteArray;
    }

    public static boolean isValidUTF16(CharSequence charSequence) {
        for (int i = 0; i < charSequence.length(); i++) {
            if (Character.isLowSurrogate(charSequence.charAt(i)) && (i == 0 || !Character.isHighSurrogate(charSequence.charAt(i - 1)))
                    || Character.isHighSurrogate(charSequence.charAt(i)) && (i == charSequence.length() - 1 || !Character.isLowSurrogate(charSequence.charAt(i + 1)))) {
                return false;
            }
        }
        return true;
    }
}
