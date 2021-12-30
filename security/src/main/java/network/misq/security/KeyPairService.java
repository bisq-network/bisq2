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

package network.misq.security;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;

public class KeyPairService {
    public static final String DEFAULT = "default";
    private final String baseDir;

    public static record Conf(String baseDir) {
    }

    // Key is an arbitrary keyId, but usually associated with interaction like offer ID. 
    // Throws when attempting to use an already existing keyId at add method.
    private final Map<String, KeyPair> keyPairsById = new ConcurrentHashMap<>();

    public KeyPairService(Conf conf) {
        baseDir = conf.baseDir;
    }

    public CompletableFuture<Boolean> initialize() {
        return getOrCreateKeyPairAsync(DEFAULT).thenApply(r -> true);
    }

    public void shutdown() {
    }


    public Optional<KeyPair> findKeyPair(String keyId) {
        return Optional.ofNullable(keyPairsById.get(keyId));
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
                        put(keyId, keyPair);
                        return keyPair;
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                }));
    }

    private void put(String keyId, KeyPair keyPair) {
        checkArgument(!keyPairsById.containsKey(keyId));
        keyPairsById.put(keyId, keyPair);
        persist();
    }

    private void persist() {
        // todo persist
    }
}
