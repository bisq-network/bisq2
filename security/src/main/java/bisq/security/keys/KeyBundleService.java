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
import bisq.common.timer.Scheduler;
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
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class KeyBundleService implements PersistenceClient<KeyBundleStore>, Service {
    private static final String DEFAULT_TAG = "default";

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
    private final Optional<String> defaultTorPrivateKey;
    private final Optional<String> defaultI2pPrivateKey; // <-- Store I2P from config
    private final boolean writeDefaultTorPrivateKeyToFile;
    private final boolean writeDefaultI2pPrivateKeyToFile;
    private final Path torStoragePath, i2pStoragePath;

    public KeyBundleService(PersistenceService persistenceService, Config config) {
        persistableStore = new KeyBundleStore();
        defaultTorPrivateKey = config.getDefaultTorPrivateKey();
        defaultI2pPrivateKey = config.getDefaultI2pPrivateKey();
        writeDefaultTorPrivateKeyToFile = config.isWriteDefaultTorPrivateKeyToFile();
        writeDefaultI2pPrivateKeyToFile = config.isWriteDefaultI2pPrivateKeyToFile();

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        String baseDir = persistenceService.getBaseDir();
        torStoragePath = Paths.get(baseDir, "db", "private", "tor");
        i2pStoragePath = Paths.get(baseDir, "db", "private", "i2p");

        if (defaultTorPrivateKey.isPresent()) {
            log.warn("defaultTorPrivateKey is provided via the config and will replace the persisted key");
        }
        if (defaultI2pPrivateKey.isPresent()) {
            log.warn("defaultI2pPrivateKey is provided via the config and will replace the persisted key");
        }
    }

    @Override
    public CompletableFuture<Optional<KeyBundleStore>> readPersisted() {
        return PersistenceClient.super.readPersisted()
                .whenComplete((persisted, throwable) -> {
                    // In pre-2.1.8 versions there was no I2P keypair set.
                    // In that case, we create a fresh one and persist .
                    if (persisted != null && persisted.isPresent() && persisted.get().hadEmptyI2PKeyPair()) {
                        Scheduler.run(this::persist).after(500);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        String defaultKeyId = getDefaultKeyId();
        Optional<KeyBundle> defaultBundle = findDefaultKeyBundle();
        if (defaultTorPrivateKey.isEmpty()
                && defaultI2pPrivateKey.isEmpty()
                && defaultBundle.isPresent()) {
            // Nothing to update, so we return
            return CompletableFuture.supplyAsync(() -> true);
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
            KeyPair keyPair = existingBundle.map(KeyBundle::getKeyPair)
                    .orElseGet(KeyGeneration::generateKeyPair);

            TorKeyPair defaultTorKeyPair = defaultTorPrivateKey.map(TorKeyUtils::fromPrivateKey)
                    .orElseGet(() -> existingBundle.map(KeyBundle::getTorKeyPair)
                            .orElseGet(TorKeyGeneration::generateKeyPair));
            if (writeDefaultTorPrivateKeyToFile) {
                TorKeyUtils.writePrivateKey(defaultTorKeyPair, torStoragePath, DEFAULT_TAG);
            }

            I2PKeyPair defaultI2pKeyPair = defaultI2pPrivateKey.map(I2PKeyUtils::fromDestinationBase64)
                    .orElseGet(() -> existingBundle.map(KeyBundle::getI2PKeyPair)
                            .orElseGet(I2PKeyGeneration::generateKeyPair));
            if (writeDefaultI2pPrivateKeyToFile) {
                I2PKeyUtils.writeDestination(defaultI2pKeyPair, i2pStoragePath, DEFAULT_TAG);
            }

            KeyBundle keyBundle = new KeyBundle(defaultKeyId, keyPair, defaultTorKeyPair, defaultI2pKeyPair);
            persistKeyBundle(defaultKeyId, keyBundle);
            return keyBundle;
        });
    }

    private KeyBundle createKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        I2PKeyPair i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        return new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
    }

    private Optional<KeyBundle> findKeyBundle(String keyId) {
        checkArgument(keyId.length() == 40, "Key ID is expected to be a 20 byte hash. keyId=" + keyId);
        return persistableStore.findKeyBundle(keyId);
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
