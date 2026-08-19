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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.services.BondedRoleGrpcService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Bisq1BridgeRequestServiceTest {
    @Test
    void boundRegistrationAdmissionIsEnabledByDefault() {
        boolean enabled = ConfigFactory.parseResources("oracle_node.conf")
                .resolve()
                .getBoolean("application.oracleNode.bisq1Bridge.bondedRoleRegistrationEnabled");

        assertThat(enabled).isTrue();
    }

    @Test
    void transportChangesDoNotChangeTheLogicalRegistrationBinding() {
        BondedRoleRegistrationRequest first = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest moved = createCurrentRequest(4321, "b".repeat(64));

        assertThat(Bisq1BridgeRequestService.registrationsMatch(first, moved)).isTrue();
    }

    @Test
    void anotherLockupIsAnIndependentRegistration() {
        BondedRoleRegistrationRequest first = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest replacement = createCurrentRequest(1234, "c".repeat(64));

        assertThat(Bisq1BridgeRequestService.registrationsMatch(first, replacement)).isFalse();
    }

    @Test
    void successfulRemovalDeletesOnlyTheExactTransportVariant() {
        BondedRoleRegistrationRequest first = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest moved = createCurrentRequest(4321, "b".repeat(64));
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                mock(NetworkService.class), mock(AuthorizedBondedRolesService.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().addAll(List.of(first, moved));

        service.removeRegistrationRequest(first);

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(moved);
    }

    @Test
    void recoversRegistrationReplayedWhenListenerIsInstalled() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        AuthorizedBondedRole role = mock(AuthorizedBondedRole.class);
        when(role.getProfileId()).thenReturn(request.getProfileId());
        when(role.getAuthorizedPublicKey()).thenReturn(request.getAuthorizedPublicKey());
        when(role.getBondedRoleType()).thenReturn(request.getBondedRoleType());
        when(role.getBondUserName()).thenReturn(request.getBondUserName());
        when(role.getSignatureBase64()).thenReturn(request.getSignatureBase64());
        when(role.getAddressByTransportTypeMap()).thenReturn(request.getAddressByTransportTypeMap());
        when(role.getNetworkId()).thenReturn(request.getNetworkId());
        when(role.getRegistrationProtocolVersion()).thenReturn(request.getRegistrationProtocolVersion());
        when(role.getProposalTxId()).thenReturn(request.getProposalTxId());
        when(role.getLockupTxId()).thenReturn(request.getLockupTxId());
        when(role.canReconstructForRemoval()).thenReturn(true);
        AuthorizedData authorizedData = new AuthorizedData(role, authorizedKeyPair.getPublic());

        NetworkService networkService = mock(NetworkService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)).thenReturn(Stream.of(role));
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService, authorizedBondedRolesService);

        service.onAuthorizedDataAdded(authorizedData);

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(request);
    }

    @Test
    void recoversRegistrationLoadedBeforeListenerInstallation() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole role = new AuthorizedBondedRole(request.getProfileId(),
                request.getAuthorizedPublicKey(),
                request.getBondedRoleType(),
                request.getBondUserName(),
                request.getSignatureBase64(),
                request.getAddressByTransportTypeMap(),
                request.getNetworkId(),
                Optional.empty(),
                false,
                request.getRegistrationProtocolVersion(),
                request.getProposalTxId(),
                request.getLockupTxId());
        AuthorizedData authorizedData = new AuthorizedData(role, authorizedKeyPair.getPublic());

        DataService dataService = mock(DataService.class);
        when(dataService.getAuthorizedData()).thenReturn(Stream.of(authorizedData));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true))
                .thenAnswer(ignored -> Stream.of(role));
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService,
                authorizedBondedRolesService);

        service.recoverRegistrationRequestsFromLoadedData();
        service.onAuthorizedDataAdded(authorizedData);

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(request);
    }

    @Test
    void skipsNetworkDataThatCannotBeReconstructedForRemoval() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole role = AuthorizedBondedRole.fromProto(new AuthorizedBondedRole(request.getProfileId(),
                        request.getAuthorizedPublicKey(),
                        request.getBondedRoleType(),
                        request.getBondUserName(),
                        request.getSignatureBase64(),
                        request.getAddressByTransportTypeMap(),
                        request.getNetworkId(),
                        Optional.empty(),
                        false,
                        BondedRoleRegistrationProtocol.CURRENT_VERSION,
                        request.getProposalTxId(),
                        request.getLockupTxId())
                .toProto(false)
                .toBuilder()
                .setVersion(0)
                .setRegistrationProtocolVersion(BondedRoleRegistrationProtocol.LEGACY_VERSION)
                .clearProposalTxId()
                .clearLockupTxId()
                .build());
        AuthorizedData authorizedData = new AuthorizedData(role, authorizedKeyPair.getPublic());

        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)).thenReturn(Stream.of(role));
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                mock(NetworkService.class), authorizedBondedRolesService);

        service.onAuthorizedDataAdded(authorizedData);

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void unknownLegacyRegistrationIsRejectedBeforeBridgeVerification() {
        BondedRoleRegistrationRequest request = legacyRequest(false);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        useDirectExecutor(service);

        service.onConfidentialMessage(request, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService, never()).requestBondedRoleVerification(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void unsupportedRegistrationIsIgnoredBeforeBridgeVerification() {
        BondedRoleRegistrationRequest current = createCurrentRequest();
        BondedRoleRegistrationRequest request = new BondedRoleRegistrationRequest(current.getProfileId(),
                current.getAuthorizedPublicKey(),
                current.getBondedRoleType(),
                current.getBondUserName(),
                current.getSignatureBase64(),
                current.getAddressByTransportTypeMap(),
                current.getNetworkId(),
                false,
                3,
                current.getProposalTxId(),
                current.getLockupTxId());
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        useDirectExecutor(service);

        service.onConfidentialMessage(request, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService, never()).requestBondedRoleVerification(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void disabledAdmissionRejectsRegistrationBeforeBridgeVerification() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(
                bondedRoleGrpcService, false);
        useDirectExecutor(service);

        service.onConfidentialMessage(request, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService, never()).requestBondedRoleVerification(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void disabledAdmissionStillAllowsKnownCancellation() {
        BondedRoleRegistrationRequest registration = legacyRequest(false);
        BondedRoleRegistrationRequest cancellation = legacyRequest(true);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.empty()));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        IdentityService identityService = mock(IdentityService.class);
        Identity identity = mock(Identity.class);
        when(identity.getNetworkIdWithKeyPair()).thenReturn(
                new bisq.network.identity.NetworkIdWithKeyPair(registration.getNetworkId(),
                        KeyGeneration.generateDefaultEcKeyPair()));
        when(identityService.getOrCreateDefaultIdentity()).thenReturn(identity);
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                identityService,
                networkService,
                mock(AuthorizedBondedRolesService.class),
                bondedRoleGrpcService,
                mock(GrpcClient.class),
                false);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(registration);
        useDirectExecutor(service);

        service.onConfidentialMessage(cancellation, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void disabledAdmissionStillAllowsKnownLegacyRenewal() {
        BondedRoleRegistrationRequest registration = legacyRequest(false);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.of("stop after admission")));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(
                bondedRoleGrpcService, false);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(registration);
        useDirectExecutor(service);

        service.onConfidentialMessage(registration, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
    }

    @Test
    void knownLegacyCancellationIsVerifiedAndRemoved() {
        BondedRoleRegistrationRequest registration = legacyRequest(false);
        BondedRoleRegistrationRequest cancellation = legacyRequest(true);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.empty()));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        IdentityService identityService = mock(IdentityService.class);
        Identity identity = mock(Identity.class);
        when(identity.getNetworkIdWithKeyPair()).thenReturn(
                new bisq.network.identity.NetworkIdWithKeyPair(registration.getNetworkId(),
                        KeyGeneration.generateDefaultEcKeyPair()));
        when(identityService.getOrCreateDefaultIdentity()).thenReturn(identity);
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                identityService, networkService, mock(AuthorizedBondedRolesService.class), bondedRoleGrpcService,
                mock(GrpcClient.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(registration);
        useDirectExecutor(service);

        service.onConfidentialMessage(cancellation, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
        verify(networkService).removeAuthorizedData(any(), any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void malformedBatchResponseRetainsEveryRegistration() {
        BondedRoleRegistrationRequest first = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest second = createCurrentRequest(4321, "c".repeat(64));
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.empty()))));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        service.getPersistableStore().getBondedRoleRegistrationRequests().addAll(List.of(first, second));

        service.revalidateBondedRolesNow();

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests())
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void bridgeFailureRetainsEveryRegistration() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenThrow(new RuntimeException("bridge unavailable"));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(request);

        service.revalidateBondedRolesNow();

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(request);
    }

    @Test
    void invalidBatchEntryRemovesOnlyItsExactRegistration() {
        BondedRoleRegistrationRequest invalid = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest valid = createCurrentRequest(4321, "c".repeat(64));
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.of("invalid")),
                                new BondedRoleVerificationResponse(Optional.empty()))));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        IdentityService identityService = mock(IdentityService.class);
        Identity identity = mock(Identity.class);
        when(identity.getNetworkIdWithKeyPair()).thenReturn(
                new bisq.network.identity.NetworkIdWithKeyPair(invalid.getNetworkId(),
                        KeyGeneration.generateDefaultEcKeyPair()));
        when(identityService.getOrCreateDefaultIdentity()).thenReturn(identity);
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                identityService, networkService, mock(AuthorizedBondedRolesService.class), bondedRoleGrpcService,
                mock(GrpcClient.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().addAll(List.of(invalid, valid));

        service.revalidateBondedRolesNow();

        verify(networkService).removeAuthorizedData(any(), any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(valid);
    }

    @Test
    void batchAboveLimitRetainsRegistrationsWithoutCallingTheBridge() {
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        for (int port = 1; port <= 1001; port++) {
            service.getPersistableStore().getBondedRoleRegistrationRequests()
                    .add(createCurrentRequest(port, "b".repeat(64)));
        }

        service.revalidateBondedRolesNow();

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).hasSize(1001);
        verify(bondedRoleGrpcService, never()).requestBondedRoleBatchVerification(any());
    }

    @Test
    void rejectedRevalidationSubmissionDoesNotWedgeLaterTriggers() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.empty()))));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(request);

        var rejectedExecutor = MoreExecutors.newDirectExecutorService();
        rejectedExecutor.shutdown();
        service.setRevalidationExecutor(rejectedExecutor);
        service.revalidateBondedRoles();

        service.setRevalidationExecutor(MoreExecutors.newDirectExecutorService());
        service.revalidateBondedRoles();

        verify(bondedRoleGrpcService, times(1)).requestBondedRoleBatchVerification(any());
    }

    @Test
    void sharedRequestExecutorRejectionDoesNotDiscardRevalidation() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.empty()))));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(request);

        var rejectedRequestExecutor = MoreExecutors.newDirectExecutorService();
        rejectedRequestExecutor.shutdown();
        service.setExecutor(rejectedRequestExecutor);
        service.setRevalidationExecutor(MoreExecutors.newDirectExecutorService());

        service.revalidateBondedRoles();

        verify(bondedRoleGrpcService).requestBondedRoleBatchVerification(any());
    }

    private static BondedRoleRegistrationRequest legacyRequest(boolean cancellation) {
        BondedRoleRegistrationRequest current = createCurrentRequest();
        return new BondedRoleRegistrationRequest(current.getProfileId(),
                current.getAuthorizedPublicKey(),
                current.getBondedRoleType(),
                current.getBondUserName(),
                current.getSignatureBase64(),
                current.getAddressByTransportTypeMap(),
                current.getNetworkId(),
                cancellation,
                BondedRoleRegistrationProtocol.LEGACY_VERSION,
                "",
                "");
    }

    private static void useDirectExecutor(Bisq1BridgeRequestService service) {
        service.setExecutor(MoreExecutors.newDirectExecutorService());
    }

    private static Bisq1BridgeRequestService createServiceWithBondedRoleGrpcService(
            BondedRoleGrpcService bondedRoleGrpcService) {
        return createServiceWithBondedRoleGrpcService(bondedRoleGrpcService, true);
    }

    private static Bisq1BridgeRequestService createServiceWithBondedRoleGrpcService(
            BondedRoleGrpcService bondedRoleGrpcService,
            boolean bondedRoleRegistrationEnabled) {
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        return createService(authorizedKeyPair,
                mock(IdentityService.class),
                mock(NetworkService.class),
                mock(AuthorizedBondedRolesService.class),
                bondedRoleGrpcService,
                mock(GrpcClient.class),
                bondedRoleRegistrationEnabled);
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
                                                           NetworkService networkService,
                                                           AuthorizedBondedRolesService authorizedBondedRolesService) {
        GrpcClient grpcClient = mock(GrpcClient.class);
        return createService(authorizedKeyPair, networkService, authorizedBondedRolesService,
                new BondedRoleGrpcService(grpcClient), grpcClient);
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
                                                           NetworkService networkService,
                                                           AuthorizedBondedRolesService authorizedBondedRolesService,
                                                           BondedRoleGrpcService bondedRoleGrpcService) {
        return createService(authorizedKeyPair, networkService, authorizedBondedRolesService,
                bondedRoleGrpcService, mock(GrpcClient.class));
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
                                                           NetworkService networkService,
                                                           AuthorizedBondedRolesService authorizedBondedRolesService,
                                                           BondedRoleGrpcService bondedRoleGrpcService,
                                                           GrpcClient grpcClient) {
        return createService(authorizedKeyPair,
                mock(IdentityService.class),
                networkService,
                authorizedBondedRolesService,
                bondedRoleGrpcService,
                grpcClient,
                true);
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
                                                           IdentityService identityService,
                                                           NetworkService networkService,
                                                           AuthorizedBondedRolesService authorizedBondedRolesService,
                                                           BondedRoleGrpcService bondedRoleGrpcService,
                                                           GrpcClient grpcClient) {
        return createService(authorizedKeyPair,
                identityService,
                networkService,
                authorizedBondedRolesService,
                bondedRoleGrpcService,
                grpcClient,
                true);
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
                                                           IdentityService identityService,
                                                           NetworkService networkService,
                                                           AuthorizedBondedRolesService authorizedBondedRolesService,
                                                           BondedRoleGrpcService bondedRoleGrpcService,
                                                           GrpcClient grpcClient,
                                                           boolean bondedRoleRegistrationEnabled) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<Bisq1BridgeRequestStore> persistence = mock(Persistence.class);
        when(persistenceService.<Bisq1BridgeRequestStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        return new Bisq1BridgeRequestService(persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic(),
                false,
                bondedRoleRegistrationEnabled,
                mock(AuthorizedOracleNode.class),
                grpcClient,
                bondedRoleGrpcService);
    }
}
