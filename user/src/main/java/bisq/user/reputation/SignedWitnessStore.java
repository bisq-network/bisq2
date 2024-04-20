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

package bisq.user.reputation;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Getter
public final class SignedWitnessStore implements PersistableStore<SignedWitnessStore> {
    private final Set<String> jsonRequests = new CopyOnWriteArraySet<>();
    @Setter
    private long lastRequested = 0;

    public SignedWitnessStore() {
    }

    private SignedWitnessStore(Set<String> jsonRequests, long lastRequested) {
        this.lastRequested = lastRequested;
        this.jsonRequests.addAll(jsonRequests);
    }

    @Override
    public bisq.user.protobuf.SignedWitnessStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.SignedWitnessStore.newBuilder()
                .addAllJsonRequests(jsonRequests)
                .setLastRequested(lastRequested);
    }

    @Override
    public bisq.user.protobuf.SignedWitnessStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SignedWitnessStore fromProto(bisq.user.protobuf.SignedWitnessStore proto) {
        return new SignedWitnessStore(new HashSet<>(proto.getJsonRequestsList()), proto.getLastRequested());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.SignedWitnessStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public SignedWitnessStore getClone() {
        return new SignedWitnessStore(new HashSet<>(jsonRequests), lastRequested);
    }

    @Override
    public void applyPersisted(SignedWitnessStore persisted) {
        jsonRequests.clear();
        jsonRequests.addAll(persisted.getJsonRequests());
        lastRequested = persisted.getLastRequested();
    }
}