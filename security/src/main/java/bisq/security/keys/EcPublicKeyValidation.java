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

import bisq.common.validation.NetworkDataValidation;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Validation of received EC public keys whose encoded bytes identify the key (in a hash, a storage key, or a signature
 * preimage).
 * <p>
 * The canonical encoding of an EC public key is the X.509 encoding which BouncyCastle's getEncoded() creates for a
 * key of the curve secp256k1: DER, named curve, uncompressed point. Bisq 2 creates only canonical keys: all its EC key
 * creation paths ({@link KeyGeneration#generateKeyPair}, {@link KeyGeneration#deriveEcPublicKey},
 * {@link KeyPairUtils#fromPrivateKey}) use the named curve spec and encode the key with getEncoded(). A key in another
 * encoding therefore comes from a modified client or was re-encoded by a third party.
 * <p>
 * The key parser ({@link KeyGeneration#generatePublic(byte[])}) also accepts other encodings of the same key, for
 * example a compressed point or a DER length in long form. Anyone can create them without the private key, so only
 * the canonical encoding must be accepted (see docs/specifications/security/ec-public-key-encoding.md).
 */
public final class EcPublicKeyValidation {
    private EcPublicKeyValidation() {
    }

    /**
     * Checks the size of the encoded key and then that it is in canonical encoding. The size check runs first, so that
     * the more expensive canonical check (see {@link #isCanonical(byte[])}) never parses oversized input.
     */
    public static void validate(byte[] encodedKey) {
        NetworkDataValidation.validateECPubKey(encodedKey);
        checkArgument(isCanonical(encodedKey), "EC public key is not in canonical encoding");
    }

    /**
     * Checks that a key object encodes in canonical encoding. Parsing and getEncoded() do not guarantee this: a key
     * which was parsed from bytes with explicit curve parameters, or a key of another curve, keeps a non-canonical
     * encoding. Use it for key objects whose getEncoded() bytes identify the key (in a hash or a serialized message).
     */
    public static void validate(PublicKey publicKey) {
        validate(publicKey.getEncoded());
    }

    /**
     * Whether the bytes are the canonical encoding of an EC public key of the curve secp256k1.
     * <p>
     * This is not a cheap check. It parses the key (ASN.1 decoding and decoding of the curve point, which includes a
     * point validation and, for a compressed point, a modular square root), creates the key again from the point and
     * encodes it. Use it only where the encoded bytes identify the key, and run a size check before it.
     */
    public static boolean isCanonical(byte[] encodedKey) {
        try {
            if (!(KeyGeneration.generatePublic(encodedKey) instanceof ECPublicKey ecPublicKey)) {
                return false;
            }
            ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(KeyGeneration.CURVE);
            PublicKey canonicalKey = KeyFactory.getInstance(KeyGeneration.EC, "BC")
                    .generatePublic(new ECPublicKeySpec(ecPublicKey.getQ(), params));
            return Arrays.equals(canonicalKey.getEncoded(), encodedKey);
        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        }
    }
}
