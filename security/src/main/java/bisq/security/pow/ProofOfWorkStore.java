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

package bisq.security.pow;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ProofOfWorkStore implements PersistableStore<ProofOfWorkStore> {

    public ProofOfWorkStore() {
    }

    @Override
    public ProofOfWorkStore getClone() {
        return new ProofOfWorkStore();
    }

    @Override
    public bisq.security.protobuf.ProofOfWorkStore toProto() {
        return bisq.security.protobuf.ProofOfWorkStore.newBuilder()
                .build();
    }

    public static ProofOfWorkStore fromProto(bisq.security.protobuf.ProofOfWorkStore proto) {
        return new ProofOfWorkStore();
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.security.protobuf.ProofOfWorkStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ProofOfWorkStore persisted) {
    }
}