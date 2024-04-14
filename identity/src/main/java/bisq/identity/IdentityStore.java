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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public final class IdentityStore implements PersistableStore<IdentityStore> {
    // Is only empty before we get initialize called the first time
    private Optional<Identity> defaultIdentity = Optional.empty();
    private final Map<String, Identity> activeIdentityByTag = new ConcurrentHashMap<>();
    private final Set<Identity> retired = new CopyOnWriteArraySet<>();

    public IdentityStore() {
    }

    private IdentityStore(Optional<Identity> defaultIdentity,
                          Map<String, Identity> activeIdentityByTag,
                          Set<Identity> retired) {
        this.defaultIdentity = defaultIdentity;
        this.activeIdentityByTag.putAll(activeIdentityByTag);
        this.retired.addAll(retired);
    }

    @Override
    public bisq.identity.protobuf.IdentityStore.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.identity.protobuf.IdentityStore.newBuilder()
                .putAllActiveIdentityByDomainId(activeIdentityByTag.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toProto(serializeForHash))))
                .addAllRetired(retired.stream()
                        .map(identity -> identity.toProto(serializeForHash))
                        .collect(Collectors.toSet()));

        defaultIdentity.ifPresent(identity -> builder.setDefaultIdentity(identity.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.identity.protobuf.IdentityStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static IdentityStore fromProto(bisq.identity.protobuf.IdentityStore proto) {
        return new IdentityStore(Optional.of(Identity.fromProto(proto.getDefaultIdentity())),
                proto.getActiveIdentityByDomainIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> Identity.fromProto(e.getValue()))),
                proto.getRetiredList().stream().map(Identity::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.identity.protobuf.IdentityStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public IdentityStore getClone() {
        return new IdentityStore(defaultIdentity, new HashMap<>(activeIdentityByTag), new HashSet<>(retired));
    }

    @Override
    public void applyPersisted(IdentityStore persisted) {
        defaultIdentity = persisted.defaultIdentity;

        activeIdentityByTag.clear();
        activeIdentityByTag.putAll(persisted.getActiveIdentityByTag());

        retired.clear();
        retired.addAll(persisted.getRetired());
    }

    Map<String, Identity> getActiveIdentityByTag() {
        return activeIdentityByTag;
    }

    Set<Identity> getRetired() {
        return retired;
    }

    Optional<Identity> getDefaultIdentity() {
        return defaultIdentity;
    }

    void setDefaultIdentity(Identity defaultIdentity) {
        this.defaultIdentity = Optional.ofNullable(defaultIdentity);
    }
}