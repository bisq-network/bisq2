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
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KeyBundleService implements PersistenceClient<KeyBundleStore> {
    @Getter
    private final KeyBundleStore persistableStore;
    @Getter
    private final Persistence<KeyBundleStore> persistence;

    public KeyBundleService(PersistenceService persistenceService) {
        persistableStore = new KeyBundleStore();
        persistence = persistenceService.getOrCreatePersistence(this,
                "db" + File.separator + "private",
                persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        String defaultKeyId = getDefaultKeyId();
        return getOrCreateKeyBundleAsync(defaultKeyId)
                .thenApply(Objects::nonNull);
    }

    // When a user creates a new profile we generate only the keypair until the user choose to use that.
    // The user can re-create many keyPairs until they are satisfied with the generated nym and robosat image
    public KeyPair generateKeyPair() {
        try {
            return KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            log.error("Error at generateKeyPair", e);
            throw new RuntimeException(e);
        }
    }

    // For the above described use case we get a chosen keyPair to create out bundle and persist it
    public KeyBundle createAndPersistKeyBundle(String keyId, KeyPair keyPair, Optional<byte[]> torPrivateKeyFromTorDir) {
        TorKeyPair torKeyPair = torPrivateKeyFromTorDir.map(TorKeyGeneration::generateKeyPair)
                .orElse(TorKeyGeneration.generateKeyPair());
        // I2pKeyPair i2PKeyPair = I2pKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(keyPair, torKeyPair/*, i2PKeyPair*/);
        persistKeyBundle(keyId, keyBundle);
        return keyBundle;
    }

    public CompletableFuture<KeyBundle> getOrCreateKeyBundleAsync(String keyId) {
        return findKeyBundle(keyId).map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.supplyAsync(() -> getOrCreateKeyBundle(keyId)));
    }

    public KeyBundle getOrCreateKeyBundle(String keyId) {
        return findKeyBundle(keyId)
                .orElseGet(() -> {
                    KeyBundle keyBundle = createKeyBundle(keyId);
                    persistKeyBundle(keyId, keyBundle);
                    return keyBundle;
                });
    }

    public Optional<KeyBundle> findKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        return persistableStore.findKeyBundle(keyId);
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return findKeyBundle(keyId).map(KeyBundle::getKeyPair);
    }

    public KeyBundle createKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        try {
            KeyPair keyPair = KeyGeneration.generateKeyPair();
            TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
            // I2pKeyPair i2PKeyPair = I2pKeyGeneration.generateKeyPair();
            return new KeyBundle(keyPair, torKeyPair/*, i2PKeyPair*/);
        } catch (GeneralSecurityException e) {
            log.error("Error at generateKeyPair", e);
            throw new RuntimeException(e);
        }
    }

    public String getKeyIdFromTag(String tag) {
        String combined = persistableStore.getSecretUid() + tag;
        return Hex.encode(DigestUtil.hash(combined.getBytes(StandardCharsets.UTF_8)));
    }

    public String getDefaultKeyId() {
        return getKeyIdFromTag("default");
    }

    public boolean isDefaultKeyId(String keyId) {
        return getDefaultKeyId().equals(keyId);
    }

    public void persistKeyBundle(String keyId, KeyBundle keyBundle) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash");
        persistableStore.putKeyBundle(keyId, keyBundle);
        persist();
    }
}
