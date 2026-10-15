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
import bisq.common.data.ByteArray;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Bisq1BridgeRequestStoreTest {
    @ParameterizedTest
    @CsvSource({
            "1700000000000, 0, 0",
            "0, 1700000000000, 0",
            "0, 0, 1700000000001",
            "1700000000000, 1700000000000, 1700000000001"
    })
    void migratesVersionTwoDatesWithoutLosingStoredBindings(long accountDate, long signedAccountDate, long signDate)
            throws UnresolvableProtobufException {
        AuthorizeAccountAgeRequest account = accountAgeRequest("12".repeat(20), "34".repeat(20));
        AuthorizeAccountAgeRequest migratedAccount = accountAgeRequest("56".repeat(20), "78".repeat(20));
        AuthorizeSignedWitnessRequest signed = signedWitnessRequest("12".repeat(20), "34".repeat(20));
        AuthorizeSignedWitnessRequest migratedSigned = signedWitnessRequest("56".repeat(20), "78".repeat(20));
        BondedRoleRegistrationRequest role = createCurrentRequest();
        ByteArray timestampHash = new ByteArray(new byte[32]);
        long timestamp = 1_700_000_000_000L;
        bisq.oracle_node.protobuf.Bisq1BridgeRequestStore proto =
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder()
                        .addAccountAgeRequests(account.toValueProto(false))
                        .addAccountAgeRequests(migratedAccount.toValueProto(false).toBuilder().setDate(accountDate))
                        .addSignedWitnessRequests(signed.toValueProto(false))
                        .addSignedWitnessRequests(migratedSigned.toValueProto(false).toBuilder()
                                .setAccountAgeWitnessDate(signedAccountDate)
                                .setWitnessSignDate(signDate))
                        .addBondedRoleRegistrationRequests(role.toValueProto(false))
                        .putAccountTimestampDateByHash("00".repeat(32), timestamp)
                        .build();

        Bisq1BridgeRequestStore loaded = Bisq1BridgeRequestStore.fromProto(proto);
        Bisq1BridgeRequestStore reloaded = Bisq1BridgeRequestStore.fromProto(loaded.toProto(false));

        for (Bisq1BridgeRequestStore store : List.of(loaded, reloaded)) {
            assertThat(store.getAccountAgeRequests()).containsExactlyInAnyOrder(account, migratedAccount);
            assertThat(store.getSignedWitnessRequests()).containsExactlyInAnyOrder(signed, migratedSigned);
            assertThat(store.getBondedRoleRegistrationRequests()).containsExactly(role);
            assertThat(store.getAccountTimestampDateByHash()).hasSize(1).containsEntry(timestampHash, timestamp);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 3})
    void preservesDatesOutsideVersionTwo(int version) throws UnresolvableProtobufException {
        var account = accountAgeRequest("12".repeat(20), "34".repeat(20)).toValueProto(false).toBuilder()
                .setDate(1_700_000_000_000L);
        var signed = signedWitnessRequest("12".repeat(20), "34".repeat(20)).toValueProto(false).toBuilder()
                .setAccountAgeWitnessDate(1_700_000_000_000L)
                .setWitnessSignDate(1_700_000_000_001L);
        if (version == -1) {
            account.clearProtocolVersion();
            signed.clearProtocolVersion();
        } else {
            account.setProtocolVersion(version);
            signed.setProtocolVersion(version);
        }
        bisq.oracle_node.protobuf.Bisq1BridgeRequestStore proto =
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder()
                        .addAccountAgeRequests(account)
                        .addSignedWitnessRequests(signed)
                        .build();

        Bisq1BridgeRequestStore loaded = Bisq1BridgeRequestStore.fromProto(proto);

        assertThat(loaded.getAccountAgeRequests()).containsExactly(AuthorizeAccountAgeRequest.fromProto(account.build()));
        assertThat(loaded.getSignedWitnessRequests()).containsExactly(AuthorizeSignedWitnessRequest.fromProto(signed.build()));
    }

    @Test
    void dateMigrationDoesNotAcceptAMissingOwnershipPreimage() {
        var account = accountAgeRequest("12".repeat(20), "34".repeat(20)).toValueProto(false).toBuilder()
                .setDate(1_700_000_000_000L)
                .clearAccountInputDataWithSalt();
        var signed = signedWitnessRequest("12".repeat(20), "34".repeat(20)).toValueProto(false).toBuilder()
                .setWitnessSignDate(1_700_000_000_000L)
                .clearAccountInputDataWithSalt();

        assertThatThrownBy(() -> Bisq1BridgeRequestStore.fromProto(
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder().addAccountAgeRequests(account).build()))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Bisq1BridgeRequestStore.fromProto(
                bisq.oracle_node.protobuf.Bisq1BridgeRequestStore.newBuilder().addSignedWitnessRequests(signed).build()))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistsSignedWitnessOwnershipProofs() throws UnresolvableProtobufException {
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
    void persistsCompleteBondedRoleRegistrationRequests() throws UnresolvableProtobufException {
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
    void ignoresCancellationCommandsInThePersistedRegistrationSet() throws UnresolvableProtobufException {
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

    private AuthorizeAccountAgeRequest accountAgeRequest(String profileId, String witnessHash) {
        return new AuthorizeAccountAgeRequest(profileId,
                witnessHash,
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));
    }

    private AuthorizeSignedWitnessRequest signedWitnessRequest(String profileId, String witnessHash) {
        return new AuthorizeSignedWitnessRequest(profileId,
                witnessHash,
                new byte[]{4, 5, 6},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));
    }
}
