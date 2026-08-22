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
import bisq.common.encoding.Hex;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.AccountAgeWitnessOwnershipResponse;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.messages.SignedWitnessOwnershipResponse;
import bisq.oracle_node.bisq1_bridge.grpc.services.AccountAgeWitnessGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.BondedRoleGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.SignedWitnessGrpcService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationRequest.MAX_REGISTRATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Bisq1BridgeRequestServiceTest {
    @Test
    void witnessAuthorizationRejectsAProfileThatDoesNotMatchTheConfidentialSender() {
        AccountAgeWitnessGrpcService accountAgeGrpcService = mock(AccountAgeWitnessGrpcService.class);
        SignedWitnessGrpcService signedWitnessGrpcService = mock(SignedWitnessGrpcService.class);
        NetworkService networkService = mock(NetworkService.class);
        Bisq1BridgeRequestService service = createServiceWithWitnessGrpcServices(
                accountAgeGrpcService, signedWitnessGrpcService, networkService);
        useDirectExecutor(service);
        String profileId = profileId(KeyGeneration.generateDefaultEcKeyPair());
        var anotherSender = KeyGeneration.generateDefaultEcKeyPair();

        service.onConfidentialMessage(accountAgeRequest(profileId), anotherSender.getPublic());
        service.onConfidentialMessage(signedWitnessRequest(profileId), anotherSender.getPublic());

        verifyNoInteractions(accountAgeGrpcService, signedWitnessGrpcService, networkService);
        assertThat(service.getPersistableStore().getAccountAgeRequests()).isEmpty();
        assertThat(service.getPersistableStore().getSignedWitnessRequests()).isEmpty();
    }

    @Test
    void witnessAuthorizationFailsClosedWhenBridgeVerificationFails() {
        AccountAgeWitnessGrpcService accountAgeGrpcService = mock(AccountAgeWitnessGrpcService.class);
        SignedWitnessGrpcService signedWitnessGrpcService = mock(SignedWitnessGrpcService.class);
        when(accountAgeGrpcService.verifyAndRequestAuthorization(any()))
                .thenThrow(new RuntimeException("unavailable"));
        when(signedWitnessGrpcService.verifyAndRequestAuthorization(any()))
                .thenThrow(new RuntimeException("unavailable"));
        NetworkService networkService = mock(NetworkService.class);
        Bisq1BridgeRequestService service = createServiceWithWitnessGrpcServices(
                accountAgeGrpcService, signedWitnessGrpcService, networkService);
        useDirectExecutor(service);
        var sender = KeyGeneration.generateDefaultEcKeyPair();
        String profileId = profileId(sender);

        service.onConfidentialMessage(accountAgeRequest(profileId), sender.getPublic());
        service.onConfidentialMessage(signedWitnessRequest(profileId), sender.getPublic());

        verify(accountAgeGrpcService).verifyAndRequestAuthorization(any());
        verify(signedWitnessGrpcService).verifyAndRequestAuthorization(any());
        verifyNoInteractions(networkService);
        assertThat(service.getPersistableStore().getAccountAgeRequests()).isEmpty();
        assertThat(service.getPersistableStore().getSignedWitnessRequests()).isEmpty();
    }

    @Test
    void witnessAuthorizationPublishesOnlyBucketsAndNullifiers() {
        long accountAgeBucket = dateBucket(1_700_000_123_456L);
        long signedWitnessBucket = dateBucket(1_710_000_123_456L);
        byte[] witnessNullifier = new byte[32];
        witnessNullifier[0] = 42;
        AccountAgeWitnessGrpcService accountAgeGrpcService = mock(AccountAgeWitnessGrpcService.class);
        SignedWitnessGrpcService signedWitnessGrpcService = mock(SignedWitnessGrpcService.class);
        when(accountAgeGrpcService.verifyAndRequestAuthorization(any()))
                .thenReturn(new AccountAgeWitnessOwnershipResponse(accountAgeBucket, witnessNullifier));
        when(signedWitnessGrpcService.verifyAndRequestAuthorization(any()))
                .thenReturn(new SignedWitnessOwnershipResponse(signedWitnessBucket, witnessNullifier));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.publishAuthorizedData(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        Bisq1BridgeRequestService service = createServiceWithWitnessGrpcServices(
                accountAgeGrpcService, signedWitnessGrpcService, networkService);
        useDirectExecutor(service);
        KeyPair sender = KeyGeneration.generateDefaultEcKeyPair();
        String profileId = profileId(sender);

        service.onConfidentialMessage(accountAgeRequest(profileId), sender.getPublic());
        service.onConfidentialMessage(signedWitnessRequest(profileId), sender.getPublic());

        ArgumentCaptor<AuthorizedDistributedData> publishedData =
                ArgumentCaptor.forClass(AuthorizedDistributedData.class);
        verify(networkService, times(2)).publishAuthorizedData(publishedData.capture(), any(), any(), any());
        assertThat(publishedData.getAllValues())
                .anySatisfy(value -> {
                    assertThat(value).isInstanceOf(AuthorizedAccountAgeData.class);
                    AuthorizedAccountAgeData data = (AuthorizedAccountAgeData) value;
                    assertThat(data.getDateBucket()).isEqualTo(accountAgeBucket);
                    assertThat(data.getWitnessNullifier()).containsExactly(witnessNullifier);
                })
                .anySatisfy(value -> {
                    assertThat(value).isInstanceOf(AuthorizedSignedWitnessData.class);
                    AuthorizedSignedWitnessData data = (AuthorizedSignedWitnessData) value;
                    assertThat(data.getDateBucket()).isEqualTo(signedWitnessBucket);
                    assertThat(data.getWitnessNullifier()).containsExactly(witnessNullifier);
                });
    }

    @Test
    void oneAccountAgeWitnessCannotBePersistedForTwoProfiles() {
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(
                mock(BondedRoleGrpcService.class));
        AuthorizeAccountAgeRequest first = accountAgeRequest("12".repeat(20));
        AuthorizeAccountAgeRequest conflicting = accountAgeRequest("34".repeat(20));

        assertThat(service.persistAccountAgeRequest(first)).isTrue();
        assertThat(service.persistAccountAgeRequest(conflicting)).isFalse();
        assertThat(service.getPersistableStore().getAccountAgeRequests()).containsExactly(first);
    }

    @Test
    void witnessCannotBePersistedForAnotherProfileAcrossReputationSources() {
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(
                mock(BondedRoleGrpcService.class));
        AuthorizeAccountAgeRequest accountAge = accountAgeRequest("12".repeat(20));
        AuthorizeSignedWitnessRequest signedWitness = signedWitnessRequest("34".repeat(20));

        assertThat(service.persistAccountAgeRequest(accountAge)).isTrue();
        assertThat(service.persistSignedWitnessRequest(signedWitness)).isFalse();
        assertThat(service.getPersistableStore().getSignedWitnessRequests()).isEmpty();
    }

    @Test
    void bothReputationSourcesMayUseOneWitnessForTheSameProfile() {
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(
                mock(BondedRoleGrpcService.class));
        AuthorizeAccountAgeRequest accountAge = accountAgeRequest("12".repeat(20));
        AuthorizeSignedWitnessRequest signedWitness = signedWitnessRequest("12".repeat(20));

        assertThat(service.persistSignedWitnessRequest(signedWitness)).isTrue();
        assertThat(service.persistAccountAgeRequest(accountAge)).isTrue();
        assertThat(service.getPersistableStore().getAccountAgeRequests()).containsExactly(accountAge);
        assertThat(service.getPersistableStore().getSignedWitnessRequests()).containsExactly(signedWitness);
    }

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
    void malformedLoadedRegistrationDoesNotAbortRecoveryOfRemainingRecords() {
        BondedRoleRegistrationRequest request = createCurrentRequest();
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole malformedRole = mock(AuthorizedBondedRole.class);
        when(malformedRole.canReconstructForRemoval()).thenReturn(true);
        when(malformedRole.getProfileId()).thenReturn("invalid");
        AuthorizedBondedRole validRole = new AuthorizedBondedRole(request.getProfileId(),
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
        AuthorizedData malformedData = new AuthorizedData(malformedRole, authorizedKeyPair.getPublic());
        AuthorizedData validData = new AuthorizedData(validRole, authorizedKeyPair.getPublic());

        DataService dataService = mock(DataService.class);
        when(dataService.getAuthorizedData()).thenReturn(Stream.of(malformedData, validData));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(true))
                .thenAnswer(ignored -> Stream.of(malformedRole, validRole));
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService,
                authorizedBondedRolesService);

        service.recoverRegistrationRequestsFromLoadedData();

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
        int registrationCount = MAX_REGISTRATIONS + 1;
        for (int port = 1; port <= registrationCount; port++) {
            service.getPersistableStore().getBondedRoleRegistrationRequests()
                    .add(createCurrentRequest(port, "b".repeat(64)));
        }

        service.revalidateBondedRolesNow();

        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).hasSize(registrationCount);
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

    private static AuthorizeAccountAgeRequest accountAgeRequest(String profileId) {
        return new AuthorizeAccountAgeRequest(profileId,
                "56".repeat(20),
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));
    }

    private static AuthorizeSignedWitnessRequest signedWitnessRequest(String profileId) {
        return new AuthorizeSignedWitnessRequest(profileId,
                "56".repeat(20),
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));
    }

    private static String profileId(KeyPair keyPair) {
        return Hex.encode(DigestUtil.hash(keyPair.getPublic().getEncoded()));
    }

    private static long dateBucket(long date) {
        long bucketSize = bisq.user.reputation.WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;
        return Math.floorDiv(date, bucketSize) * bucketSize;
    }

    private static void useDirectExecutor(Bisq1BridgeRequestService service) {
        service.setExecutor(MoreExecutors.newDirectExecutorService());
    }

    private static Bisq1BridgeRequestService createServiceWithBondedRoleGrpcService(
            BondedRoleGrpcService bondedRoleGrpcService) {
        return createServiceWithBondedRoleGrpcService(bondedRoleGrpcService, true);
    }

    private static Bisq1BridgeRequestService createServiceWithWitnessGrpcServices(
            AccountAgeWitnessGrpcService accountAgeWitnessGrpcService,
            SignedWitnessGrpcService signedWitnessGrpcService,
            NetworkService networkService) {
        var authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<Bisq1BridgeRequestStore> persistence = mock(Persistence.class);
        when(persistenceService.<Bisq1BridgeRequestStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        GrpcClient grpcClient = mock(GrpcClient.class);

        IdentityService identityService = mock(IdentityService.class);
        Identity identity = mock(Identity.class);
        when(identity.getNetworkIdWithKeyPair()).thenReturn(
                new bisq.network.identity.NetworkIdWithKeyPair(
                        mock(bisq.network.identity.NetworkId.class),
                        KeyGeneration.generateDefaultEcKeyPair()));
        when(identityService.getOrCreateDefaultIdentity()).thenReturn(identity);

        return new Bisq1BridgeRequestService(persistenceService,
                identityService,
                networkService,
                mock(AuthorizedBondedRolesService.class),
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic(),
                false,
                true,
                mock(AuthorizedOracleNode.class),
                grpcClient,
                mock(BondedRoleGrpcService.class),
                accountAgeWitnessGrpcService,
                signedWitnessGrpcService);
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
