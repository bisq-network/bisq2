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

import bisq.common.encoding.Hex;
import bisq.common.validation.NetworkDataValidation;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyGeneration {
    public static final String ECDH = "ECDH";
    public static final String EC = "EC";
    public static final String DSA = "DSA";

    static final String CURVE = "secp256k1";
    private static final String ECDSA = "ECDSA";

    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyPair generateDefaultEcKeyPair() {
        //TODO should be EC -> test if it would break anything
        return generateKeyPair(CURVE, ECDH);
    }

    /**
     * The public key of the returned key pair is in canonical encoding (see {@link EcPublicKeyValidation}) if the
     * curve is secp256k1.
     */
    public static KeyPair generateKeyPair(String curve, String algorithm) {
        try {
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(curve);
            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, "BC");
            generator.initialize(ecSpec, new SecureRandom());
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to generate " + algorithm + " key pair (curve=" + curve + ", provider=BC)", e);
        }
    }

    public static PublicKey generatePublic(byte[] encodedKey) throws GeneralSecurityException {
        return generatePublic(encodedKey, ECDH);
    }

    /**
     * Parses the X.509 encoding of a public key. Received key bytes can reach this method before any other check of
     * the message which contains them. For EC keys the size is therefore checked before the parser runs: the work of
     * the parser grows with the size of its input, and a key with explicit curve parameters and a large order takes
     * seconds to reject (see docs/specifications/security/ec-public-key-encoding.md). Keys of other algorithms, for
     * example DSA keys of Bisq 1 accounts, have other sizes; their callers check the size.
     */
    public static PublicKey generatePublic(byte[] encodedKey, String algorithm) throws GeneralSecurityException {
        if (isEcAlgorithm(algorithm) && !NetworkDataValidation.isECPubKeySizeValid(encodedKey)) {
            throw new InvalidKeySpecException("EC public key not of the expected size. size=" + encodedKey.length);
        }
        EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
    }

    private static boolean isEcAlgorithm(String algorithm) {
        return algorithm.equals(EC) || algorithm.equals(ECDH) || algorithm.equals(ECDSA);
    }

    public static PublicKey generatePublicFromCompressed(byte[] compressedKey) throws GeneralSecurityException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
        KeyFactory fact = KeyFactory.getInstance(ECDSA, "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, compressedKey);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return fact.generatePublic(keySpec);
    }

    public static PrivateKey generatePrivate(byte[] encodedKey) throws GeneralSecurityException {
        return generatePrivate(encodedKey, ECDH);
    }

    public static PrivateKey generatePrivate(byte[] encodedKey, String algorithm) throws GeneralSecurityException {
        EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
    }

    public static byte[] encodePublicKey(PublicKey publicKey) {
        return new X509EncodedKeySpec(publicKey.getEncoded()).getEncoded();
    }

    public static PublicKey getPublicKeyFromHex(String publicKey) {
        try {
            return KeyGeneration.generatePublic(Hex.decode(publicKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey getPrivateKeyFromHex(String privateKey) {
        try {
            return KeyGeneration.generatePrivate(Hex.decode(privateKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The returned public key is in canonical encoding (see {@link EcPublicKeyValidation}).
     */
    public static PublicKey deriveEcPublicKey(PrivateKey privateKey) throws GeneralSecurityException {
        var ecPrivateKey = (ECPrivateKey) privateKey;
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
        var q = params.getG().multiply(ecPrivateKey.getD());
        var pubSpec = new ECPublicKeySpec(q, params);
        return KeyFactory.getInstance("EC", "BC").generatePublic(pubSpec);
    }
}
