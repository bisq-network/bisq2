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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.crypto.SecretKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
public class AesSecretKey implements SecretKey {
    public static AesSecretKey getClone(AesSecretKey key) {
        return new AesSecretKey(Arrays.copyOf(key.getEncoded(), key.getEncoded().length));
    }

    private final byte[] encoded;

    public AesSecretKey(byte[] encoded) {
        this.encoded = encoded;
    }

    public void clear() {
        Arrays.fill(encoded, (byte) 0);
    }

    @Override
    public String getAlgorithm() {
        return "AES";
    }

    @Override
    public String getFormat() {
        return "RAW";
    }

    @Override
    public String toString() {
        return "toString not supported for security reasons";
        /*return "AESSecretKey{" +
                "\r\n     encoded=" + Hex.encode(encoded) +
                "\r\n}";*/
    }
}