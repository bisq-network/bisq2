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

import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
public class KeyPairService implements PersistenceClient<KeyPairStore> {
    public static final String DEFAULT = "default";
    @Getter
    private final KeyPairStore persistableStore = new KeyPairStore();
    @Getter
    private final Persistence<KeyPairStore> persistence;

    public KeyPairService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        return getOrCreateKeyPairAsync(DEFAULT).thenApply(r -> true);
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        synchronized (persistableStore) {
            return persistableStore.findKeyPair(keyId);
        }
    }

    public KeyPair getOrCreateKeyPair(String keyId) {
        try {
            return getOrCreateKeyPairAsync(keyId).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public KeyPair generateKeyPair() {
        try {
            return KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void persistKeyPair(String keyId, KeyPair keyPair) {
        synchronized (persistableStore) {
            persistableStore.put(keyId, keyPair);
        }
        persist();
    }

    public CompletableFuture<KeyPair> getOrCreateKeyPairAsync(String keyId) {
        return findKeyPair(keyId).map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.supplyAsync(() -> {
                    try {
                        KeyPair keyPair = KeyGeneration.generateKeyPair();
                        synchronized (persistableStore) {
                            persistableStore.put(keyId, keyPair);
                        }
                        persist();
                        return keyPair;
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                }));
    }

    public PubKey getDefaultPubKey() {
        String keyId = KeyPairService.DEFAULT;
        PublicKey publicKey = getOrCreateKeyPair(keyId).getPublic();
        return new PubKey(publicKey, keyId);
    }

    public static KeyPair loadDsaKey(String privateKeyPath) throws GeneralSecurityException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        File filePrivateKey = new File(privateKeyPath);
        try (FileInputStream fileInputStream = new FileInputStream(filePrivateKey.getPath())) {
            byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
            //noinspection ResultOfMethodCallIgnored
            fileInputStream.read(encodedPrivateKey);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            DSAPrivateKey privateKey = (DSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            DSAParams dsaParams = privateKey.getParams();
            BigInteger p = dsaParams.getP();
            BigInteger q = dsaParams.getQ();
            BigInteger g = dsaParams.getG();
            BigInteger y = g.modPow(privateKey.getX(), p);
            KeySpec publicKeySpec = new DSAPublicKeySpec(y, p, q, g);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return new KeyPair(publicKey, privateKey);
        }
    }
}
