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

package bisq.identity;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public final class IdentityStore implements PersistableStore<IdentityStore> {
    private final Map<String, Identity> activeIdentityByTag = new ConcurrentHashMap<>();
    private final Set<Identity> pool = new CopyOnWriteArraySet<>();
    private final Set<Identity> retired = new CopyOnWriteArraySet<>();

    public IdentityStore() {
    }

    private IdentityStore(Map<String, Identity> activeIdentityByTag,
                          Set<Identity> pool,
                          Set<Identity> retired) {
        this.activeIdentityByTag.putAll(activeIdentityByTag);
        this.pool.addAll(pool);
        this.retired.addAll(retired);
    }

    @Override
    public bisq.user.protobuf.IdentityStore toProto() {
        return bisq.user.protobuf.IdentityStore.newBuilder()
                .putAllActiveIdentityByDomainId(activeIdentityByTag.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toProto())))
                .addAllPool(pool.stream().map(Identity::toProto).collect(Collectors.toSet()))
                .addAllRetired(retired.stream().map(Identity::toProto).collect(Collectors.toSet()))
                .build();
    }

    public static IdentityStore fromProto(bisq.user.protobuf.IdentityStore proto) {
        return new IdentityStore(proto.getActiveIdentityByDomainIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Identity.fromProto(e.getValue()))),
                proto.getPoolList().stream().map(Identity::fromProto).collect(Collectors.toSet()),
                proto.getRetiredList().stream().map(Identity::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.IdentityStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public IdentityStore getClone() {
        return new IdentityStore(activeIdentityByTag, pool, retired);
    }

    @Override
    public void applyPersisted(IdentityStore persisted) {
        activeIdentityByTag.clear();
        activeIdentityByTag.putAll(persisted.getActiveIdentityByTag());

        pool.clear();
        pool.addAll(persisted.getPool());

        retired.clear();
        retired.addAll(persisted.getRetired());
    }

    Map<String, Identity> getActiveIdentityByTag() {
        return activeIdentityByTag;
    }

    Set<Identity> getPool() {
        return pool;
    }

    Set<Identity> getRetired() {
        return retired;
    }

}