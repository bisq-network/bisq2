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
import network.misq.common.util.ByteArrayUtils;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SignatureException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class HybridEncryptionTest {

    @Test
    public void testHybridEncryption() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        KeyPair keyPairSender = KeyGeneration.generateKeyPair();
        KeyPair keyPairReceiver = KeyGeneration.generateKeyPair();

        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message, keyPairReceiver.getPublic(), keyPairSender);
        PublicKey senderPublicKey = keyPairSender.getPublic();
        byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, keyPairReceiver);
        assertArrayEquals(message, decrypted);

        // failure cases
        byte[] encodedSenderPublicKey = confidentialData.getEncodedSenderPublicKey();
        byte[] hmac = confidentialData.getHmac();
        byte[] iv = confidentialData.getIv();
        byte[] cypherText = confidentialData.getCypherText();
        byte[] signature = confidentialData.getSignature();

        KeyPair fakeKeyPair = KeyGeneration.generateKeyPair();
        byte[] bitStream = ByteArrayUtils.concat(hmac, cypherText);
        byte[] fakeSignature = SignatureUtil.sign(bitStream, fakeKeyPair.getPrivate());
        ConfidentialData withFakeSigAndPubKey = new ConfidentialData(encodedSenderPublicKey, hmac, iv, cypherText, fakeSignature);
        try {
            // Expect to fail as pub key in method call not matching the one in sealed data
            HybridEncryption.decryptAndVerify(withFakeSigAndPubKey, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // fake sig or fake signed message throw SignatureException
        try {
            ConfidentialData withFakeSig = new ConfidentialData(encodedSenderPublicKey, hmac, iv, cypherText, "signature".getBytes());
            HybridEncryption.decryptAndVerify(withFakeSig, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof SignatureException);
        }

        // fake iv
        try {
            ConfidentialData withFakeIv = new ConfidentialData(encodedSenderPublicKey, hmac, "iv".getBytes(), cypherText, signature);
            HybridEncryption.decryptAndVerify(withFakeIv, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // fake hmac
        try {
            ConfidentialData withFakeHmac = new ConfidentialData(encodedSenderPublicKey, "hmac".getBytes(), iv, cypherText, signature);
            HybridEncryption.decryptAndVerify(withFakeHmac, keyPairReceiver);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
