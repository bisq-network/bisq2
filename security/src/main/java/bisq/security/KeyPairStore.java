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

import bisq.persistence.PersistableStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class KeyPairStore implements PersistableStore<KeyPairStore> {
    @Getter
    private final Map<String, KeyPair> keyPairsById = new ConcurrentHashMap<>();

    public KeyPairStore() {
    }

    private KeyPairStore(Map<String, KeyPair> map) {
        this.keyPairsById.putAll(map);
    }

    @Override
    public KeyPairStore getClone() {
        return new KeyPairStore(keyPairsById);
    }

    @Override
    public bisq.security.protobuf.KeyPairStore toProto() {
        return bisq.security.protobuf.KeyPairStore.newBuilder().putAllKeyPairsById(keyPairsById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> KeyPairProtoUtil.toProto(entry.getValue()))))
                .build();
    }

    public static KeyPairStore fromProto(bisq.security.protobuf.KeyPairStore proto) {
        return new KeyPairStore(proto.getKeyPairsByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> KeyPairProtoUtil.fromProto(e.getValue()))));
    }

    @Override
    public void applyPersisted(KeyPairStore persisted) {
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