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

package bisq.support.arbitration.mu_sig;

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
final class MuSigArbitratorStore implements PersistableStore<MuSigArbitratorStore> {
    private final ObservableSet<MuSigArbitrationCase> muSigArbitrationCases = new ObservableSet<>();

    private MuSigArbitratorStore(Set<MuSigArbitrationCase> muSigArbitrationCases) {
        this.muSigArbitrationCases.setAll(muSigArbitrationCases);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitratorStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigArbitratorStore.newBuilder()
                .addAllMuSigArbitrationCases(muSigArbitrationCases.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toSet()));
    }

    @Override
    public bisq.support.protobuf.MuSigArbitratorStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigArbitratorStore fromProto(bisq.support.protobuf.MuSigArbitratorStore proto) {
        return new MuSigArbitratorStore(proto.getMuSigArbitrationCasesList()
                .stream().map(MuSigArbitrationCase::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.MuSigArbitratorStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MuSigArbitratorStore getClone() {
        return new MuSigArbitratorStore(Set.copyOf(muSigArbitrationCases));
    }

    @Override
    public void applyPersisted(MuSigArbitratorStore persisted) {
        muSigArbitrationCases.setAll(persisted.getMuSigArbitrationCases());
    }
}
