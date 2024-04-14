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

package bisq.support.mediation;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
final class MediatorStore implements PersistableStore<MediatorStore> {
    private final ObservableSet<MediationCase> mediationCases = new ObservableSet<>();

    MediatorStore() {
    }

    private MediatorStore(Set<MediationCase> mediationCases) {
        this.mediationCases.setAll(mediationCases);
    }

    @Override
    public bisq.support.protobuf.MediatorStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MediatorStore.newBuilder()
                .addAllMediationCases(mediationCases.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toSet()));
    }

    @Override
    public bisq.support.protobuf.MediatorStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MediatorStore fromProto(bisq.support.protobuf.MediatorStore proto) {
        return new MediatorStore(proto.getMediationCasesList()
                .stream().map(MediationCase::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.MediatorStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MediatorStore getClone() {
        return new MediatorStore(new HashSet<>(mediationCases));
    }

    @Override
    public void applyPersisted(MediatorStore persisted) {
        mediationCases.setAll(persisted.getMediationCases());
    }
}