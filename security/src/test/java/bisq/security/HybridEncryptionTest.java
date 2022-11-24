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
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SignatureException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class HybridEncryptionTest {

    private final KeyPair keyPairSender = KeyGeneration.generateKeyPair();
    private final KeyPair keyPairReceiver = KeyGeneration.generateKeyPair();

    public HybridEncryptionTest() throws GeneralSecurityException {
    }

    @Test
    void testValidEncryption() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);

        byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, keyPairReceiver);
        assertArrayEquals(message, decrypted);
    }

    @Test
    void decryptWithWrongKey() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);

        byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();
        byte[] iv = confidentialData.getIv();
        byte[] cypherText = confidentialData.getCipherText();

        KeyPair fakeKeyPair = KeyGeneration.generateKeyPair();
        byte[] fakeSignature = SignatureUtil.sign(cypherText, fakeKeyPair.getPrivate());
        ConfidentialData withFakeSigAndPubKey = new ConfidentialData(encodedSenderPublicKey, iv, cypherText, fakeSignature);
        try {
            // Expect to fail as pub key in method call not matching the one in sealed data
            HybridEncryption.decryptAndVerify(withFakeSigAndPubKey, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    void decryptWrongSignature() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);

        byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();
        byte[] iv = confidentialData.getIv();
        byte[] cypherText = confidentialData.getCipherText();

        try {
            ConfidentialData withFakeSig = new ConfidentialData(encodedSenderPublicKey, iv, cypherText, "signature".getBytes());
            HybridEncryption.decryptAndVerify(withFakeSig, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof SignatureException);
        }
    }

    @Test
    void decryptWithWrongIv() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);

        byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();
        byte[] cypherText = confidentialData.getCipherText();
        byte[] signature = confidentialData.getSignature();

        // fake iv
        try {
            ConfidentialData withFakeIv = new ConfidentialData(encodedSenderPublicKey, "iv".getBytes(), cypherText, signature);
            HybridEncryption.decryptAndVerify(withFakeIv, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof AEADBadTagException);
        }
    }

    @Test
    void decryptBrokenMessageFailure() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);

        // failure cases
        byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();
        byte[] iv = confidentialData.getIv();
        byte[] cipherText = confidentialData.getCipherText();
        byte[] signature = confidentialData.getSignature();

        cipherText[2] = (byte) (~cipherText[2] & 0xff);

        try {
            ConfidentialData withFakeHmac = new ConfidentialData(encodedSenderPublicKey, iv, cipherText, signature);
            HybridEncryption.decryptAndVerify(withFakeHmac, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
