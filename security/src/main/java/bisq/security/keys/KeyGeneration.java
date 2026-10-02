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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class KeyGeneration {
    public static final String ECDH = "ECDH";
    public static final String EC = "EC";
    public static final String DSA = "DSA";

    private static final String CURVE = "secp256k1";
    private static final String ECDSA = "ECDSA";

    // Many network entries carry the same public key, and a decoded key is heavy: once used for signature
    // verification it retains BouncyCastle's point precomputation, several KB. We share one instance per encoding.
    // Weak values let keys no entry references be collected, so the size is only a ceiling.
    private static final int MAX_CACHED_PUBLIC_KEYS = 50_000;
    private static final Cache<PublicKeyCacheKey, PublicKey> PUBLIC_KEY_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .maximumSize(MAX_CACHED_PUBLIC_KEYS)
            .build();

    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyPair generateDefaultEcKeyPair() {
        //TODO should be EC -> test if it would break anything
        return generateKeyPair(CURVE, ECDH);
    }

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

    /**
     * WARNING: The returned key is one instance shared by every caller that decodes the same bytes, across all
     * network data and all threads. NEVER mutate it, e.g. with BouncyCastle's ECPointEncoder.setPointFormat().
     * A mutation silently changes getEncoded() for every holder at once: key ids, profile ids, serialized data and
     * the hashes that signatures are computed over all change, so signature verification, ownership checks and
     * deduplication fail for unrelated data that happens to carry the same key.
     */
    public static PublicKey generatePublic(byte[] encodedKey) throws GeneralSecurityException {
        PublicKey cachedPublicKey = PUBLIC_KEY_CACHE.getIfPresent(new PublicKeyCacheKey(encodedKey));
        if (cachedPublicKey != null) {
            return cachedPublicKey;
        }

        // The cache key must not alias the caller's array, as a later mutation would map other bytes to this key
        byte[] encodedKeyCopy = encodedKey.clone();
        PublicKey publicKey = generatePublic(encodedKeyCopy, ECDH);
        // BouncyCastle fills its encoding cache lazily without synchronization. Filling it before the cache publishes
        // the key makes the bytes visible to every thread sharing the instance.
        publicKey.getEncoded();
        PublicKey existingPublicKey = PUBLIC_KEY_CACHE.asMap()
                .putIfAbsent(new PublicKeyCacheKey(encodedKeyCopy), publicKey);
        return existingPublicKey != null ? existingPublicKey : publicKey;
    }

    public static PublicKey generatePublic(byte[] encodedKey, String algorithm) throws GeneralSecurityException {
        EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
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

    public static PublicKey deriveEcPublicKey(PrivateKey privateKey) throws GeneralSecurityException {
        var ecPrivateKey = (ECPrivateKey) privateKey;
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
        var q = params.getG().multiply(ecPrivateKey.getD());
        var pubSpec = new ECPublicKeySpec(q, params);
        return KeyFactory.getInstance("EC", "BC").generatePublic(pubSpec);
    }

    // Encodings come from peers, and colliding Arrays.hashCode values are trivial to build for them, which would turn
    // a cache bucket into a linear scan. A per-process secret hash prevents that. Equality still compares the exact
    // bytes, so a hash collision costs only a comparison and never returns a wrong key.
    @VisibleForTesting
    static final class PublicKeyCacheKey {
        private static final HashFunction HASH_FUNCTION = newSecretHashFunction();

        private final byte[] encodedKey;
        private final int hash;

        PublicKeyCacheKey(byte[] encodedKey) {
            this.encodedKey = encodedKey;
            this.hash = HASH_FUNCTION.hashBytes(encodedKey).asInt();
        }

        private static HashFunction newSecretHashFunction() {
            SecureRandom random = new SecureRandom();
            return Hashing.sipHash24(random.nextLong(), random.nextLong());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PublicKeyCacheKey that)) return false;

            return Arrays.equals(encodedKey, that.encodedKey);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
