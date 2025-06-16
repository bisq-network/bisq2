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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        private final Optional<String> defaultI2pPrivateKey; // <-- Added support for default I2P key
        private final boolean writeDefaultTorPrivateKeyToFile;
        private final boolean writeDefaultI2pPrivateKeyToFile;

        public Config(String defaultTorPrivateKey,
                      String defaultI2pPrivateKey,
                      boolean writeDefaultTorPrivateKeyToFile,
                      boolean writeDefaultI2pPrivateKeyToFile) {
            this.defaultTorPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultTorPrivateKey));
            this.defaultI2pPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultI2pPrivateKey));
            this.writeDefaultTorPrivateKeyToFile = writeDefaultTorPrivateKeyToFile;
            this.writeDefaultI2pPrivateKeyToFile = writeDefaultI2pPrivateKeyToFile;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getString("defaultTorPrivateKey"),
                    config.getString("defaultI2pPrivateKey"),
                    config.getBoolean("writeDefaultTorPrivateKeyToFile"),
                    config.getBoolean("writeDefaultI2pPrivateKeyToFile")
            );
        }
    }

    @Getter
    private final KeyBundleStore persistableStore;
    @Getter
    private final Persistence<KeyBundleStore> persistence;
    private final String baseDir;
    private final Optional<String> defaultTorPrivateKey;
    private final Optional<String> defaultI2pPrivateKey; // <-- Store I2P from config
    private final boolean writeDefaultTorPrivateKeyToFile;
    private final boolean writeDefaultI2pPrivateKeyToFile;
    private Path i2pPersistencePath;

    public KeyBundleService(PersistenceService persistenceService, Config config) {
        persistableStore = new KeyBundleStore();
        defaultTorPrivateKey = config.getDefaultTorPrivateKey();
        defaultI2pPrivateKey = config.getDefaultI2pPrivateKey();
        writeDefaultTorPrivateKeyToFile = config.isWriteDefaultTorPrivateKeyToFile();
        writeDefaultI2pPrivateKeyToFile = config.isWriteDefaultI2pPrivateKeyToFile();

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        baseDir = persistenceService.getBaseDir();

        if (defaultTorPrivateKey.isPresent()) {
            log.warn("defaultTorPrivateKey is provided via the config and will replace the persisted key");
        }
        if (defaultI2pPrivateKey.isPresent()) {
            log.warn("defaultI2pPrivateKey is provided via the config and will replace the persisted key");
        }
    }

    public CompletableFuture<Boolean> initialize() {
        String defaultKeyId = getDefaultKeyId();
        String tag = "default";

        Path torStoragePath = Paths.get(baseDir, "db", "private", "tor");
        Path i2pStoragePath = Paths.get(baseDir, "db", "private", "i2p");
        i2pPersistencePath = i2pStoragePath;

        // Tor initialization future
        CompletableFuture<Boolean> torInit;
        if (defaultTorPrivateKey.isEmpty()) {
            torInit = getOrCreateKeyBundleAsync(defaultKeyId).thenApply(keyBundle -> {
                if (keyBundle != null && writeDefaultTorPrivateKeyToFile) {
                    TorKeyUtils.writePrivateKey(keyBundle.getTorKeyPair(), torStoragePath, tag);
                }
                return keyBundle != null;
            });
        } else {
            torInit = CompletableFuture.supplyAsync(() -> {
                byte[] torPrivateKey = Hex.decode(defaultTorPrivateKey.get());
                TorKeyPair defaultTorKeyPair = TorKeyGeneration.generateKeyPair(torPrivateKey);
                KeyBundle keyBundle = findKeyBundle(defaultKeyId).map(bundle -> new KeyBundle(defaultKeyId, bundle.getKeyPair(), defaultTorKeyPair, bundle.getI2PKeyPair())).orElseGet(() -> createKeyBundle(defaultKeyId, defaultTorKeyPair, null));
                persistKeyBundle(defaultKeyId, keyBundle);

                if (writeDefaultTorPrivateKeyToFile) {
                    TorKeyUtils.writePrivateKey(keyBundle.getTorKeyPair(), torStoragePath, tag);
                }
                return keyBundle;
            }).thenApply(Objects::nonNull);
        }

        CompletableFuture<Boolean> i2pInit;
        if (defaultI2pPrivateKey.isEmpty()) {
            i2pInit = getOrCreateKeyBundleAsync(defaultKeyId).thenApply(keyBundle -> {
                if (keyBundle != null && writeDefaultI2pPrivateKeyToFile) {
                    I2PKeyUtils.writePrivateKey(keyBundle.getI2PKeyPair(), i2pStoragePath, tag);
                }
                return keyBundle != null;
            });
        } else {
            i2pInit = CompletableFuture.supplyAsync(() -> {
                I2PKeyPair defaultI2pKeyPair = null;
                try {
                    defaultI2pKeyPair = I2PKeyGeneration.generateKeyPair(i2pStoragePath, tag);
                } catch (IOException | GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }

                I2PKeyPair finalDefaultI2pKeyPair = defaultI2pKeyPair;
                KeyBundle keyBundle = findKeyBundle(defaultKeyId).map(bundle -> new KeyBundle(defaultKeyId, bundle.getKeyPair(), bundle.getTorKeyPair(), finalDefaultI2pKeyPair)).orElseGet(() -> createKeyBundle(defaultKeyId, null, finalDefaultI2pKeyPair));

                persistKeyBundle(defaultKeyId, keyBundle);

                if (writeDefaultI2pPrivateKeyToFile) {
                    I2PKeyUtils.writePrivateKey(keyBundle.getI2PKeyPair(), i2pStoragePath, tag);
                }
                return keyBundle;
            }).thenApply(Objects::nonNull);
        }

        // Run both in parallel, return true if any one succeeded
        return torInit.thenCombine(i2pInit, (torSuccess, i2pSuccess) -> torSuccess || i2pSuccess);
    }

    public KeyPair generateKeyPair() {
        try {
            return KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            log.error("Error at generateKeyPair", e);
            throw new RuntimeException(e);
        }
    }

    public KeyBundle createAndPersistKeyBundle(String identityTag, KeyPair keyPair) throws GeneralSecurityException {
        String keyId = getKeyIdFromTag(identityTag);
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        I2PKeyPair i2pKeyPair = null;
        try {
            i2pKeyPair = I2PKeyGeneration.generateKeyPair(i2pPersistencePath, identityTag);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        KeyBundle keyBundle = new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
        persistKeyBundle(keyId, keyBundle);
        return keyBundle;
    }

    public CompletableFuture<KeyBundle> getOrCreateKeyBundleAsync(String keyId) {
        return findKeyBundle(keyId).map(CompletableFuture::completedFuture).orElseGet(() -> CompletableFuture.supplyAsync(() -> {
            KeyBundle keyBundle = null;
            try {
                keyBundle = createKeyBundle(keyId);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
            persistKeyBundle(keyId, keyBundle);
            return keyBundle;
        }));
    }

    public KeyBundle getOrCreateKeyBundle(String keyId) {
        return findKeyBundle(keyId).orElseGet(() -> {
            KeyBundle keyBundle = null;
            try {
                keyBundle = createKeyBundle(keyId);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
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

    public KeyBundle createKeyBundle(String keyId) throws GeneralSecurityException {
        try {
            return createKeyBundle(keyId, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair(i2pPersistencePath, "default"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // <-- Refactored to allow full control over all key types
    public KeyBundle createKeyBundle(String keyId, TorKeyPair torKeyPair, I2PKeyPair i2pKeyPair) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        try {
            KeyPair keyPair = KeyGeneration.generateKeyPair();
            return new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
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
