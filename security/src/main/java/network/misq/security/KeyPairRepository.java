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
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

public class KeyPairRepository {
    public static final String DEFAULT = "default";
    private final String baseDirPath;

    public static record Options(String baseDirPath) {
    }

    // Key is an arbitrary tag, but usually associated with interaction like offer ID. 
    // Throws when attempting to use an already existing tag at add method.
    private final Map<String, KeyPair> keyPairsByTag = new ConcurrentHashMap<>();

    public KeyPairRepository(Options options) {
        baseDirPath = options.baseDirPath;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            KeyPair keyPair = KeyGeneration.generateKeyPair();
            add(keyPair, DEFAULT);
            future.complete(true);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    public void shutdown() {
    }

    public void add(KeyPair keyPair, String tag) {
        checkArgument(!keyPairsByTag.containsKey(tag));
        keyPairsByTag.put(tag, keyPair);
        persist();
    }

    private void persist() {
        // todo persist
    }

    public Optional<KeyPair> findKeyPair(String tag) {
        if (keyPairsByTag.containsKey(tag)) {
            return Optional.of(keyPairsByTag.get(tag));
        } else {
            return Optional.empty();
        }
    }
}
