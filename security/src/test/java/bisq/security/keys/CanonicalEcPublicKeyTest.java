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

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The key parser accepts several encodings of the same EC public key. Each variant below parses to the same key as the
 * canonical encoding, so a third party can create it without the private key. Only the canonical encoding is accepted.
 */
class CanonicalEcPublicKeyTest {
    private final KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final byte[] canonical = keyPair.getPublic().getEncoded();
    private final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getQ();
    private final SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(canonical);

    @Test
    void keysOfAllKeyPairsAreCanonical() throws GeneralSecurityException {
        assertTrue(EcPublicKeyValidation.isCanonical(canonical));
        assertTrue(EcPublicKeyValidation.isCanonical(KeyGeneration.generatePublic(canonical).getEncoded()));
        assertTrue(EcPublicKeyValidation.isCanonical(
                KeyGeneration.generateKeyPair("secp256k1", KeyGeneration.EC).getPublic().getEncoded()));
        assertTrue(EcPublicKeyValidation.isCanonical(
                KeyGeneration.deriveEcPublicKey(keyPair.getPrivate()).getEncoded()));
    }

    @Test
    void otherEncodingsOfTheSameKeyAreRejected() throws Exception {
        byte[] compressed = encode(point.getEncoded(true));
        byte[] hybrid = point.getEncoded(false);
        hybrid[0] = (byte) (point.normalize().getAffineYCoord().testBitZero() ? 0x07 : 0x06);
        hybrid = encode(hybrid);
        byte[] longFormLength = new byte[canonical.length + 1];
        longFormLength[0] = 0x30;
        longFormLength[1] = (byte) 0x81;
        System.arraycopy(canonical, 1, longFormLength, 2, canonical.length - 1);
        byte[] indefiniteLength = new byte[canonical.length + 2];
        indefiniteLength[0] = 0x30;
        indefiniteLength[1] = (byte) 0x80;
        System.arraycopy(canonical, 2, indefiniteLength, 2, canonical.length - 2);
        X9ECParameters curve = org.bouncycastle.asn1.x9.ECNamedCurveTable.getByName("secp256k1");
        byte[] explicitParameters = new SubjectPublicKeyInfo(
                new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, new X962Parameters(curve)),
                point.getEncoded(false)).getEncoded(ASN1Encoding.DER);

        for (byte[] variant : new byte[][]{compressed, hybrid, longFormLength, indefiniteLength, explicitParameters}) {
            PublicKey parsed = KeyGeneration.generatePublic(variant);
            assertArrayEquals(point.getEncoded(false), ((ECPublicKey) parsed).getQ().getEncoded(false),
                    "The variant must be a valid encoding of the same key");
            assertFalse(Arrays.equals(canonical, variant));
            assertFalse(EcPublicKeyValidation.isCanonical(variant));
        }
    }

    @Test
    void keysOfOtherCurvesAndInvalidBytesAreRejected() {
        byte[] otherCurve = KeyGeneration.generateKeyPair("secp256r1", KeyGeneration.EC).getPublic().getEncoded();
        assertFalse(EcPublicKeyValidation.isCanonical(otherCurve));
        assertFalse(EcPublicKeyValidation.isCanonical(Arrays.copyOf(canonical, canonical.length + 1)));
        assertFalse(EcPublicKeyValidation.isCanonical(Arrays.copyOf(canonical, canonical.length - 1)));
        assertFalse(EcPublicKeyValidation.isCanonical(new byte[0]));
    }

    private byte[] encode(byte[] encodedPoint) throws IOException {
        return new SubjectPublicKeyInfo(keyInfo.getAlgorithm(), encodedPoint).getEncoded(ASN1Encoding.DER);
    }
}
