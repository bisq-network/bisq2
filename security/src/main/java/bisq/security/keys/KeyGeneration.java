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
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyGeneration {
    public static final String ECDH = "ECDH";
    private static final String CURVE = "secp256k1";
    private static final String ECDSA = "ECDSA";
    public static final String DSA = "DSA";

    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE);
        KeyPairGenerator generator = KeyPairGenerator.getInstance(ECDH, "BC");
        generator.initialize(ecSpec, new SecureRandom());
        return generator.generateKeyPair();
    }

    public static PublicKey generatePublic(byte[] encodedKey) throws GeneralSecurityException {
        return generatePublic(encodedKey, ECDH);
    }

    public static PublicKey generatePublic(byte[] encodedKey, String algorithm) throws GeneralSecurityException {
        EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
    }

    public static PublicKey generatePublicFromCompressed(byte[] compressedKey) throws GeneralSecurityException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
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
}
