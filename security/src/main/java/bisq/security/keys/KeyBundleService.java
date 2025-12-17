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

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.facades.FacadeProvider;
import bisq.common.timer.Scheduler;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.security.DigestUtil;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KeyBundleService extends RateLimitedPersistenceClient<KeyBundleStore> implements Service {
    private static final String DEFAULT_TAG = "default";

    @Getter
    public static class Config {
        private final Optional<String> keyStoreSecretUid; // Used to recreate the same keyId which is based on a secret UID and the identity tag
        private final Optional<String> defaultPrivateKey;
        private final Optional<String> defaultTorPrivateKey;
        private final Optional<String> defaultI2pIdentityBase64;
        private final boolean writeKeyStoreSecretUidToFile;
        private final boolean writeDefaultPrivateKeyToFile;
        private final boolean writeDefaultTorPrivateKeyToFile;
        private final boolean writeDefaultI2pIdentityBase64ToFile;

        public Config(String keyStoreSecretUid,
                      String defaultPrivateKey,
                      String defaultTorPrivateKey,
                      String defaultI2pIdentityBase64,
                      boolean writeKeyStoreSecretUidToFile,
                      boolean writeDefaultPrivateKeyToFile,
                      boolean writeDefaultTorPrivateKeyToFile,
                      boolean writeDefaultI2pIdentityBase64ToFile) {
            this.keyStoreSecretUid = Optional.ofNullable(Strings.emptyToNull(keyStoreSecretUid));
            this.defaultPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultPrivateKey));
            this.defaultTorPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultTorPrivateKey));
            this.defaultI2pIdentityBase64 = Optional.ofNullable(Strings.emptyToNull(defaultI2pIdentityBase64));
            this.writeKeyStoreSecretUidToFile = writeKeyStoreSecretUidToFile;
            this.writeDefaultPrivateKeyToFile = writeDefaultPrivateKeyToFile;
            this.writeDefaultTorPrivateKeyToFile = writeDefaultTorPrivateKeyToFile;
            this.writeDefaultI2pIdentityBase64ToFile = writeDefaultI2pIdentityBase64ToFile;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getString("keyStoreSecretUid"),
                    config.getString("defaultPrivateKey"),
                    config.getString("defaultTorPrivateKey"),
                    config.getString("defaultI2pIdentityBase64"),
                    config.getBoolean("writeKeyStoreSecretUidToFile"),
                    config.getBoolean("writeDefaultPrivateKeyToFile"),
                    config.getBoolean("writeDefaultTorPrivateKeyToFile"),
                    config.getBoolean("writeDefaultI2pIdentityBase64ToFile")
            );
        }
    }

    @Getter
    private final KeyBundleStore persistableStore;
    @Getter
    private final Persistence<KeyBundleStore> persistence;
    private final Optional<String> keyStoreSecretUid;
    private final Optional<String> defaultPrivateKey;
    private final Optional<String> defaultTorPrivateKey;
    private final Optional<String> defaultI2pIdentityBase64; // <-- Store I2P from config
    private final boolean writeKeyStoreSecretUidToFile;
    private final boolean writeDefaultPrivateKeyToFile;
    private final boolean writeDefaultTorPrivateKeyToFile;
    private final boolean writeDefaultI2pIdentityBase64ToFile;
    private final Path defaultKeyStoragePath, torStoragePath, i2pStoragePath;

    public KeyBundleService(PersistenceService persistenceService, Config config) {
        keyStoreSecretUid = config.getKeyStoreSecretUid();
        defaultPrivateKey = config.getDefaultPrivateKey();
        defaultTorPrivateKey = config.getDefaultTorPrivateKey();
        defaultI2pIdentityBase64 = config.getDefaultI2pIdentityBase64();
        writeKeyStoreSecretUidToFile = config.isWriteKeyStoreSecretUidToFile();
        writeDefaultPrivateKeyToFile = config.isWriteDefaultPrivateKeyToFile();
        writeDefaultTorPrivateKeyToFile = config.isWriteDefaultTorPrivateKeyToFile();
        writeDefaultI2pIdentityBase64ToFile = config.isWriteDefaultI2pIdentityBase64ToFile();

        persistableStore = new KeyBundleStore(keyStoreSecretUid);
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        Path appDataDirPath = persistenceService.getAppDataDirPath();
        defaultKeyStoragePath = appDataDirPath.resolve("db").resolve("private").resolve("key");
        torStoragePath = appDataDirPath.resolve("db").resolve("private").resolve("tor");
        i2pStoragePath = appDataDirPath.resolve("db").resolve("private").resolve("i2p");

        if (defaultPrivateKey.isPresent()) {
            log.warn("defaultPrivateKey is provided via the config and will replace the persisted key");
        }
        if (defaultTorPrivateKey.isPresent()) {
            log.warn("defaultTorPrivateKey is provided via the config and will replace the persisted key");
        }
        if (defaultI2pIdentityBase64.isPresent()) {
            log.warn("defaultI2pIdentityBase64 is provided via the config and will replace the persisted key");
        }
    }

    @Override
    public void onPersistedApplied(KeyBundleStore persisted) {
        // If we have passed the value as jvm argument we override the persisted data.
        keyStoreSecretUid.ifPresent(persistableStore::setSecretUid);
    }

    @Override
    public Optional<KeyBundleStore> readPersisted() {
        Optional<KeyBundleStore> persisted = super.readPersisted();
        if (persisted.isPresent() && persisted.get().hadEmptyI2PKeyPair()) {
            // In pre-2.1.8 versions there was no I2P keypair set.
            // In that case, we create a fresh one and persist .
            Scheduler.run(this::persist).after(500);
        }
        return persisted;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        String defaultKeyId = getDefaultKeyId();
        Optional<KeyBundle> defaultBundle = findDefaultKeyBundle();
        if (defaultTorPrivateKey.isEmpty()
                && defaultI2pIdentityBase64.isEmpty()
                && defaultBundle.isPresent()) {
            // Nothing to update, so we return
            return CompletableFuture.completedFuture(true);
        } else {
            return createOrUpdateDefaultBundle(defaultBundle, defaultKeyId)
                    .thenApply(Objects::nonNull);
        }
    }

    public KeyPair generateKeyPair() {
        return KeyGeneration.generateKeyPair();
    }

    public KeyBundle createAndPersistKeyBundle(String identityTag, KeyPair keyPair) {
        String keyId = getKeyIdFromTag(identityTag);
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        I2PKeyPair i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
        persistKeyBundle(keyId, keyBundle);
        return keyBundle;
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return findKeyBundle(keyId).map(KeyBundle::getKeyPair);
    }

    public KeyBundle getOrCreateKeyBundle(String keyId) {
        return findKeyBundle(keyId)
                .orElseGet(() -> {
                    KeyBundle keyBundle = createKeyBundle(keyId);
                    persistKeyBundle(keyId, keyBundle);
                    return keyBundle;
                });
    }

    public String getKeyIdFromTag(String tag) {
        String combined = persistableStore.getSecretUid() + tag;
        return Hex.encode(DigestUtil.hash(combined.getBytes(StandardCharsets.UTF_8)));
    }

    public String getDefaultKeyId() {
        return getKeyIdFromTag(DEFAULT_TAG);
    }

    public boolean isDefaultKeyId(String keyId) {
        return getDefaultKeyId().equals(keyId);
    }

    private CompletableFuture<KeyBundle> createOrUpdateDefaultBundle(Optional<KeyBundle> existingBundle,
                                                                     String defaultKeyId) {
        return CompletableFuture.supplyAsync(() -> {
            KeyPair defaultKeyPair = defaultPrivateKey.map(KeyPairUtils::fromPrivateKey)
                    .orElseGet(() -> existingBundle.map(KeyBundle::getKeyPair)
                            .orElseGet(KeyGeneration::generateKeyPair));
            if (writeDefaultPrivateKeyToFile) {
                KeyPairUtils.writePrivateKey(defaultKeyPair, defaultKeyStoragePath, DEFAULT_TAG);
            }
            if (writeKeyStoreSecretUidToFile) {
                try {
                    FacadeProvider.getJdkFacade().createDirectories(defaultKeyStoragePath);
                } catch (IOException e) {
                    log.error("Could not create {}", defaultKeyStoragePath);
                    throw new RuntimeException(e);
                }
                Path filePath = defaultKeyStoragePath.resolve("keyStoreSecretUid");
                try {
                    FacadeProvider.getJdkFacade().writeString(persistableStore.getSecretUid(), filePath);
                } catch (IOException e) {
                    log.error("Could not write keyStoreSecretUid to {}", filePath);
                }
            }

            TorKeyPair defaultTorKeyPair = defaultTorPrivateKey.map(TorKeyUtils::fromPrivateKey)
                    .orElseGet(() -> existingBundle.map(KeyBundle::getTorKeyPair)
                            .orElseGet(TorKeyGeneration::generateKeyPair));
            if (writeDefaultTorPrivateKeyToFile) {
                TorKeyUtils.writePrivateKey(defaultTorKeyPair, torStoragePath, DEFAULT_TAG);
            }

            I2PKeyPair defaultI2pKeyPair = defaultI2pIdentityBase64.map(I2PKeyUtils::fromIdentityBase64)
                    .orElseGet(() -> existingBundle.map(KeyBundle::getI2PKeyPair)
                            .orElseGet(I2PKeyGeneration::generateKeyPair));
            if (writeDefaultI2pIdentityBase64ToFile) {
                I2PKeyUtils.writeDestination(defaultI2pKeyPair, i2pStoragePath, DEFAULT_TAG);
            }

            KeyBundle keyBundle = new KeyBundle(defaultKeyId, defaultKeyPair, defaultTorKeyPair, defaultI2pKeyPair);
            persistKeyBundle(defaultKeyId, keyBundle);
            return keyBundle;
        }, commonForkJoinPool());
    }

    private KeyBundle createKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        I2PKeyPair i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        return new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
    }

    public Optional<KeyBundle> findKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        return persistableStore.findKeyBundle(keyId);
    }

    public KeyBundle getKeyBundle(String keyId) {
        Optional<KeyBundle> keyBundle = findKeyBundle(keyId);
        checkArgument(keyBundle.isPresent(), "keyBundle must be present at getKeyBundle. keyId=" + keyId);
        return keyBundle.get();
    }

    public Optional<KeyBundle> findDefaultKeyBundle() {
        return findKeyBundle(getDefaultKeyId());
    }

    private void persistKeyBundle(String keyId, KeyBundle keyBundle) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        persistableStore.putKeyBundle(keyId, keyBundle);
        persist();
    }
}
