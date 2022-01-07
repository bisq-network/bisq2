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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class KeyPairService implements PersistenceClient<HashMap<String, KeyPair>> {
    public static final String DEFAULT = "default";
    @Getter
    private final Persistence<HashMap<String, KeyPair>> persistence;

    private final Map<String, KeyPair> keyPairsById = new ConcurrentHashMap<>();

    public KeyPairService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, "db", "keyPairs");
    }

    @Override
    public void applyPersisted(HashMap<String, KeyPair> persisted) {
        synchronized (keyPairsById) {
            keyPairsById.putAll(persisted);
        }
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        return persistence.persistAsync(getClonedMap());
    }

    @Override
    public HashMap<String, KeyPair> getClonedMap() {
        synchronized (keyPairsById) {
            return new HashMap<>(keyPairsById);
        }
    }

    public CompletableFuture<Boolean> initialize() {
        return getOrCreateKeyPairAsync(DEFAULT).thenApply(r -> true);
    }

    public void shutdown() {
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        synchronized (keyPairsById) {
            return Optional.ofNullable(keyPairsById.get(keyId));
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

    public CompletableFuture<KeyPair> getOrCreateKeyPairAsync(String keyId) {
        return findKeyPair(keyId).map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.supplyAsync(() -> {
                    try {
                        KeyPair keyPair = KeyGeneration.generateKeyPair();
                        synchronized (keyPairsById) {
                            keyPairsById.put(keyId, keyPair);
                        }
                        persist();
                        return keyPair;
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                }));
    }
}
