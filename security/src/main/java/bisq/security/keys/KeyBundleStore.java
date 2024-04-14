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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.StringUtils;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class KeyBundleStore implements PersistableStore<KeyBundleStore> {
    // Secret uid used for deriving keyIds
    // As the keyID is public in the mailbox message we do not want to leak any information of the user identity
    // to the network.
    // Once we have persisted the stores we use the secretUid from the persisted data
    private String secretUid = StringUtils.createUid();
    private final Map<String, KeyBundle> keyBundleById = new ConcurrentHashMap<>();

    public KeyBundleStore() {
    }

    private KeyBundleStore(String secretUid,
                           Map<String, KeyBundle> keyBundleById) {
        this.secretUid = secretUid;
        this.keyBundleById.putAll(keyBundleById);
    }

    @Override
    public KeyBundleStore getClone() {
        return new KeyBundleStore(secretUid, new HashMap<>(keyBundleById));
    }

    @Override
    public bisq.security.protobuf.KeyBundleStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.KeyBundleStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.KeyBundleStore.newBuilder()
                .setSecretUid(secretUid)
                .putAllKeyBundleById(keyBundleById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue().toProto(serializeForHash))));
    }

    public static KeyBundleStore fromProto(bisq.security.protobuf.KeyBundleStore proto) {
        return new KeyBundleStore(proto.getSecretUid(),
                proto.getKeyBundleByIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> KeyBundle.fromProto(e.getValue()))));
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
        keyBundleById.clear();
        keyBundleById.putAll(persisted.keyBundleById);
    }

    public Optional<KeyBundle> findKeyBundle(String keyId) {
        synchronized (keyBundleById) {
            return Optional.ofNullable(keyBundleById.get(keyId));
        }
    }

    public void putKeyBundle(String keyId, KeyBundle keyBundle) {
        synchronized (keyBundleById) {
            if (keyBundleById.put(keyId, keyBundle) != null) {
                log.warn("We had already an entry for key ID {}", keyId);
            }
        }
    }

    String getSecretUid() {
        return secretUid;
    }
}