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

package bisq.oracle_node.bisq1_bridge;

import bisq.bonded_roles.node.bisq1_bridge.requests.AuthorizeAccountAgeRequest;
import bisq.bonded_roles.node.bisq1_bridge.requests.AuthorizeSignedWitnessRequest;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * We persist the requests so that in case of a scam or trade rule violation we could block the user at Bisq 1 as well.
 * This is a bit of a trade-off between security and privacy. One option to improve that would be that all data is
 * persisted as encrypted entries and the decryption key is help by another bonded role. So it would require the
 * cooperation of the oracle node operator with the key holder.
 */
@Slf4j
public final class Bisq1BridgeStore implements PersistableStore<Bisq1BridgeStore> {
    @Getter
    private final Set<AuthorizeAccountAgeRequest> accountAgeRequests = new CopyOnWriteArraySet<>();
    @Getter
    private final Set<AuthorizeSignedWitnessRequest> signedWitnessRequests = new CopyOnWriteArraySet<>();

    public Bisq1BridgeStore() {
    }

    private Bisq1BridgeStore(Set<AuthorizeAccountAgeRequest> accountAgeRequests, Set<AuthorizeSignedWitnessRequest> signedWitnessRequests) {
        this.accountAgeRequests.addAll(accountAgeRequests);
        this.signedWitnessRequests.addAll(signedWitnessRequests);
    }

    @Override
    public bisq.oracle_node.protobuf.Bisq1BridgeStore toProto() {
        return bisq.oracle_node.protobuf.Bisq1BridgeStore.newBuilder()
                .addAllAccountAgeRequests(accountAgeRequests.stream()
                        .map(AuthorizeAccountAgeRequest::toAuthorizeAccountAgeRequestProto)
                        .collect(Collectors.toList()))
                .addAllSignedWitnessRequests(signedWitnessRequests.stream()
                        .map(AuthorizeSignedWitnessRequest::toAuthorizeSignedWitnessRequestProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Bisq1BridgeStore fromProto(bisq.oracle_node.protobuf.Bisq1BridgeStore proto) {
        return new Bisq1BridgeStore(
                proto.getAccountAgeRequestsList().stream()
                        .map(AuthorizeAccountAgeRequest::fromProto)
                        .collect(Collectors.toSet()),
                proto.getSignedWitnessRequestsList().stream()
                        .map(AuthorizeSignedWitnessRequest::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle_node.protobuf.Bisq1BridgeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public Bisq1BridgeStore getClone() {
        return new Bisq1BridgeStore(accountAgeRequests, signedWitnessRequests);
    }

    @Override
    public void applyPersisted(Bisq1BridgeStore persisted) {
        accountAgeRequests.clear();
        accountAgeRequests.addAll(persisted.getAccountAgeRequests());
        signedWitnessRequests.clear();
        signedWitnessRequests.addAll(persisted.getSignedWitnessRequests());
    }
}