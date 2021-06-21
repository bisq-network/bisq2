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

package network.misq.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.NullCipher;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;

@Slf4j
public class AsymEncryption {
    static final String CIPHER_ALGO = "ECIESwithAES-CBC";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    static byte[] encrypt(byte[] message, ECPublicKey publicKey) throws GeneralSecurityException {
        ECPublicKeySpec spec = new ECPublicKeySpec(publicKey.getW(), publicKey.getParams());
        // todo find cipher which is supported. "EC" oe "ECDH" are not found. ECIES would require IES params
        // NullCipher is just a dummy doing nothing...
        // Cipher cipher = new NullCipher();
        Cipher cipher = Cipher.getInstance("ECDHE-RSA-AES256-GCM-SHA384");
        // Cipher cipher = Cipher.getInstance("ECDHE");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec.getParams());
        return cipher.doFinal(message);
    }

    static byte[] decrypt(byte[] encrypted, ECPrivateKey privateKey) throws GeneralSecurityException {
        ECPrivateKeySpec spec = new ECPrivateKeySpec(privateKey.getS(), privateKey.getParams());
        Cipher cipher = new NullCipher(); //todo
        cipher.init(Cipher.DECRYPT_MODE, privateKey, spec.getParams());
        return cipher.doFinal(encrypted);
    }
}
