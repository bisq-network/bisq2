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

package bisq.support.mediation.mu_sig;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class MuSigMediatorStore implements PersistableStore<MuSigMediatorStore> {
    private final ObservableSet<MuSigMediationCase> muSigMediationCases = new ObservableSet<>();

    private MuSigMediatorStore(Set<MuSigMediationCase> muSigMediationCases) {
        this.muSigMediationCases.setAll(muSigMediationCases);
    }

    @Override
    public bisq.support.protobuf.MuSigMediatorStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigMediatorStore.newBuilder()
                .addAllMuSigMediationCases(muSigMediationCases.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toSet()));
    }

    @Override
    public bisq.support.protobuf.MuSigMediatorStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigMediatorStore fromProto(bisq.support.protobuf.MuSigMediatorStore proto) {
        return new MuSigMediatorStore(proto.getMuSigMediationCasesList()
                .stream().map(MuSigMediationCase::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.MuSigMediatorStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MuSigMediatorStore getClone() {
        return new MuSigMediatorStore(Set.copyOf(muSigMediationCases));
    }

    @Override
    public void applyPersisted(MuSigMediatorStore persisted) {
        muSigMediationCases.setAll(persisted.getMuSigMediationCases());
    }
}