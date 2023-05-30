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

// Borrowed from https://github.com/sparrowwallet/drongo

package bisq.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.SCrypt;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * <p>This class encrypts and decrypts byte arrays and strings using scrypt as the
 * key derivation function and AES for the encryption.</p>
 *
 * <p>You can use this class to:</p>
 *
 * <p>1) Using a user password, create an AES key that can encrypt and decrypt your private keys.
 * To convert the password to the AES key, scrypt is used. This is an algorithm resistant
 * to brute force attacks. You can use the ScryptParameters to tune how difficult you
 * want this to be generation to be.</p>
 *
 * <p>2) Using the AES Key generated above, you then can encrypt and decrypt any bytes using
 * the AES symmetric cipher. Eight bytes of salt is used to prevent dictionary attacks.</p>
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class ScryptKeyDeriver {
    public static final int SALT_LENGTH = 8;
    private static final SecureRandom secureRandom = new SecureRandom();

    public static byte[] randomSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    @Getter
    private final ScryptParameters scryptParameters;

    public ScryptKeyDeriver() {
        this(randomSalt());
    }

    public ScryptKeyDeriver(byte[] salt) {
        this(new ScryptParameters(salt));
    }

    /**
     * Encryption/Decryption using custom number of cost parameters and a random salt.
     * As of August 2016, a useful value for mobile devices is 4096 (derivation takes about 1 second).
     *
     * @param cost CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than
     *             <code>2^(128 * r / 8)</code>.
     */
    public ScryptKeyDeriver(int cost) {
        this(new ScryptParameters(randomSalt(), cost));
    }

    public ScryptKeyDeriver(ScryptParameters scryptParameters) {
        this.scryptParameters = scryptParameters;
        if (scryptParameters.getSalt().length == 0) {
            log.warn("You are using a ScryptParameters with no salt. Your encryption may be vulnerable to a dictionary attack.");
        }
    }


    /**
     * Generate AES key.
     * <p>
     * This is a very slow operation compared to encrypt/ decrypt, so it is normally worth caching the result.
     *
     * @param password The password to use in key generation
     * @return The AES key as byte array
     * @throws GeneralSecurityException
     */
    public AesSecretKey deriveKeyFromPassword(CharSequence password) throws GeneralSecurityException {
        byte[] passwordBytes = null;
        try {
            passwordBytes = SecureString.toBytesUTF8(password);
            byte[] key = SCrypt.generate(passwordBytes, scryptParameters.getSalt(), scryptParameters.getCost(), scryptParameters.getBlockSize(), scryptParameters.getParallelization(), scryptParameters.getKeyLength());
            return new AesSecretKey(key);
        } catch (Exception e) {
            throw new GeneralSecurityException("Could not generate key from password and salt.", e);
        } finally {
            // Zero the password bytes.
            if (passwordBytes != null) {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }
    }
}
