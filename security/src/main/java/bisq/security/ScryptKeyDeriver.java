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

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.SCrypt;

import java.security.SecureRandom;
import java.util.Arrays;

// Borrowed from https://github.com/sparrowwallet/drongo

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
public class ScryptKeyDeriver {
    private static final SecureRandom secureRandom = new SecureRandom();

    private final int iterations;

    public ScryptKeyDeriver() {
        this.iterations = 16384;
    }

    /**
     * Encryption/Decryption using custom number of iterations parameters and a random salt.
     * As of August 2016, a useful value for mobile devices is 4096 (derivation takes about 1 second).
     *
     * @param iterations number of scrypt iterations
     */
    public ScryptKeyDeriver(int iterations) {
        this.iterations = iterations;
    }

    /**
     * Generate AES key.
     * <p>
     * This is a very slow operation compared to encrypt/ decrypt so it is normally worth caching the result.
     *
     * @param password The password to use in key generation
     * @return The Key containing the created AES key
     */
    public byte[] deriveKey(CharSequence password) {
        byte[] passwordBytes = null;
        try {
            passwordBytes = SecureString.toBytesUTF8(password);
            byte[] salt = new byte[8];
            secureRandom.nextBytes(salt);
            return SCrypt.generate(passwordBytes,
                    salt,
                    iterations,
                    8,
                    1,
                    32);
        } catch (Exception e) {
            throw new RuntimeException("Could not derive key from password.", e);
        } finally {
            // Zero the password bytes.
            if (passwordBytes != null) {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }
    }
}
