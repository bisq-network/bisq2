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

import bisq.common.validation.NetworkDataValidation;
import bisq.security.keys.KeyGeneration;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class SignatureTest {

    @Test
    public void testSignature() {
        byte[] message = "hello".getBytes();
        try {
            KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());
            boolean result = SignatureUtil.verify(message, signature, keyPair.getPublic());
            assertTrue(result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail();
        }
    }

    // BouncyCastle does not normalize S, so about half of its signatures are high-S. 64 low-S signatures in a row
    // happen by chance with a probability of 2^-64.
    @Test
    public void signCreatesLowSSignatures() throws GeneralSecurityException, IOException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        BigInteger halfOrder = curveOrder(keyPair).shiftRight(1);
        for (int i = 0; i < 64; i++) {
            byte[] message = ("message " + i).getBytes();
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());

            assertTrue(decode(signature)[1].compareTo(halfOrder) <= 0);
            assertTrue(SignatureUtil.verify(message, signature, keyPair.getPublic()));
        }
    }

    // Nodes of all versions reject an ECDSA signature with fewer than 68 bytes (NetworkDataValidation). The provider
    // creates one for about 1 of 8 million low-S signatures, so the test gives the lengths.
    @Test
    public void signsAgainIfTheSignatureIsTooShort() throws GeneralSecurityException {
        byte[] tooShort = new byte[NetworkDataValidation.MIN_EC_SIGNATURE_LENGTH - 1];
        byte[] accepted = new byte[NetworkDataValidation.MIN_EC_SIGNATURE_LENGTH];
        Iterator<byte[]> signatures = List.of(tooShort, tooShort, accepted).iterator();

        assertSame(accepted, SignatureUtil.signWithAcceptedLength(signatures::next));
    }

    @Test
    public void failsIfNoSignatureHasTheRequiredLength() {
        byte[] tooShort = new byte[NetworkDataValidation.MIN_EC_SIGNATURE_LENGTH - 1];

        assertThrows(SignatureException.class, () -> SignatureUtil.signWithAcceptedLength(() -> tooShort));
    }

    @Test
    public void createdSignaturesHaveAnAcceptedLength() throws GeneralSecurityException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        for (int i = 0; i < 64; i++) {
            NetworkDataValidation.validateECSignature(SignatureUtil.sign(("message " + i).getBytes(), keyPair.getPrivate()));
        }
    }

    @Test
    public void highSVariantIsAcceptedByVerifyOnly() throws GeneralSecurityException, IOException {
        byte[] message = "hello".getBytes();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());
        byte[] highS = toHighS(signature, curveOrder(keyPair));

        assertTrue(SignatureUtil.verifyLowS(message, signature, keyPair.getPublic()));
        // Signatures of earlier versions are high-S in about half of all cases. verify must keep accepting them.
        assertTrue(SignatureUtil.verify(message, highS, keyPair.getPublic()));
        assertFalse(SignatureUtil.verifyLowS(message, highS, keyPair.getPublic()));
    }

    @Test
    public void verifyLowSRejectsInvalidSignatures() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());

        assertInvalid("other".getBytes(), signature, keyPair);
        assertInvalid(message, new byte[0], keyPair);
        assertInvalid(message, new byte[]{0x30, 0x00}, keyPair);
        assertInvalid(message, new byte[]{0x02, 0x01, 0x01}, keyPair);
    }

    // BouncyCastle rejects other encodings of the same (r, s). verifyLowS relies on this for a unique encoding.
    @Test
    public void verifyLowSRejectsNonDerEncodings() throws GeneralSecurityException, IOException {
        byte[] message = "hello".getBytes();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());

        byte[] withTrailingByte = new byte[signature.length + 1];
        System.arraycopy(signature, 0, withTrailingByte, 0, signature.length);
        assertInvalid(message, withTrailingByte, keyPair);

        // Long form of the sequence length: 0x30 0x81 <length> instead of 0x30 <length>
        byte[] berLength = new byte[signature.length + 1];
        berLength[0] = 0x30;
        berLength[1] = (byte) 0x81;
        System.arraycopy(signature, 1, berLength, 2, signature.length - 1);
        assertInvalid(message, berLength, keyPair);

        // r with an unnecessary leading zero byte
        BigInteger[] rs = decode(signature);
        byte[] r = rs[0].toByteArray();
        byte[] paddedR = new byte[r.length + 1];
        System.arraycopy(r, 0, paddedR, 1, r.length);
        byte[] s = rs[1].toByteArray();
        byte[] nonMinimal = new byte[2 + 2 + paddedR.length + 2 + s.length];
        nonMinimal[0] = 0x30;
        nonMinimal[1] = (byte) (nonMinimal.length - 2);
        nonMinimal[2] = 0x02;
        nonMinimal[3] = (byte) paddedR.length;
        System.arraycopy(paddedR, 0, nonMinimal, 4, paddedR.length);
        nonMinimal[4 + paddedR.length] = 0x02;
        nonMinimal[5 + paddedR.length] = (byte) s.length;
        System.arraycopy(s, 0, nonMinimal, 6 + paddedR.length, s.length);
        assertInvalid(message, nonMinimal, keyPair);
    }

    @Test
    public void dsaSignaturesAreNotChanged() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyGeneration.DSA);
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), SignatureUtil.SHA256withDSA);

        assertTrue(SignatureUtil.verify(message, signature, keyPair.getPublic(), SignatureUtil.SHA256withDSA));
        assertThrows(GeneralSecurityException.class,
                () -> SignatureUtil.verifyLowS(message, signature, keyPair.getPublic()));
    }

    // Aliases of SHA256withECDSA, and other ECDSA variants, could otherwise create high-S signatures.
    @Test
    public void signRejectsUnsupportedAlgorithmNames() {
        byte[] message = "hello".getBytes();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        for (String algorithm : new String[]{"SHA256WITHECDSA", "sha256withecdsa", "1.2.840.10045.4.3.2",
                "OID.1.2.840.10045.4.3.2", "ECDSA", "NONEwithECDSA", "SHA256withECDDSA", "SHA256withPLAIN-ECDSA",
                "SHA256withCVC-ECDSA", "SHA512withECDSA"}) {
            assertThrows(NoSuchAlgorithmException.class,
                    () -> SignatureUtil.sign(message, keyPair.getPrivate(), algorithm), algorithm);
        }
    }

    @Test
    public void signRejectsKeyWhichDoesNotMatchAlgorithm() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyGeneration.DSA);
        generator.initialize(2048);
        KeyPair dsaKeyPair = generator.generateKeyPair();
        assertThrows(InvalidKeyException.class,
                () -> SignatureUtil.sign(message, dsaKeyPair.getPrivate(), SignatureUtil.SHA256withECDSA));
    }

    // Both methods report an invalid signature with false, or with a SignatureException for an invalid encoding.
    private static void assertInvalid(byte[] message, byte[] signature, KeyPair keyPair) throws GeneralSecurityException {
        try {
            assertFalse(SignatureUtil.verify(message, signature, keyPair.getPublic()));
        } catch (SignatureException expected) {
        }
        try {
            assertFalse(SignatureUtil.verifyLowS(message, signature, keyPair.getPublic()));
        } catch (SignatureException expected) {
        }
    }

    private static BigInteger curveOrder(KeyPair keyPair) {
        return ((ECPublicKey) keyPair.getPublic()).getParams().getOrder();
    }

    private static BigInteger[] decode(byte[] signature) throws IOException {
        ASN1Sequence sequence = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(signature));
        return new BigInteger[]{
                ASN1Integer.getInstance(sequence.getObjectAt(0)).getValue(),
                ASN1Integer.getInstance(sequence.getObjectAt(1)).getValue()
        };
    }

    private static byte[] toHighS(byte[] signature, BigInteger curveOrder) throws IOException {
        BigInteger[] rs = decode(signature);
        return new DERSequence(new ASN1Integer[]{new ASN1Integer(rs[0]), new ASN1Integer(curveOrder.subtract(rs[1]))})
                .getEncoded();
    }
}
