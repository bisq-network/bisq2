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

import net.i2p.crypto.KeyGenerator;
import net.i2p.crypto.SHA256Generator;
import net.i2p.crypto.SigType;
import net.i2p.data.Base32;
import net.i2p.data.Certificate;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.data.SigningPrivateKey;
import net.i2p.data.SigningPublicKey;
import net.i2p.data.SimpleDataStructure;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class I2PKeyGeneration {

    /**
     * Generates a new I2P key pair including full Destination data.
     *
     * @return An {@link I2PKeyPair} containing serialized private key and full Destination public key bytes.
     */
    public static I2PKeyPair generateKeyPair() throws GeneralSecurityException {
            // Set up context and key generator
        KeyGenerator keyGen = KeyGenerator.getInstance();

        // Generate encryption key pair (ElGamal 2048 by default)
        Object[] encPair = keyGen.generatePKIKeypair();
        PublicKey pubKey = (PublicKey) encPair[0];
        PrivateKey privKey = (PrivateKey) encPair[1];

        // Generate signing key pair (Ed25519)
        SimpleDataStructure[] signingPair = keyGen.generateSigningKeys(SigType.EdDSA_SHA512_Ed25519);
        SigningPublicKey sigPub = (SigningPublicKey) signingPair[0];
        SigningPrivateKey sigPriv = (SigningPrivateKey) signingPair[1];

        int paddingLength = 384 - (256 + 32 );
        SecureRandom rng = new SecureRandom();
        byte[] pad = new byte[paddingLength];
        rng.nextBytes(pad);

        // Construct the Destination
        Destination dest = new Destination();
        dest.setPublicKey(pubKey);
        dest.setSigningPublicKey(sigPub);
        dest.setCertificate(Certificate.NULL_CERT);
        dest.setPadding(pad);

        return new I2PKeyPair(privKey, sigPriv, dest);

    }

    /**
     * Not implemented: deriving public key from private key is not supported for I2P Destination format.
     */
    public static byte[] getPublicKey(byte[] privateKeyEncoded) {
        throw new UnsupportedOperationException("Deriving Destination from private key is not supported.");
    }

    /**
     * Converts Destination bytes to a .b32.i2p address.
     */
    public static String getDestinationFromPublicKey(byte[] publicKeyBytes) {
        if (publicKeyBytes == null || publicKeyBytes.length == 0) {
            throw new IllegalArgumentException("Public key bytes cannot be null or empty");
        }
        try {
            Destination destination = new Destination();
            destination.fromByteArray(publicKeyBytes);
            byte[] hash = SHA256Generator.getInstance().calculateHash(destination.toByteArray()).getData();
            return Base32.encode(hash).toLowerCase() + ".b32.i2p";
        } catch (Exception e) {
            throw new RuntimeException("Invalid Destination byte array", e);
        }
    }

}
