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
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KeyBundleService implements PersistenceClient<KeyBundleStore> {
    @Getter
    public static class Config {
        private final Optional<String> defaultTorPrivateKey;
        private final boolean writeDefaultTorPrivateKeyToFile;

        public Config(String defaultTorPrivateKey, boolean writeDefaultTorPrivateKeyToFile) {
            this.defaultTorPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultTorPrivateKey));
            this.writeDefaultTorPrivateKeyToFile = writeDefaultTorPrivateKeyToFile;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getString("defaultTorPrivateKey"),
                    config.getBoolean("writeDefaultTorPrivateKeyToFile"));
        }
    }

    @Getter
    private final KeyBundleStore persistableStore;
    @Getter
    private final Persistence<KeyBundleStore> persistence;
    private final String baseDir;
    private final Optional<String> defaultTorPrivateKey;
    private final boolean writeDefaultTorPrivateKeyToFile;

    public KeyBundleService(PersistenceService persistenceService, Config config) {
        persistableStore = new KeyBundleStore();
        defaultTorPrivateKey = config.getDefaultTorPrivateKey();
        writeDefaultTorPrivateKeyToFile = config.isWriteDefaultTorPrivateKeyToFile();
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);

        baseDir = persistenceService.getBaseDir();

        if (defaultTorPrivateKey.isPresent()) {
            log.warn("defaultTorPrivateKey is provided via the config and will replace the persisted key");
        }
    }

    public CompletableFuture<Boolean> initialize() {
        String defaultKeyId = getDefaultKeyId();
        String tag = "default";
        Path storagePath = Path.of(baseDir, "db", "private", "tor");
        if (defaultTorPrivateKey.isEmpty()) {
            return getOrCreateKeyBundleAsync(defaultKeyId)
                    .thenApply(keyBundle -> {
                        if (keyBundle != null && writeDefaultTorPrivateKeyToFile) {
                            TorKeyUtils.writePrivateKey(keyBundle.getTorKeyPair(), storagePath, tag);
                        }
                        return keyBundle != null;
                    });
        } else {
            // If we get a tor private key passed from the config we always use that and override existing tor keys.
            return CompletableFuture.supplyAsync(() -> {
                        byte[] torPrivateKey = Hex.decode(defaultTorPrivateKey.get());
                        TorKeyPair defaultTorKeyPair = TorKeyGeneration.generateKeyPair(torPrivateKey);
                        KeyBundle keyBundle = findKeyBundle(defaultKeyId)
                                .map(bundle -> new KeyBundle(defaultKeyId, bundle.getKeyPair(), defaultTorKeyPair))
                                .orElseGet(() -> createKeyBundle(defaultKeyId, defaultTorKeyPair));
                        persistKeyBundle(defaultKeyId, keyBundle);

                        // We write the key to the tor directory. This is used only for the default key, as that is the
                        // only supported use case (seed nodes, oracle nodes,...)
                        if (writeDefaultTorPrivateKeyToFile) {
                            TorKeyUtils.writePrivateKey(keyBundle.getTorKeyPair(), storagePath, tag);
                        }
                        return keyBundle;
                    })
                    .thenApply(Objects::nonNull);
        }
    }

    // When a user creates a new profile we generate only the keypair until the user choose to use that.
    // The user can re-create many keyPairs until they are satisfied with the generated nym and cathash image
    public KeyPair generateKeyPair() {
        try {
            return KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            log.error("Error at generateKeyPair", e);
            throw new RuntimeException(e);
        }
    }

    // For the above described use case we get a chosen keyPair to create out bundle and persist it
    public KeyBundle createAndPersistKeyBundle(String identityTag, KeyPair keyPair) {
        String keyId = getKeyIdFromTag(identityTag);
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        // I2pKeyPair i2PKeyPair = I2pKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(keyId, keyPair, torKeyPair/*, i2PKeyPair*/);
        persistKeyBundle(keyId, keyBundle);
        return keyBundle;
    }

    public CompletableFuture<KeyBundle> getOrCreateKeyBundleAsync(String keyId) {
        return findKeyBundle(keyId).map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.supplyAsync(() -> {
                    KeyBundle keyBundle = createKeyBundle(keyId);
                    persistKeyBundle(keyId, keyBundle);
                    return keyBundle;
                }));
    }

    public KeyBundle getOrCreateKeyBundle(String keyId) {
        return findKeyBundle(keyId)
                .orElseGet(() -> {
                    KeyBundle keyBundle = createKeyBundle(keyId);
                    persistKeyBundle(keyId, keyBundle);
                    return keyBundle;
                });
    }

    public Optional<KeyBundle> findDefaultKeyBundle() {
        return findKeyBundle(getDefaultKeyId());
    }

    public Optional<KeyBundle> findKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        return persistableStore.findKeyBundle(keyId);
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return findKeyBundle(keyId).map(KeyBundle::getKeyPair);
    }

    public KeyBundle createKeyBundle(String keyId) {
        return createKeyBundle(keyId, TorKeyGeneration.generateKeyPair());
    }

    public KeyBundle createKeyBundle(String keyId, TorKeyPair torKeyPair) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        try {
            KeyPair keyPair = KeyGeneration.generateKeyPair();
            // I2pKeyPair i2PKeyPair = I2pKeyGeneration.generateKeyPair();
            return new KeyBundle(keyId, keyPair, torKeyPair/*, i2PKeyPair*/);
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
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        persistableStore.putKeyBundle(keyId, keyBundle);
        persist();
    }
}
