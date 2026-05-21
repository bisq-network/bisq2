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

package bisq.security.mobile_notifications;


import bisq.common.encoding.Base64;
import bisq.common.util.ByteArrayUtils;
import bisq.security.AesGcm;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
public class MobileNotificationEncryption {
    private static final int GCM_NONCE_LENGTH = 12;

    /**
     * Encrypt the message using the device's public key (ECIES for EC keys).
     * Used as fallback when no symmetric key is available.
     *
     * @param publicKeyBase64 The Base64 encoded public key
     * @param message         The message to encrypt
     * @return Base64 encoded encrypted message
     */
    public static String encrypt(String publicKeyBase64, String message) throws GeneralSecurityException {
        try {
            byte[] publicKeyBytes = Base64.decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Encrypt with ECIES (Elliptic Curve Integrated Encryption Scheme)
            // Using AES-128-CBC with HMAC-SHA1 for MAC (default BouncyCastle ECIES parameters)
            //
            // CRITICAL: These IESParameterSpec parameters MUST match exactly on the mobile client side:
            // - derivation: empty byte[] (not null) - used for KDF derivation parameter
            // - encoding: empty byte[] (not null) - used for KDF encoding parameter
            // - macKeySize: 128 bits - selects AES-128-CBC with HMAC-SHA1 per BouncyCastle ECIES defaults
            //
            // Mobile teams: Verify your decryption uses identical IESParameterSpec(new byte[0], new byte[0], 128)
            // Any mismatch in these parameters will cause decryption to fail silently or produce garbage.
            byte[] derivation = new byte[0];  // Intentionally empty, not null
            byte[] encoding = new byte[0];    // Intentionally empty, not null
            int macKeySize = 128;
            IESParameterSpec iesSpec = new IESParameterSpec(derivation, encoding, macKeySize);
            Cipher cipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, iesSpec);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.encode(encryptedBytes);
        } catch (Exception e) {
            log.error("ECIES encryption failed", e);
            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Encrypt the message using a shared AES-256 symmetric key (AES-GCM).
     * Used for iOS devices that provide a symmetric key during registration,
     * enabling decryption in the Notification Service Extension via Apple CryptoKit.
     *
     * <p>Output format: nonce (12 bytes) || ciphertext || authentication tag (16 bytes)
     * This matches the format expected by the iOS NSE (CryptoKit AES.GCM).
     *
     * @param symmetricKeyBase64 The Base64 encoded AES-256 key (32 bytes)
     * @param message            The message to encrypt
     * @return Base64 encoded encrypted message (nonce + ciphertext + tag)
     */
    public static String encryptWithSymmetricKey(String symmetricKeyBase64, String message) throws GeneralSecurityException {
        try {
            byte[] keyBytes = Base64.decode(symmetricKeyBase64);
            if (keyBytes.length != 32) {
                throw new GeneralSecurityException("Symmetric key must be 256 bits (32 bytes), got " + keyBytes.length);
            }

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            byte[] nonce = ByteArrayUtils.getRandomBytes(GCM_NONCE_LENGTH);

            // Reuse the project's AesGcm utility for the cipher operation.
            // Java's GCM mode appends the authentication tag to the ciphertext.
            byte[] ciphertextWithTag = AesGcm.encrypt(secretKey, nonce, message.getBytes(StandardCharsets.UTF_8));

            // Output: nonce || ciphertext || tag — matches CryptoKit AES.GCM on iOS
            return Base64.encode(ByteArrayUtils.concat(nonce, ciphertextWithTag));
        } catch (Exception e) {
            log.error("AES-GCM encryption failed", e);
            throw new GeneralSecurityException(e);
        }
    }
}
