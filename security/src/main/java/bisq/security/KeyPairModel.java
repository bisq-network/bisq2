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

import bisq.persistence.Persistable;
import lombok.Getter;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class KeyPairModel implements Persistable<KeyPairModel> {

    @Getter
    private final Map<String, KeyPair> keyPairsById = new ConcurrentHashMap<>();

    public KeyPairModel() {
    }

    private KeyPairModel(Map<String, KeyPair> map) {
        this.keyPairsById.putAll(map);
    }

    @Override
    public KeyPairModel getClone() {
        return new KeyPairModel(keyPairsById);
    }

    @Override
    public void applyPersisted(KeyPairModel persisted) {
        keyPairsById.clear();
        keyPairsById.putAll(persisted.getKeyPairsById());
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return Optional.ofNullable(keyPairsById.get(keyId));
    }

    public void put(String keyId, KeyPair keyPair) {
        keyPairsById.put(keyId, keyPair);
    }
}