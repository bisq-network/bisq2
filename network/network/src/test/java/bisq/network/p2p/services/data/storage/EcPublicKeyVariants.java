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
package bisq.network.p2p.services.data.storage;

import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PublicKey;

/**
 * Creates another valid encoding of an EC public key, as a relay can do without the private key.
 */
public final class EcPublicKeyVariants {
    private EcPublicKeyVariants() {
    }

    // The same key with a compressed point
    public static ByteString toCompressed(PublicKey publicKey) {
        try {
            SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            byte[] compressedPoint = ((ECPublicKey) publicKey).getQ().getEncoded(true);
            return ByteString.copyFrom(new SubjectPublicKeyInfo(keyInfo.getAlgorithm(), compressedPoint)
                    .getEncoded(ASN1Encoding.DER));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The same key with explicit curve parameters instead of the name of the curve. It exceeds the size limit of a key.
    public static ByteString toExplicitParameters(PublicKey publicKey) {
        try {
            byte[] point = ((ECPublicKey) publicKey).getQ().getEncoded(false);
            return ByteString.copyFrom(new SubjectPublicKeyInfo(
                    new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey,
                            new X962Parameters(ECNamedCurveTable.getByName("secp256k1"))),
                    point).getEncoded(ASN1Encoding.DER));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
