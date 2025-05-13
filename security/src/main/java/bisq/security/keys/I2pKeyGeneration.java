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

import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.crypto.SHA256Generator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class I2pKeyGeneration {
    static {
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
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyEncoded));
            throw new UnsupportedOperationException("Ed25519 public key cannot be derived from PKCS#8 private key alone.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to derive public key from private key", e);
        }
    }

    /**
     * Derives a valid .b32.i2p address from a Destination byte array.
     * This expects a complete I2P Destination object (usually ~387 bytes).
     *
     * @param publicKeyBytes The raw I2P Destination byte array.
     * @return The corresponding .b32.i2p address.
     */
    public static String getDestinationFromPublicKey(byte[] publicKeyBytes) {
        try {
            Destination destination = new Destination();
            destination.fromByteArray(publicKeyBytes);
            byte[] hash = SHA256Generator.getInstance().calculateHash(destination.toByteArray()).getData();
            return Base32.encode(hash).toLowerCase() + ".b32.i2p";
        } catch (Exception e) {
            throw new RuntimeException("Error generating .b32.i2p address from Destination bytes", e);
        }
    }

}
