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

import bisq.common.proto.UnresolvableProtobufException;
import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PubKeyTest {
    // Chat messages name their author by the ID of the author's public key. The storage compares the hash, so a hash
    // must have only one ID.
    @Test
    void onlyIdsWhichGetIdCreatesAreIds() {
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId");
        String id = pubKey.getId();

        assertTrue(PubKey.isId(id));
        assertArrayEquals(pubKey.getHash(), PubKey.getHashFromId(id));
        assertFalse(PubKey.isId(id.toUpperCase(Locale.ROOT)));
        assertFalse(PubKey.isId(id.substring(1)));
        assertFalse(PubKey.isId("z" + id.substring(1)));
        assertThrows(IllegalArgumentException.class, () -> PubKey.getHashFromId(id.toUpperCase(Locale.ROOT)));
    }

    // A hash has 20 bytes, so an ID has 40 characters. Other lengths are valid hex encodings, but no IDs.
    @Test
    void idsOfOtherLengthsAreNoIds() {
        String id = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId").getId();

        assertFalse(PubKey.isId(""));
        assertFalse(PubKey.isId(id.substring(2)));
        assertFalse(PubKey.isId(id + "00"));
        assertThrows(IllegalArgumentException.class, () -> PubKey.getHashFromId(id.substring(2)));
    }

    // The hash and the serialized form use getEncoded(). Parsing does not make this encoding canonical for a key of
    // another curve, so the key object itself must be checked.
    @Test
    void keyOfAnotherCurveIsRejected() {
        PublicKey otherCurve = KeyGeneration.generateKeyPair("secp256r1", KeyGeneration.EC).getPublic();

        assertThrows(IllegalArgumentException.class, () -> new PubKey(otherCurve, "keyId"));
    }

    // A received key in another encoding of the same point is encoded again in canonical encoding.
    @Test
    void receivedCompressedKeyIsEncodedCanonically() throws IOException, UnresolvableProtobufException {
        PublicKey publicKey = KeyGeneration.generateDefaultEcKeyPair().getPublic();
        byte[] compressedPoint = ((ECPublicKey) publicKey).getQ().getEncoded(true);
        byte[] compressed = new SubjectPublicKeyInfo(
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()).getAlgorithm(),
                compressedPoint).getEncoded(ASN1Encoding.DER);
        bisq.security.protobuf.PubKey proto = bisq.security.protobuf.PubKey.newBuilder()
                .setPublicKey(ByteString.copyFrom(compressed))
                .setKeyId("keyId")
                .build();

        assertArrayEquals(publicKey.getEncoded(), PubKey.fromProto(proto).getPublicKey().getEncoded());
    }

    // A key with explicit curve parameters is a valid encoding of the same key, but it exceeds the size limit of a
    // key. The work of the key parser grows with the size of such a key, so it is rejected before the parser runs.
    @Test
    void receivedOversizedKeyIsRejectedBeforeTheParserRuns() throws IOException {
        PublicKey publicKey = KeyGeneration.generateDefaultEcKeyPair().getPublic();
        byte[] explicitParameters = new SubjectPublicKeyInfo(
                new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey,
                        new X962Parameters(ECNamedCurveTable.getByName("secp256k1"))),
                ((ECPublicKey) publicKey).getQ().getEncoded(false)).getEncoded(ASN1Encoding.DER);
        bisq.security.protobuf.PubKey proto = bisq.security.protobuf.PubKey.newBuilder()
                .setPublicKey(ByteString.copyFrom(explicitParameters))
                .setKeyId("keyId")
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> PubKey.fromProto(proto));

        assertInstanceOf(InvalidKeySpecException.class, exception.getCause());
    }
}
