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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.StringUtils;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class KeyBundleStore implements PersistableStore<KeyBundleStore> {
    // Secret uid used for deriving keyIds
    // As the keyID is public in the mailbox message we do not want to leak any information of the user identity
    // to the network.
    private String secretUid = StringUtils.createUid();
    private final Map<String, KeyPair> keyPairsById = new ConcurrentHashMap<>();

    public KeyBundleStore() {
    }

    private KeyBundleStore(String secretUid, Map<String, KeyPair> map) {
        this.secretUid = secretUid;
        this.keyPairsById.putAll(map);
    }

    @Override
    public KeyBundleStore getClone() {
        return new KeyBundleStore(secretUid, keyPairsById);
    }

    @Override
    public bisq.security.protobuf.KeyBundleStore toProto() {
        return bisq.security.protobuf.KeyBundleStore.newBuilder().setSecretUid(secretUid).putAllKeyPairsById(keyPairsById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> KeyPairProtoUtil.toProto(entry.getValue()))))
                .build();
    }

    public static KeyBundleStore fromProto(bisq.security.protobuf.KeyBundleStore proto) {
        return new KeyBundleStore(proto.getSecretUid(),
                proto.getKeyPairsByIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> KeyPairProtoUtil.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.security.protobuf.KeyBundleStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(KeyBundleStore persisted) {
        secretUid = persisted.secretUid;
        keyPairsById.clear();
        keyPairsById.putAll(persisted.keyPairsById);
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return Optional.ofNullable(keyPairsById.get(keyId));
    }

    public void put(String keyId, KeyPair keyPair) {
        keyPairsById.put(keyId, keyPair);
    }

    String getSecretUid() {
        return secretUid;
    }
}