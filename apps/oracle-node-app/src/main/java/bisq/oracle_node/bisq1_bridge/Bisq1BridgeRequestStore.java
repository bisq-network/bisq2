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

import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * We persist the requests so that in case of a scam or trade rule violation we could block the user at Bisq 1 as well.
 * This is a bit of a trade-off between security and privacy. One option to improve that would be that all data is
 * persisted as encrypted entries and the decryption key is help by another bonded role. So it would require the
 * cooperation of the oracle node operator with the key holder.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class Bisq1BridgeRequestStore implements PersistableStore<Bisq1BridgeRequestStore> {
    // Pin the migration to version 2; a later protocol may give these date fields meaning again.
    private static final int WITNESS_REQUEST_VERSION_WITH_UNUSED_DATES = 2;

    @Getter(AccessLevel.PACKAGE)
    private final Set<AuthorizeAccountAgeRequest> accountAgeRequests = new CopyOnWriteArraySet<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<AuthorizeSignedWitnessRequest> signedWitnessRequests = new CopyOnWriteArraySet<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<ByteArray, Long> accountTimestampDateByHash = new ConcurrentHashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<BondedRoleRegistrationRequest> bondedRoleRegistrationRequests = new CopyOnWriteArraySet<>();

    private Bisq1BridgeRequestStore(Set<AuthorizeAccountAgeRequest> accountAgeRequests,
                                    Set<AuthorizeSignedWitnessRequest> signedWitnessRequests,
                                    Map<ByteArray, Long> accountTimestampDateByHash,
                                    Set<BondedRoleRegistrationRequest> bondedRoleRegistrationRequests) {
        this.accountAgeRequests.addAll(accountAgeRequests);
        this.signedWitnessRequests.addAll(signedWitnessRequests);
        this.accountTimestampDateByHash.putAll(accountTimestampDateByHash);
        this.bondedRoleRegistrationRequests.addAll(bondedRoleRegistrationRequests);
    }

    @Override
    public bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.Builder getBuilder(boolean serializeForHash) {

        return bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder()
                .addAllAccountAgeRequests(accountAgeRequests.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllSignedWitnessRequests(signedWitnessRequests.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllBondedRoleRegistrationRequests(bondedRoleRegistrationRequests.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()))
                .putAllAccountTimestampDateByHash(accountTimestampDateByHash.entrySet().stream()
                        .collect(Collectors.toMap(e -> Hex.encode(e.getKey().getBytes()),
                                Map.Entry::getValue)));
    }

    @Override
    public bisq.oracle_node.protobuf.Bisq1BridgeRequestStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    // Unlike other stores, this store does not skip a persisted request which it cannot parse (rule 4 of
    // docs/specifications/network/validation-at-parsing.md). The requests are permanent witness bindings. A skipped
    // request would be missing from the conflict check of Bisq1BridgeRequestService, so the oracle could bind its
    // witness to another profile (see docs/specifications/reputation/account-age.md).
    public static Bisq1BridgeRequestStore fromProto(bisq.oracle_node.protobuf.Bisq1BridgeRequestStore proto)
            throws UnresolvableProtobufException {
        try {
            return new Bisq1BridgeRequestStore(
                    ProtobufUtils.fromProtoStream(proto.getAccountAgeRequestsList(),
                                    Bisq1BridgeRequestStore::fromPersistedAccountAgeRequest)
                            .collect(Collectors.toSet()),
                    ProtobufUtils.fromProtoStream(proto.getSignedWitnessRequestsList(),
                                    Bisq1BridgeRequestStore::fromPersistedSignedWitnessRequest)
                            .collect(Collectors.toSet()),
                    proto.getAccountTimestampDateByHashMap().entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> new ByteArray(Hex.decode(e.getKey())),
                                    Map.Entry::getValue
                            )),
                    ProtobufUtils.fromProtoStream(proto.getBondedRoleRegistrationRequestsList(),
                                    BondedRoleRegistrationRequest::fromProto)
                            .filter(request -> {
                                if (request.isCancellationRequest()) {
                                    log.warn("Ignoring cancellation request found in the persisted bonded-role registration set");
                                    return false;
                                }
                                return true;
                            })
                            .collect(Collectors.toSet()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    // Earlier version-2 requests could persist unused dates. Normalize them only at this persistence boundary
    // so their ownership bindings survive stricter network validation without changing the proof.
    private static AuthorizeAccountAgeRequest fromPersistedAccountAgeRequest(bisq.user.protobuf.AuthorizeAccountAgeRequest proto)
            throws UnresolvableProtobufException {
        if (proto.getProtocolVersion() == WITNESS_REQUEST_VERSION_WITH_UNUSED_DATES) {
            proto = proto.toBuilder().clearDate().build();
        }
        return AuthorizeAccountAgeRequest.fromProto(proto);
    }

    private static AuthorizeSignedWitnessRequest fromPersistedSignedWitnessRequest(bisq.user.protobuf.AuthorizeSignedWitnessRequest proto)
            throws UnresolvableProtobufException {
        if (proto.getProtocolVersion() == WITNESS_REQUEST_VERSION_WITH_UNUSED_DATES) {
            proto = proto.toBuilder().clearAccountAgeWitnessDate().clearWitnessSignDate().build();
        }
        return AuthorizeSignedWitnessRequest.fromProto(proto);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public Bisq1BridgeRequestStore getClone() {
        return new Bisq1BridgeRequestStore(Set.copyOf(accountAgeRequests),
                Set.copyOf(signedWitnessRequests),
                Map.copyOf(accountTimestampDateByHash),
                Set.copyOf(bondedRoleRegistrationRequests));
    }

    @Override
    public void applyPersisted(Bisq1BridgeRequestStore persisted) {
        accountAgeRequests.clear();
        accountAgeRequests.addAll(persisted.getAccountAgeRequests());
        signedWitnessRequests.clear();
        signedWitnessRequests.addAll(persisted.getSignedWitnessRequests());
        accountTimestampDateByHash.clear();
        accountTimestampDateByHash.putAll(persisted.getAccountTimestampDateByHash());
        bondedRoleRegistrationRequests.clear();
        bondedRoleRegistrationRequests.addAll(persisted.getBondedRoleRegistrationRequests());
    }
}
