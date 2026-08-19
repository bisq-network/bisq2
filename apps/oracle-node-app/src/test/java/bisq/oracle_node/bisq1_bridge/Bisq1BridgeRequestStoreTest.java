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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class Bisq1BridgeRequestStoreTest {
    @Test
    void persistsSignedWitnessOwnershipProofs() {
        AuthorizeSignedWitnessRequest request = new AuthorizeSignedWitnessRequest(
                "12".repeat(20),
                "34".repeat(20),
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));
        Bisq1BridgeRequestStore store = new Bisq1BridgeRequestStore();
        store.getSignedWitnessRequests().add(request);

        Bisq1BridgeRequestStore roundTrip = Bisq1BridgeRequestStore.fromProto(store.toProto(false));

        assertThat(roundTrip.getSignedWitnessRequests()).containsExactly(request);
    }

    @Test
    void persistsCompleteBondedRoleRegistrationRequests() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        Bisq1BridgeRequestStore store = new Bisq1BridgeRequestStore();
        store.getBondedRoleRegistrationRequests().add(request);

        Bisq1BridgeRequestStore roundTrip = Bisq1BridgeRequestStore.fromProto(store.toProto(false));

        assertThat(roundTrip.getBondedRoleRegistrationRequests()).containsExactly(request);
    }

    @Test
    void appliesAndClonesRegistrationRequests() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        Bisq1BridgeRequestStore source = new Bisq1BridgeRequestStore();
        source.getBondedRoleRegistrationRequests().add(request);

        Bisq1BridgeRequestStore target = new Bisq1BridgeRequestStore();
        target.applyPersisted(source.getClone());

        assertThat(target.getBondedRoleRegistrationRequests()).containsExactly(request);
    }

    @Test
    void loadsPersistedRegistrationWithAnUnsupportedProtocolVersion() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest futureRequest = request.toValueProto(false)
                .toBuilder()
                .setRegistrationProtocolVersion(3)
                .build();
        bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.Builder proto =
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder()
                .addBondedRoleRegistrationRequests(futureRequest);

        AtomicReference<Bisq1BridgeRequestStore> loadedStore = new AtomicReference<>();
        assertThatCode(() -> loadedStore.set(Bisq1BridgeRequestStore.fromProto(proto.build())))
                .doesNotThrowAnyException();
        assertThat(loadedStore.get().getBondedRoleRegistrationRequests())
                .singleElement()
                .satisfies(loaded -> {
                    assertThat(loaded.getBondedRoleType()).isEqualTo(BondedRoleType.MEDIATOR);
                    assertThat(loaded.getRegistrationProtocolVersion()).isEqualTo(3);
                });
    }

    @Test
    void ignoresCancellationCommandsInThePersistedRegistrationSet() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest cancellation = request.toValueProto(false)
                .toBuilder()
                .setIsCancellationRequest(true)
                .build();
        bisq.oracle_node.protobuf.Bisq1BridgeRequestStore proto =
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder()
                        .addBondedRoleRegistrationRequests(cancellation)
                        .build();

        assertThat(Bisq1BridgeRequestStore.fromProto(proto).getBondedRoleRegistrationRequests()).isEmpty();
    }
}
