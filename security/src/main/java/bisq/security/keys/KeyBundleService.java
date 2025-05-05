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
import java.nio.file.Files;
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

        public Config(String defaultTorPrivateKey, String defaultI2pPrivateKey, boolean writeDefaultTorPrivateKeyToFile) {
            this.defaultTorPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultTorPrivateKey));
            this.defaultI2pPrivateKey = Optional.ofNullable(Strings.emptyToNull(defaultI2pPrivateKey));
            this.writeDefaultTorPrivateKeyToFile = writeDefaultTorPrivateKeyToFile;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getString("defaultTorPrivateKey"),
                    config.getString("defaultI2pPrivateKey"),
                    config.getBoolean("writeDefaultTorPrivateKeyToFile")
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

    public KeyBundleService(PersistenceService persistenceService, Config config) {
        persistableStore = new KeyBundleStore();
        defaultTorPrivateKey = config.getDefaultTorPrivateKey();
        defaultI2pPrivateKey = config.getDefaultI2pPrivateKey();
        writeDefaultTorPrivateKeyToFile = config.isWriteDefaultTorPrivateKeyToFile();

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

        if (defaultTorPrivateKey.isPresent()) {
            return CompletableFuture.supplyAsync(() -> {
                byte[] torPrivateKeyBytes = Hex.decode(defaultTorPrivateKey.get());
                TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair(torPrivateKeyBytes);

                I2pKeyPair i2pKeyPair = defaultI2pPrivateKey.map(Hex::decode)
                        .map(bytes -> I2pKeyGeneration.generateKeyPair())
                        .orElse(I2pKeyGeneration.generateKeyPair());

                KeyPair identityKeyPair = null;
                try {
                    identityKeyPair = KeyGeneration.generateKeyPair();
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
                KeyBundle keyBundle = new KeyBundle(defaultKeyId, identityKeyPair, torKeyPair, i2pKeyPair);

                persistKeyBundle(defaultKeyId, keyBundle);

                if (writeDefaultTorPrivateKeyToFile) {
                    TorKeyUtils.writePrivateKey(torKeyPair, torStoragePath, tag);
                }
                I2PKeyUtils.writePrivateKey(i2pKeyPair, i2pStoragePath, tag);

                return true;
            });
        } else {
            return getOrCreateKeyBundleAsync(defaultKeyId)
                    .thenApply(keyBundle -> {
                        if (keyBundle != null) {
                            if (writeDefaultTorPrivateKeyToFile) {
                                TorKeyUtils.writePrivateKey(keyBundle.getTorKeyPair(), torStoragePath, tag);
                            }
                            I2PKeyUtils.writePrivateKey(keyBundle.getI2PKeyPair(), i2pStoragePath, tag);
                            return true;
                        }
                        return false;
                    });
        }
    }

    public KeyPair generateKeyPair() {
        try {
            return KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            log.error("Error at generateKeyPair", e);
            throw new RuntimeException(e);
        }
    }

    public KeyBundle createAndPersistKeyBundle(String identityTag, KeyPair keyPair) {
        String keyId = getKeyIdFromTag(identityTag);
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        I2pKeyPair i2pKeyPair = I2pKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(keyId, keyPair, torKeyPair, i2pKeyPair);
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
        return createKeyBundle(keyId, TorKeyGeneration.generateKeyPair(), I2pKeyGeneration.generateKeyPair());
    }

    // <-- Refactored to allow full control over all key types
    public KeyBundle createKeyBundle(String keyId, TorKeyPair torKeyPair, I2pKeyPair i2pKeyPair) {
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
