package bisq.security;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/*
 * Performs AES/CBC/PKCS7 encryption and decryption
 */
public class AESEncryption {
    /**
     * The size of an AES block in bytes.
     * This is also the length of the initialisation vector.
     */
    public static final int BLOCK_LENGTH = 16;  // = 128 bits.

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Password based encryption using AES - CBC - PKCS7
     */
    public static EncryptedData encrypt(byte[] plainBytes, AESSecretKey aesKey) throws GeneralSecurityException {
        if (plainBytes == null || aesKey == null) {
            throw new GeneralSecurityException("Data and key to encrypt cannot be null");
        }

        try {
            // Generate iv - each encryption call has a different iv.
            byte[] iv = new byte[BLOCK_LENGTH];
            secureRandom.nextBytes(iv);

            ParametersWithIV keyWithIv = new ParametersWithIV(new KeyParameter(aesKey.getEncoded()), iv);

            // Encrypt using AES.
            BufferedBlockCipher cipher = getBufferedBlockCipher();
            cipher.init(true, keyWithIv);
            byte[] encryptedBytes = new byte[cipher.getOutputSize(plainBytes.length)];
            final int length1 = cipher.processBytes(plainBytes, 0, plainBytes.length, encryptedBytes, 0);
            final int length2 = cipher.doFinal(encryptedBytes, length1);

            return new EncryptedData(iv, Arrays.copyOf(encryptedBytes, length1 + length2));
        } catch (Exception e) {
            throw new GeneralSecurityException("Could not encrypt bytes.", e);
        }
    }

    /**
     * Decrypt bytes previously encrypted with this class.
     *
     * @param encryptedData The data to decrypt
     * @param aesKey        The AES key to use for decryption
     * @return The decrypted bytes
     */
    public static byte[] decrypt(EncryptedData encryptedData, AESSecretKey aesKey) throws GeneralSecurityException {
        if (encryptedData == null || aesKey == null) {
            throw new GeneralSecurityException("Data and key to decrypt cannot be null");
        }

        try {
            ParametersWithIV keyWithIv = new ParametersWithIV(new KeyParameter(aesKey.getEncoded()),
                    encryptedData.getInitialisationVector());

            // Decrypt the message.
            BufferedBlockCipher cipher = getBufferedBlockCipher();
            cipher.init(false, keyWithIv);

            byte[] cipherBytes = encryptedData.getEncryptedBytes();
            byte[] decryptedBytes = new byte[cipher.getOutputSize(cipherBytes.length)];
            final int length1 = cipher.processBytes(cipherBytes, 0, cipherBytes.length, decryptedBytes, 0);
            final int length2 = cipher.doFinal(decryptedBytes, length1);

            byte[] decrypted = Arrays.copyOf(decryptedBytes, length1 + length2);
            Arrays.fill(decryptedBytes, (byte) 0);
            return decrypted;
        } catch (Throwable e) {
            throw new GeneralSecurityException("Could not decrypt bytes", e);
        }
    }

    private static PaddedBufferedBlockCipher getBufferedBlockCipher() {
        return new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
    }

}
