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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

/**
 * Creates the second valid form (r, n - s) of an ECDSA signature (r, s), as a relay can do without the private key.
 */
public final class EcdsaSignatureVariants {
    private EcdsaSignatureVariants() {
    }

    public static ByteString toOtherS(ByteString signature, PublicKey publicKey) {
        try {
            ASN1Sequence sequence = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(signature.toByteArray()));
            BigInteger r = ASN1Integer.getInstance(sequence.getObjectAt(0)).getValue();
            BigInteger s = ASN1Integer.getInstance(sequence.getObjectAt(1)).getValue();
            BigInteger curveOrder = ((ECPublicKey) publicKey).getParams().getOrder();
            byte[] otherS = new DERSequence(new ASN1Integer[]{new ASN1Integer(r), new ASN1Integer(curveOrder.subtract(s))})
                    .getEncoded();
            return ByteString.copyFrom(otherS);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
