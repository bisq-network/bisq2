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

import bisq.security.keys.KeyGeneration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;

public class AesGcm {
    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String AES_GCM_NO_PADDING_CIPHER_NAME = "AES/GCM/NoPadding";
    private static final String AES = "AES";
    private static final int MAX_TAG_LENGTH = 128;
    private static final int RECOMMENDED_IV_LENGTH = 12;

    public static byte[] encrypt(SecretKey secretKey, byte[] iv, byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING_CIPHER_NAME, BouncyCastleProvider.PROVIDER_NAME);
        GCMParameterSpec spec = new GCMParameterSpec(MAX_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(SecretKey secretKey, byte[] iv, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING_CIPHER_NAME, BouncyCastleProvider.PROVIDER_NAME);
        GCMParameterSpec spec = new GCMParameterSpec(MAX_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(cipherText);
    }

    public static SecretKey generateSharedAesSecretKey(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KeyGeneration.ECDH, BouncyCastleProvider.PROVIDER_NAME);
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret(AES);
    }

    public static IvParameterSpec generateIv() {
        return new IvParameterSpec(new SecureRandom().generateSeed(RECOMMENDED_IV_LENGTH));
    }


}
