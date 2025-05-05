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

package bisq.security.keys;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base32;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class I2pKeyGeneration {
    static {
        // Ensure BouncyCastle is available
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] generatePrivateKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            keyGen.initialize(256, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            return keyPair.getPrivate().getEncoded(); // Returns PKCS#8 format
        } catch (Exception e) {
            throw new RuntimeException("Error generating private key", e);
        }
    }

    public static I2pKeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            keyGen.initialize(256, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            byte[] privateKey = keyPair.getPrivate().getEncoded();  // PKCS#8
            byte[] publicKey = keyPair.getPublic().getEncoded();    // X.509
            return new I2pKeyPair(privateKey, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair", e);
        }
    }

    public static byte[] getPublicKey(byte[] privateKeyEncoded) {
        try {
            // Reconstruct private key from bytes
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyEncoded));
            // Derive public key from private key
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            keyGen.initialize(256);
            // Note: Ed25519 does not support regenerating public key from encoded private key directly without access to internal key material
            // Best to cache the public key; otherwise return null or regenerate a new pair
            throw new UnsupportedOperationException("Ed25519 public key cannot be derived from PKCS#8 private key alone.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to derive public key from private key", e);
        }
    }

    public static String getDestinationFromPublicKey(byte[] publicKey) {
        // In a real I2P context, the destination would be derived from full keypair bytes and signed
        // This simplified version encodes the public key as a Base32 .b32.i2p address
        return Base32.toBase32String(publicKey).toLowerCase() + ".b32.i2p";
    }
}

