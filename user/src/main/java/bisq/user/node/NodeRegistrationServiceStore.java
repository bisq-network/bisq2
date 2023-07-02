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

package bisq.user.node;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public final class NodeRegistrationServiceStore implements PersistableStore<NodeRegistrationServiceStore> {
    private final ObservableSet<AuthorizedNodeRegistrationData> myNodeRegistrations = new ObservableSet<>();

    NodeRegistrationServiceStore() {
    }

    private NodeRegistrationServiceStore(Set<AuthorizedNodeRegistrationData> myNodeRegistrations) {
        this.myNodeRegistrations.setAll(myNodeRegistrations);
    }

    @Override
    public bisq.user.protobuf.NodeRegistrationServiceStore toProto() {
        return bisq.user.protobuf.NodeRegistrationServiceStore.newBuilder()
                .addAllMyNodeRegistrations(myNodeRegistrations.stream()
                        .map(AuthorizedNodeRegistrationData::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static NodeRegistrationServiceStore fromProto(bisq.user.protobuf.NodeRegistrationServiceStore proto) {
        return new NodeRegistrationServiceStore(proto.getMyNodeRegistrationsList().stream()
                .map(AuthorizedNodeRegistrationData::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.NodeRegistrationServiceStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public NodeRegistrationServiceStore getClone() {
        return new NodeRegistrationServiceStore(myNodeRegistrations);
    }

    @Override
    public void applyPersisted(NodeRegistrationServiceStore persisted) {
        myNodeRegistrations.setAll(persisted.getMyNodeRegistrations());
    }
}