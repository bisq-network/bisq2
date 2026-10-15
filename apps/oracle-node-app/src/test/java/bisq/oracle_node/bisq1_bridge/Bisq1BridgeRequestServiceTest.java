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

import bisq.account.timestamp.AuthorizeAccountTimestampV2Payload;
import bisq.account.timestamp.AuthorizeAccountTimestampV2Request;
import bisq.account.timestamp.AuthorizedAccountTimestamp;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.encoding.Hex;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.ByteArrayUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
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
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static bisq.oracle_node.TestBondedRoleRegistrations.LOCKUP_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.PROPOSAL_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationRequest.MAX_REGISTRATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
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
        when(networkService.publishAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        Bisq1BridgeRequestService service = createServiceWithWitnessGrpcServices(
                accountAgeGrpcService, signedWitnessGrpcService, networkService, authorizedKeyPair);
        useDirectExecutor(service);
        KeyPair sender = KeyGeneration.generateDefaultEcKeyPair();
        String profileId = profileId(sender);

        service.onConfidentialMessage(accountAgeRequest(profileId), sender.getPublic());
        service.onConfidentialMessage(signedWitnessRequest(profileId), sender.getPublic());

        ArgumentCaptor<AuthorizedDistributedData> publishedData =
                ArgumentCaptor.forClass(AuthorizedDistributedData.class);
        // The authorized key owns the data which the oracle node publishes (see
        // docs/specifications/network/storage-ownership.md)
        verify(networkService, times(2)).publishAuthorizedData(publishedData.capture(),
                argThat((KeyPair keyPair) -> keyPair.getPublic().equals(authorizedKeyPair.getPublic())));
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

    // A request which the node processed before the service started, for example from the mailbox, reaches the service
    // with its sender key (see docs/specifications/network/startup-replay.md)
    @Test
    void accountAgeRequestWhichTheNodeProcessedBeforeTheStartIsProcessed() {
        AccountAgeWitnessGrpcService accountAgeGrpcService = mock(AccountAgeWitnessGrpcService.class);
        when(accountAgeGrpcService.verifyAndRequestAuthorization(any()))
                .thenReturn(new AccountAgeWitnessOwnershipResponse(dateBucket(1_700_000_123_456L), new byte[32]));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.publishAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        KeyPair sender = KeyGeneration.generateDefaultEcKeyPair();
        processedBeforeTheStart(networkService, accountAgeRequest(profileId(sender)), sender.getPublic());
        Bisq1BridgeRequestService service = createStartableService(networkService, accountAgeGrpcService);

        service.initialize().join();
        try {
            // The service asks the bridge only if the profile of the request is the profile of the sender key
            verify(accountAgeGrpcService, timeout(5000)).verifyAndRequestAuthorization(any());
            verify(networkService).subscribeToConfidentialMessages(service);
        } finally {
            service.shutdown().join();
        }
    }

    // The service handles the account timestamp requests in onMessage, without the sender key. The replay passes the
    // processed messages to it as well, as for a new message.
    @Test
    void accountTimestampRequestWhichTheNodeProcessedBeforeTheStartIsProcessed() throws GeneralSecurityException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.publishAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        processedBeforeTheStart(networkService,
                accountTimestampV2Request(),
                KeyGeneration.generateDefaultEcKeyPair().getPublic());
        Bisq1BridgeRequestService service = createStartableService(networkService,
                mock(AccountAgeWitnessGrpcService.class));

        service.initialize().join();
        try {
            verify(networkService, timeout(5000)).publishAuthorizedData(
                    argThat(data -> data instanceof AuthorizedAccountTimestamp), any());
            verify(networkService).subscribeToConfidentialMessages(service);
        } finally {
            service.shutdown().join();
        }
    }

    // Anybody can send many account timestamp requests. They have their own queue, so they cannot drop the requests of
    // the other types (finding O-4, see docs/specifications/bonded-roles/oracle-requests.md).
    @Test
    void floodOfAccountTimestampRequestsDoesNotDropAnAccountAgeRequest() throws GeneralSecurityException {
        AccountAgeWitnessGrpcService accountAgeGrpcService = mock(AccountAgeWitnessGrpcService.class);
        when(accountAgeGrpcService.verifyAndRequestAuthorization(any()))
                .thenReturn(new AccountAgeWitnessOwnershipResponse(dateBucket(1_700_000_123_456L), new byte[32]));
        NetworkService networkService = mock(NetworkService.class);
        // The publication of an account timestamp blocks, so each account timestamp request keeps its thread
        CountDownLatch releaseAccountTimestamps = new CountDownLatch(1);
        when(networkService.publishAuthorizedData(any(), any())).thenAnswer(invocation -> {
            if (invocation.getArgument(0) instanceof AuthorizedAccountTimestamp) {
                releaseAccountTimestamps.await();
            }
            return CompletableFuture.completedFuture(new BroadcastResult());
        });
        Bisq1BridgeRequestService service = createStartableService(networkService, accountAgeGrpcService);
        service.initialize().join();
        try {
            AuthorizeAccountTimestampV2Request accountTimestampRequest = accountTimestampV2Request();
            // More requests than the 5 threads and the 20 places of the queue of an executor hold
            for (int i = 0; i < 25; i++) {
                service.onMessage(accountTimestampRequest);
            }
            KeyPair sender = KeyGeneration.generateDefaultEcKeyPair();
            service.onConfidentialMessage(accountAgeRequest(profileId(sender)), sender.getPublic());
            // A shared queue would drop the oldest waiting requests, at last the account age request
            for (int i = 0; i < 25; i++) {
                service.onMessage(accountTimestampRequest);
            }

            verify(accountAgeGrpcService, timeout(5000)).verifyAndRequestAuthorization(any());
        } finally {
            releaseAccountTimestamps.countDown();
            service.shutdown().join();
        }
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

    // The node reads all stores once at its start, before it creates this service, so the service reads its store
    // itself. Otherwise the oracle forgets its witness bindings at every restart, and binds a witness to a second
    // profile (see docs/specifications/reputation/account-age.md).
    @Test
    void witnessBindingOfAnEarlierRunPreventsABindingToAnotherProfile(@TempDir Path dataDir) {
        AuthorizeAccountAgeRequest first = accountAgeRequest("12".repeat(20));
        AuthorizeAccountAgeRequest conflicting = accountAgeRequest("34".repeat(20));
        Bisq1BridgeRequestService earlierRun = createStartableService(new PersistenceService(dataDir),
                mock(NetworkService.class),
                mock(AccountAgeWitnessGrpcService.class));
        assertThat(earlierRun.persistAccountAgeRequest(first)).isTrue();

        Bisq1BridgeRequestService service = createStartableService(new PersistenceService(dataDir),
                mock(NetworkService.class),
                mock(AccountAgeWitnessGrpcService.class));
        service.initialize().join();
        try {
            assertThat(service.getPersistableStore().getAccountAgeRequests()).containsExactly(first);
            assertThat(service.persistAccountAgeRequest(conflicting)).isFalse();
            assertThat(service.getPersistableStore().getAccountAgeRequests()).containsExactly(first);
        } finally {
            service.shutdown().join();
        }
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
        AuthorizedData malformedData = mock(AuthorizedData.class);
        when(malformedData.getAuthorizedDistributedData()).thenReturn(malformedRole);
        when(malformedData.getAuthorizedPublicKeyBytes())
                .thenReturn(authorizedKeyPair.getPublic().getEncoded());
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
    void skipsNetworkDataThatCannotBeReconstructedForRemoval() throws UnresolvableProtobufException {
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

    // While only static oracle nodes are allowed, only the static oracle node keys may act as an oracle node (see
    // docs/specifications/bonded-roles/authorized-data.md)
    @Test
    void oracleNodeRegistrationOfAKeyWhichIsNoStaticOracleNodeKeyIsRejectedBeforeBridgeVerification() {
        BondedRoleRegistrationRequest request = oracleNodeRequest(false);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.isOracleNodeKeyAllowed(request.getAuthorizedPublicKey())).thenReturn(false);
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                mock(NetworkService.class),
                authorizedBondedRolesService,
                bondedRoleGrpcService,
                mock(GrpcClient.class),
                true);
        useDirectExecutor(service);

        service.onConfidentialMessage(request, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService, never()).requestBondedRoleVerification(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void oracleNodeRegistrationOfAStaticOracleNodeKeyIsVerified() {
        BondedRoleRegistrationRequest request = oracleNodeRequest(false);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.of("stop after the key check")));
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.isOracleNodeKeyAllowed(request.getAuthorizedPublicKey())).thenReturn(true);
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                mock(NetworkService.class),
                authorizedBondedRolesService,
                bondedRoleGrpcService,
                mock(GrpcClient.class),
                true);
        useDirectExecutor(service);

        service.onConfidentialMessage(request, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
    }

    // The role of a node contains the addresses of the node. Nodes add the addresses of a seed node role to their seed
    // nodes (see docs/specifications/bonded-roles/registration.md).
    @Test
    void registrationOfANodeWithoutAddressesIsRejectedBeforeBridgeVerification() {
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.empty()));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.publishAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                networkService,
                mock(AuthorizedBondedRolesService.class),
                bondedRoleGrpcService);
        useDirectExecutor(service);

        service.onConfidentialMessage(seedNodeRequestWithoutAddresses(),
                KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService, never()).requestBondedRoleVerification(any(), any());
        verify(networkService, never()).publishAuthorizedData(any(), any());
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    // An oracle of an earlier version could persist the registration of a node without addresses. Nodes of this
    // version drop its role data, and this oracle cannot create the removal of it. A failed revalidation removes only
    // the registration, and the oracle still deactivates the other registrations.
    @Test
    void storedRegistrationWithInvalidRoleDataDoesNotStopTheDeactivationOfOthers()
            throws UnresolvableProtobufException {
        BondedRoleRegistrationRequest withoutAddresses = seedNodeRequestWithoutAddresses();
        BondedRoleRegistrationRequest other = createCurrentRequest(4321, "c".repeat(64));
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.of("invalid")),
                                new BondedRoleVerificationResponse(Optional.of("invalid")))));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService, mock(AuthorizedBondedRolesService.class), bondedRoleGrpcService,
                mock(GrpcClient.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().addAll(List.of(withoutAddresses, other));

        service.revalidateBondedRolesNow();

        // One removal: the one of the other registration
        verifyRemovalWithAuthorizedKeyPair(networkService, authorizedKeyPair);
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    // The registration of an oracle node role of a non-static key can still be cancelled
    @Test
    void knownOracleNodeRegistrationOfAKeyWhichIsNoStaticOracleNodeKeyCanBeCancelled() {
        BondedRoleRegistrationRequest registration = oracleNodeRequest(false);
        BondedRoleRegistrationRequest cancellation = oracleNodeRequest(true);
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleVerification(any(), any()))
                .thenReturn(new BondedRoleVerificationResponse(Optional.empty()));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
                networkService,
                mock(AuthorizedBondedRolesService.class),
                bondedRoleGrpcService,
                mock(GrpcClient.class),
                true);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(registration);
        useDirectExecutor(service);

        service.onConfidentialMessage(cancellation, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
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
        when(networkService.removeAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        Bisq1BridgeRequestService service = createService(KeyGeneration.generateDefaultEcKeyPair(),
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
        when(networkService.removeAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService, mock(AuthorizedBondedRolesService.class), bondedRoleGrpcService,
                mock(GrpcClient.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(registration);
        useDirectExecutor(service);

        service.onConfidentialMessage(cancellation, KeyGeneration.generateDefaultEcKeyPair().getPublic());

        verify(bondedRoleGrpcService).requestBondedRoleVerification(any(), any());
        verifyRemovalWithAuthorizedKeyPair(networkService, authorizedKeyPair);
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).isEmpty();
    }

    @Test
    void malformedBatchResponseRetainsEveryRegistration() throws UnresolvableProtobufException {
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
    void bridgeFailureRetainsEveryRegistration() throws UnresolvableProtobufException {
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
    void invalidBatchEntryRemovesOnlyItsExactRegistration() throws UnresolvableProtobufException {
        BondedRoleRegistrationRequest invalid = createCurrentRequest(1234, "b".repeat(64));
        BondedRoleRegistrationRequest valid = createCurrentRequest(4321, "c".repeat(64));
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.of("invalid")),
                                new BondedRoleVerificationResponse(Optional.empty()))));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.removeAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        Bisq1BridgeRequestService service = createService(authorizedKeyPair,
                networkService, mock(AuthorizedBondedRolesService.class), bondedRoleGrpcService,
                mock(GrpcClient.class));
        service.getPersistableStore().getBondedRoleRegistrationRequests().addAll(List.of(invalid, valid));

        service.revalidateBondedRolesNow();

        verifyRemovalWithAuthorizedKeyPair(networkService, authorizedKeyPair);
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests()).containsExactly(valid);
    }

    @Test
    void oneUnverifiableRegistrationDoesNotStopRevalidationOfTheRemainingOnes() throws UnresolvableProtobufException {
        BondedRoleRegistrationRequest valid = createCurrentRequest();
        // A record which passes its own structural validation but cannot be expressed as a bridge message. The
        // bridge has no way to find a bond without a bond user name, so it can never be verified.
        BondedRoleRegistrationRequest unverifiable = mock(BondedRoleRegistrationRequest.class);
        when(unverifiable.getRegistrationProtocolVersion())
                .thenReturn(BondedRoleRegistrationProtocol.CURRENT_VERSION);
        when(unverifiable.getBondUserName()).thenReturn("");
        when(unverifiable.getBondedRoleType()).thenReturn(BondedRoleType.MEDIATOR);
        when(unverifiable.getProfileId()).thenReturn("0123456789abcdef0123456789abcdef01234567");
        when(unverifiable.getSignatureBase64()).thenReturn("signature");
        when(unverifiable.getProposalTxId()).thenReturn(PROPOSAL_TX_ID);
        when(unverifiable.getLockupTxId()).thenReturn(LOCKUP_TX_ID);

        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.requestBondedRoleBatchVerification(any()))
                .thenReturn(new BondedRolesVerificationResponse(941001,
                        List.of(new BondedRoleVerificationResponse(Optional.empty()))));
        Bisq1BridgeRequestService service = createServiceWithBondedRoleGrpcService(bondedRoleGrpcService);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(unverifiable);
        service.getPersistableStore().getBondedRoleRegistrationRequests().add(valid);

        service.revalidateBondedRolesNow();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BondedRoleRegistrationRequest>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(bondedRoleGrpcService).requestBondedRoleBatchVerification(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).containsExactly(valid);
        assertThat(service.getPersistableStore().getBondedRoleRegistrationRequests())
                .containsExactlyInAnyOrder(unverifiable, valid);
    }

    @Test
    void batchAboveLimitRetainsRegistrationsWithoutCallingTheBridge() throws UnresolvableProtobufException {
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
    void rejectedRevalidationSubmissionDoesNotWedgeLaterTriggers() throws UnresolvableProtobufException {
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
    void sharedRequestExecutorRejectionDoesNotDiscardRevalidation() throws UnresolvableProtobufException {
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

    // The registration of a node contains the addresses of the node, as the desktop sends it
    private static BondedRoleRegistrationRequest oracleNodeRequest(boolean cancellation) {
        BondedRoleRegistrationRequest current = createCurrentRequest();
        return new BondedRoleRegistrationRequest(current.getProfileId(),
                current.getAuthorizedPublicKey(),
                BondedRoleType.ORACLE_NODE,
                current.getBondUserName(),
                current.getSignatureBase64(),
                Optional.of(current.getNetworkId().getAddressByTransportTypeMap()),
                current.getNetworkId(),
                cancellation,
                current.getRegistrationProtocolVersion(),
                current.getProposalTxId(),
                current.getLockupTxId());
    }

    // A modified client can send it. Released clients send the addresses of a node with its registration.
    private static BondedRoleRegistrationRequest seedNodeRequestWithoutAddresses() {
        BondedRoleRegistrationRequest current = createCurrentRequest();
        return new BondedRoleRegistrationRequest(current.getProfileId(),
                current.getAuthorizedPublicKey(),
                BondedRoleType.SEED_NODE,
                current.getBondUserName(),
                current.getSignatureBase64(),
                Optional.empty(),
                current.getNetworkId(),
                false,
                current.getRegistrationProtocolVersion(),
                current.getProposalTxId(),
                current.getLockupTxId());
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

    // An account timestamp request of an account created in Bisq 2, as AccountTimestampService creates it
    private static AuthorizeAccountTimestampV2Request accountTimestampV2Request() throws GeneralSecurityException {
        KeyPair accountKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] publicKey = accountKeyPair.getPublic().getEncoded();
        byte[] fingerprintHash = DigestUtil.hash("fingerprint".getBytes(StandardCharsets.UTF_8));
        byte[] saltedFingerprintHash = DigestUtil.hash(ByteArrayUtils.concat(fingerprintHash, new byte[32]));
        byte[] hash = DigestUtil.hash(ByteArrayUtils.concat(saltedFingerprintHash, publicKey));
        AuthorizeAccountTimestampV2Payload payload = new AuthorizeAccountTimestampV2Payload(System.currentTimeMillis(),
                hash,
                fingerprintHash,
                saltedFingerprintHash,
                publicKey);
        byte[] signature = SignatureUtil.sign(payload.toProto(true).toByteArray(),
                accountKeyPair.getPrivate(),
                SignatureUtil.SHA256withECDSA);
        return new AuthorizeAccountTimestampV2Request(payload, signature);
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

    // The authorized key owns the data. The entries which oracle nodes of earlier versions published with the key pair
    // of their network identity are removed by the migration at startup, not with each removal (see
    // docs/specifications/network/storage-ownership.md).
    private static void verifyRemovalWithAuthorizedKeyPair(NetworkService networkService, KeyPair authorizedKeyPair) {
        verify(networkService).removeAuthorizedData(any(),
                argThat((KeyPair keyPair) -> keyPair.getPublic().equals(authorizedKeyPair.getPublic())));
        verify(networkService, never()).removeAuthorizedData(any(), any(), any());
    }

    private static void useDirectExecutor(Bisq1BridgeRequestService service) {
        service.setExecutor(MoreExecutors.newDirectExecutorService());
    }

    // A service whose initialize() completes: the gRPC services of the bridge start and stop at once
    private static Bisq1BridgeRequestService createStartableService(NetworkService networkService,
                                                                    AccountAgeWitnessGrpcService accountAgeWitnessGrpcService) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<Bisq1BridgeRequestStore> persistence = mock(Persistence.class);
        when(persistenceService.<Bisq1BridgeRequestStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        return createStartableService(persistenceService, networkService, accountAgeWitnessGrpcService);
    }

    private static Bisq1BridgeRequestService createStartableService(PersistenceService persistenceService,
                                                                    NetworkService networkService,
                                                                    AccountAgeWitnessGrpcService accountAgeWitnessGrpcService) {
        when(accountAgeWitnessGrpcService.initialize()).thenReturn(CompletableFuture.completedFuture(true));
        when(accountAgeWitnessGrpcService.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
        SignedWitnessGrpcService signedWitnessGrpcService = mock(SignedWitnessGrpcService.class);
        when(signedWitnessGrpcService.initialize()).thenReturn(CompletableFuture.completedFuture(true));
        when(signedWitnessGrpcService.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
        BondedRoleGrpcService bondedRoleGrpcService = mock(BondedRoleGrpcService.class);
        when(bondedRoleGrpcService.initialize()).thenReturn(CompletableFuture.completedFuture(true));
        when(bondedRoleGrpcService.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new Bisq1BridgeRequestService(persistenceService,
                networkService,
                mock(AuthorizedBondedRolesService.class),
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic(),
                false,
                true,
                mock(AuthorizedOracleNode.class),
                mock(GrpcClient.class),
                bondedRoleGrpcService,
                accountAgeWitnessGrpcService,
                signedWitnessGrpcService);
    }

    // The node processed the message before the service started. The subscription passes it to the service as a new
    // message is passed: to onMessage and to onConfidentialMessage with the sender key.
    private static void processedBeforeTheStart(NetworkService networkService,
                                                EnvelopePayloadMessage message,
                                                PublicKey senderPublicKey) {
        doAnswer(invocation -> {
            ConfidentialMessageService.Listener listener = invocation.getArgument(0);
            listener.onMessage(message);
            listener.onConfidentialMessage(message, senderPublicKey);
            return null;
        }).when(networkService).subscribeToConfidentialMessages(any());
    }

    private static Bisq1BridgeRequestService createServiceWithBondedRoleGrpcService(
            BondedRoleGrpcService bondedRoleGrpcService) {
        return createServiceWithBondedRoleGrpcService(bondedRoleGrpcService, true);
    }

    private static Bisq1BridgeRequestService createServiceWithWitnessGrpcServices(
            AccountAgeWitnessGrpcService accountAgeWitnessGrpcService,
            SignedWitnessGrpcService signedWitnessGrpcService,
            NetworkService networkService) {
        return createServiceWithWitnessGrpcServices(accountAgeWitnessGrpcService,
                signedWitnessGrpcService,
                networkService,
                KeyGeneration.generateDefaultEcKeyPair());
    }

    private static Bisq1BridgeRequestService createServiceWithWitnessGrpcServices(
            AccountAgeWitnessGrpcService accountAgeWitnessGrpcService,
            SignedWitnessGrpcService signedWitnessGrpcService,
            NetworkService networkService,
            KeyPair authorizedKeyPair) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<Bisq1BridgeRequestStore> persistence = mock(Persistence.class);
        when(persistenceService.<Bisq1BridgeRequestStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        GrpcClient grpcClient = mock(GrpcClient.class);

        return new Bisq1BridgeRequestService(persistenceService,
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
                networkService,
                authorizedBondedRolesService,
                bondedRoleGrpcService,
                grpcClient,
                true);
    }

    private static Bisq1BridgeRequestService createService(KeyPair authorizedKeyPair,
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
