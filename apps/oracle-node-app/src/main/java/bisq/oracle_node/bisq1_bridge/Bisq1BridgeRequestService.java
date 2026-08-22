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

import bisq.account.timestamp.AccountTimestamp;
import bisq.account.timestamp.AccountTimestampService;
import bisq.account.timestamp.AuthorizeAccountTimestampV1Payload;
import bisq.account.timestamp.AuthorizeAccountTimestampV1Request;
import bisq.account.timestamp.AuthorizeAccountTimestampV2Payload;
import bisq.account.timestamp.AuthorizeAccountTimestampV2Request;
import bisq.account.timestamp.AuthorizedAccountTimestamp;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.data.Result;
import bisq.common.encoding.Hex;
import bisq.common.threading.DiscardOldestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.services.AccountAgeWitnessGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.AccountTimestampGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.BondedRoleGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.SignedWitnessGrpcService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRolesVerificationRequest.MAX_REGISTRATIONS;

@Slf4j
public class Bisq1BridgeRequestService implements Service,
        PersistenceClient<Bisq1BridgeRequestStore>,
        ConfidentialMessageService.Listener,
        AuthorizedBondedRolesService.Listener {
    @Getter
    private final Bisq1BridgeRequestStore persistableStore = new Bisq1BridgeRequestStore();
    @Getter
    private final Persistence<Bisq1BridgeRequestStore> persistence;
    private final BondedRoleGrpcService bondedRoleGrpcService;
    private final AccountAgeWitnessGrpcService accountAgeWitnessGrpcService;
    private final SignedWitnessGrpcService signedWitnessGrpcService;
    private final AccountTimestampGrpcService accountTimestampGrpcService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final AuthorizedOracleNode myAuthorizedOracleNode;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final boolean staticPublicKeysProvided;
    private final boolean bondedRoleRegistrationEnabled;
    private final Object witnessRequestLock = new Object();
    private final Object bondedRoleRegistrationLock = new Object();
    private final AtomicBoolean revalidationInProgress = new AtomicBoolean();
    private final AtomicBoolean revalidationRequested = new AtomicBoolean();

    private ExecutorService executor;
    private ExecutorService revalidationExecutor;

    public Bisq1BridgeRequestService(PersistenceService persistenceService,
                                     IdentityService identityService,
                                     NetworkService networkService,
                                     AuthorizedBondedRolesService authorizedBondedRolesService,
                                     PrivateKey authorizedPrivateKey,
                                     PublicKey authorizedPublicKey,
                                     boolean staticPublicKeysProvided,
                                     boolean bondedRoleRegistrationEnabled,
                                     AuthorizedOracleNode myAuthorizedOracleNode,
                                     GrpcClient grpcClient) {
        this(persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided,
                bondedRoleRegistrationEnabled,
                myAuthorizedOracleNode,
                grpcClient,
                new BondedRoleGrpcService(grpcClient));
    }

    @VisibleForTesting
    Bisq1BridgeRequestService(PersistenceService persistenceService,
                              IdentityService identityService,
                              NetworkService networkService,
                              AuthorizedBondedRolesService authorizedBondedRolesService,
                              PrivateKey authorizedPrivateKey,
                              PublicKey authorizedPublicKey,
                              boolean staticPublicKeysProvided,
                              boolean bondedRoleRegistrationEnabled,
                              AuthorizedOracleNode myAuthorizedOracleNode,
                              GrpcClient grpcClient,
                              BondedRoleGrpcService bondedRoleGrpcService) {
        this(persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided,
                bondedRoleRegistrationEnabled,
                myAuthorizedOracleNode,
                grpcClient,
                bondedRoleGrpcService,
                new AccountAgeWitnessGrpcService(grpcClient),
                new SignedWitnessGrpcService(grpcClient));
    }

    @VisibleForTesting
    Bisq1BridgeRequestService(PersistenceService persistenceService,
                              IdentityService identityService,
                              NetworkService networkService,
                              AuthorizedBondedRolesService authorizedBondedRolesService,
                              PrivateKey authorizedPrivateKey,
                              PublicKey authorizedPublicKey,
                              boolean staticPublicKeysProvided,
                              boolean bondedRoleRegistrationEnabled,
                              AuthorizedOracleNode myAuthorizedOracleNode,
                              GrpcClient grpcClient,
                              BondedRoleGrpcService bondedRoleGrpcService,
                              AccountAgeWitnessGrpcService accountAgeWitnessGrpcService,
                              SignedWitnessGrpcService signedWitnessGrpcService) {
        this.identityService = identityService;
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.bondedRoleRegistrationEnabled = bondedRoleRegistrationEnabled;
        this.myAuthorizedOracleNode = myAuthorizedOracleNode;

        this.accountAgeWitnessGrpcService = accountAgeWitnessGrpcService;
        this.signedWitnessGrpcService = signedWitnessGrpcService;
        accountTimestampGrpcService = new AccountTimestampGrpcService(grpcClient);
        this.bondedRoleGrpcService = bondedRoleGrpcService;

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    @VisibleForTesting
    void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @VisibleForTesting
    void setRevalidationExecutor(ExecutorService revalidationExecutor) {
        this.revalidationExecutor = revalidationExecutor;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        int maxPoolSize = 5;
        String name = "Bisq1BridgeRequestService";
        int queueCapacity = 20;
        executor = ExecutorFactory.boundedCachedPool(name,
                1,
                maxPoolSize,
                10,
                queueCapacity,
                new DiscardOldestPolicy(name, queueCapacity, maxPoolSize)
        );
        revalidationExecutor = ExecutorFactory.newSingleThreadExecutor("BondedRoleRevalidation");

        return accountAgeWitnessGrpcService.initialize()
                .thenCompose(result -> signedWitnessGrpcService.initialize())
                .thenCompose(result -> accountTimestampGrpcService.initialize())
                .thenCompose(result -> bondedRoleGrpcService.initialize())
                .thenApply(result -> {
                    authorizedBondedRolesService.addListener(this);
                    networkService.addConfidentialMessageListener(this);
                    recoverRegistrationRequestsFromLoadedData();
                    revalidateBondedRoles();
                    return result;
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeConfidentialMessageListener(this);
        authorizedBondedRolesService.removeListener(this);

        ExecutorService currentRevalidationExecutor = revalidationExecutor;
        revalidationExecutor = null;
        ExecutorFactory.shutdownAndAwaitTermination(currentRevalidationExecutor, 100);
        ExecutorFactory.shutdownAndAwaitTermination(executor, 100);
        executor = null;
        return bondedRoleGrpcService.shutdown()
                .thenCompose(result -> accountTimestampGrpcService.shutdown())
                .thenCompose(result -> signedWitnessGrpcService.shutdown())
                .thenCompose(result -> accountAgeWitnessGrpcService.shutdown());
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof AuthorizeAccountTimestampV1Request request) {
            processAuthorizeAccountTimestampV1Request(request);
        } else if (envelopePayloadMessage instanceof AuthorizeAccountTimestampV2Request request) {
            processAuthorizeAccountTimestampV2Request(request);
        }
    }

    @Override
    public void onConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PublicKey senderPublicKey) {
        if (envelopePayloadMessage instanceof AuthorizeAccountAgeRequest request) {
            processAuthorizeAccountAgeRequest(senderPublicKey, request);
        } else if (envelopePayloadMessage instanceof AuthorizeSignedWitnessRequest request) {
            processAuthorizeSignedWitnessRequest(senderPublicKey, request);
        } else if (envelopePayloadMessage instanceof BondedRoleRegistrationRequest request) {
            processBondedRoleRegistrationRequest(senderPublicKey, request);
        }
    }


    /* --------------------------------------------------------------------- */
    // AuthorizedBondedRolesService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (!(authorizedData.getAuthorizedDistributedData() instanceof AuthorizedBondedRole authorizedBondedRole)) {
            return;
        }
        if (!authorizedBondedRole.canReconstructForRemoval()) {
            log.info("Skipping recovery of AuthorizedBondedRole data version {} because this oracle cannot reproduce its removal hash",
                    authorizedBondedRole.getVersion());
            return;
        }
        if (isStaticBootstrapRole(authorizedBondedRole) ||
                !Arrays.equals(authorizedData.getAuthorizedPublicKeyBytes(), authorizedPublicKey.getEncoded()) ||
                authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                        .noneMatch(authorizedBondedRole::equals)) {
            return;
        }

        BondedRoleRegistrationRequest request = toRegistrationRequest(authorizedBondedRole);
        if (!containsExactRegistrationRequest(request) && persistRegistrationRequest(request)) {
            log.info("Persisted bonded-role registration recovered from authorized network data: {}", request);
            revalidateBondedRoles();
        }
    }

    private boolean isStaticBootstrapRole(AuthorizedBondedRole role) {
        return staticPublicKeysProvided &&
                role.getRegistrationProtocolVersion() == BondedRoleRegistrationProtocol.LEGACY_VERSION &&
                role.getBondedRoleType() == BondedRoleType.ORACLE_NODE &&
                role.getProfileId().equals(myAuthorizedOracleNode.getProfileId()) &&
                role.getAuthorizedPublicKey().equals(myAuthorizedOracleNode.getAuthorizedPublicKey()) &&
                role.getBondUserName().equals(myAuthorizedOracleNode.getBondUserName()) &&
                role.getSignatureBase64().equals(myAuthorizedOracleNode.getSignatureBase64());
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void revalidateBondedRoles() {
        ExecutorService currentRevalidationExecutor = revalidationExecutor;
        if (currentRevalidationExecutor == null) {
            return;
        }

        revalidationRequested.set(true);
        if (!revalidationInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    while (revalidationRequested.getAndSet(false)) {
                        revalidateBondedRolesNow();
                    }
                } finally {
                    revalidationInProgress.set(false);
                    if (revalidationRequested.get()) {
                        revalidateBondedRoles();
                    }
                }
            }, currentRevalidationExecutor).exceptionally(throwable -> {
                log.warn("Bonded-role revalidation failed", throwable);
                return null;
            });
        } catch (RejectedExecutionException e) {
            // A concurrent shutdown can reject the submission after the executor null check. Clear the guard so
            // a later initialization or trigger can schedule revalidation again.
            revalidationInProgress.set(false);
            log.debug("Bonded-role revalidation was rejected because its executor is shutting down", e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    @VisibleForTesting
    void recoverRegistrationRequestsFromLoadedData() {
        // AuthorizedBondedRolesService.addListener replays stored data only when that service completed its own
        // initialization first. This explicit replay makes recovery independent of the initialization order and is
        // idempotent. Read the authorized wrappers here because their outer signing key is required for ownership.
        networkService.getDataService().ifPresent(dataService -> dataService.getAuthorizedData()
                .filter(authorizedData ->
                        authorizedData.getAuthorizedDistributedData() instanceof AuthorizedBondedRole)
                .forEach(this::onAuthorizedDataAdded));
    }

    private void processAuthorizeAccountAgeRequest(PublicKey senderPublicKey,
                                                   AuthorizeAccountAgeRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                if (request.getProtocolVersion() != AuthorizeAccountAgeRequest.CURRENT_VERSION) {
                    log.warn("Rejecting legacy or unsupported account age authorization protocol version {}",
                            request.getProtocolVersion());
                    return;
                }
                String senderProfileId = Hex.encode(DigestUtil.hash(senderPublicKey.getEncoded()));
                if (!request.getProfileId().equals(senderProfileId)) {
                    log.warn("Rejecting account age authorization whose profile does not match the confidential sender");
                    return;
                }
                if (hasWitnessClaimConflict(request.getHashAsHex(), request.getProfileId())) {
                    log.warn("Rejecting account age witness {} because it is already authorized for another profile",
                            request.getHashAsHex());
                    return;
                }
                var response = accountAgeWitnessGrpcService.verifyAndRequestAuthorization(request);
                if (!persistAccountAgeRequest(request)) {
                    log.warn("Failed to persist account age witness authorization for profile {}",
                            request.getProfileId());
                    return;
                }

                publishAuthorizedData(new AuthorizedAccountAgeData(request.getProfileId(),
                        response.getDateBucket(),
                        response.getWitnessNullifier(),
                        staticPublicKeysProvided));
            } catch (Exception e) {
                log.error("processAuthorizeAccountAgeRequest failed", e);
            }
        }, executor);
    }

    private boolean hasWitnessClaimConflict(String hashAsHex, String profileId) {
        boolean accountAgeConflict = persistableStore.getAccountAgeRequests().stream()
                .filter(existing -> existing.getProtocolVersion() == AuthorizeAccountAgeRequest.CURRENT_VERSION)
                .filter(existing -> existing.getHashAsHex().equalsIgnoreCase(hashAsHex))
                .anyMatch(existing -> !existing.getProfileId().equals(profileId));
        return accountAgeConflict || persistableStore.getSignedWitnessRequests().stream()
                .filter(existing -> existing.getProtocolVersion() == AuthorizeSignedWitnessRequest.CURRENT_VERSION)
                .filter(existing -> existing.getHashAsHex().equalsIgnoreCase(hashAsHex))
                .anyMatch(existing -> !existing.getProfileId().equals(profileId));
    }

    @VisibleForTesting
    boolean persistAccountAgeRequest(AuthorizeAccountAgeRequest request) {
        synchronized (witnessRequestLock) {
            if (hasWitnessClaimConflict(request.getHashAsHex(), request.getProfileId())) {
                return false;
            }
            boolean alreadyPersisted = persistableStore.getAccountAgeRequests().stream()
                    .filter(existing -> existing.getProtocolVersion() == AuthorizeAccountAgeRequest.CURRENT_VERSION)
                    .filter(existing -> existing.getHashAsHex().equalsIgnoreCase(request.getHashAsHex()))
                    .anyMatch(existing -> existing.getProfileId().equals(request.getProfileId()));
            if (alreadyPersisted) {
                return true;
            }

            persistableStore.getAccountAgeRequests().add(request);
            boolean persisted = persist().join();
            if (!persisted) {
                persistableStore.getAccountAgeRequests().remove(request);
            }
            return persisted;
        }
    }

    private void processAuthorizeSignedWitnessRequest(PublicKey senderPublicKey,
                                                       AuthorizeSignedWitnessRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                if (request.getProtocolVersion() != AuthorizeSignedWitnessRequest.CURRENT_VERSION) {
                    log.warn("Rejecting legacy or unsupported signed-witness authorization protocol version {}",
                            request.getProtocolVersion());
                    return;
                }
                String senderProfileId = Hex.encode(DigestUtil.hash(senderPublicKey.getEncoded()));
                if (!request.getProfileId().equals(senderProfileId)) {
                    log.warn("Rejecting signed-witness authorization whose profile does not match the confidential sender");
                    return;
                }
                if (hasWitnessClaimConflict(request.getHashAsHex(), request.getProfileId())) {
                    log.warn("Rejecting signed witness {} because it is already authorized for another profile",
                            request.getHashAsHex());
                    return;
                }
                var response = signedWitnessGrpcService.verifyAndRequestAuthorization(request);
                if (!persistSignedWitnessRequest(request)) {
                    log.warn("Failed to persist signed-witness authorization for profile {}", request.getProfileId());
                    return;
                }

                publishAuthorizedData(new AuthorizedSignedWitnessData(request.getProfileId(),
                        response.getDateBucket(),
                        response.getWitnessNullifier(),
                        staticPublicKeysProvided));
            } catch (Exception e) {
                log.error("processAuthorizeSignedWitnessRequest failed", e);
            }
        }, executor);
    }

    @VisibleForTesting
    boolean persistSignedWitnessRequest(AuthorizeSignedWitnessRequest request) {
        synchronized (witnessRequestLock) {
            if (hasWitnessClaimConflict(request.getHashAsHex(), request.getProfileId())) {
                return false;
            }
            boolean alreadyPersisted = persistableStore.getSignedWitnessRequests().stream()
                    .filter(existing -> existing.getProtocolVersion() == AuthorizeSignedWitnessRequest.CURRENT_VERSION)
                    .filter(existing -> existing.getHashAsHex().equalsIgnoreCase(request.getHashAsHex()))
                    .anyMatch(existing -> existing.getProfileId().equals(request.getProfileId()));
            if (alreadyPersisted) {
                return true;
            }

            persistableStore.getSignedWitnessRequests().add(request);
            boolean persisted = persist().join();
            if (!persisted) {
                persistableStore.getSignedWitnessRequests().remove(request);
            }
            return persisted;
        }
    }

    private void processBondedRoleRegistrationRequest(PublicKey senderPublicKey,
                                                      BondedRoleRegistrationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("processBondedRoleRegistrationRequest {}", request);
                int protocolVersion = request.getRegistrationProtocolVersion();
                if (!BondedRoleRegistrationProtocol.isSupported(protocolVersion)) {
                    log.warn("Ignoring unsupported bonded-role registration protocol version {}", protocolVersion);
                    return;
                }
                if (protocolVersion == BondedRoleRegistrationProtocol.CURRENT_VERSION &&
                        !request.isCancellationRequest() &&
                        !bondedRoleRegistrationEnabled) {
                    log.warn("Rejecting bound bonded-role registration because oracle admission is disabled");
                    return;
                }
                BondedRoleRegistrationProtocol.verifyProof(protocolVersion,
                        request.getProposalTxId(), request.getLockupTxId());
                boolean knownRegistration = hasMatchingRegistrationRequest(request);
                if (protocolVersion == BondedRoleRegistrationProtocol.LEGACY_VERSION &&
                        !knownRegistration) {
                    log.warn("Rejecting an unknown legacy bonded-role registration after rollout: {}", request);
                    return;
                }
                if (request.isCancellationRequest() && !knownRegistration) {
                    log.info("No matching persisted bonded-role registration found for cancellation request {}", request);
                    return;
                }

                BondedRoleVerificationResponse response = bondedRoleGrpcService.requestBondedRoleVerification(request, senderPublicKey);
                log.info("BondedRoleVerificationResponse {}", response);
                if (response.getErrorMessage().isPresent()) {
                    log.warn("Request BondedRoleVerification from Bisq 1 failed with error message {}", response.getErrorMessage().get());
                    return;
                }

                if (request.isCancellationRequest()) {
                    cancelRegistration(request);
                } else {
                    if (!persistRegistrationRequest(request)) {
                        log.warn("Failed to persist bonded-role registration request: {}", request);
                        return;
                    }

                    AuthorizedBondedRole data = toAuthorizedBondedRole(request);
                    log.info("Publish authorizedBondedRole {}", data);
                    publishAuthorizedData(data)
                            .whenComplete((broadcastResult, throwable) -> {
                                if (throwable == null) {
                                    log.info("Tried to broadcast authorizedBondedRole message. Size of broadcastResult: {}", broadcastResult.size());
                                } else {
                                    log.warn("Failed to broadcast authorizedBondedRole message");
                                }
                            });
                }
            } catch (Exception e) {
                log.error("Request BondedRoleVerification failed", e);
            }
        }, executor);
    }

    @VisibleForTesting
    void revalidateBondedRolesNow() {
        List<BondedRoleRegistrationRequest> requests = persistableStore.getBondedRoleRegistrationRequests().stream()
                .filter(request -> BondedRoleRegistrationProtocol.isSupported(request.getRegistrationProtocolVersion()))
                .toList();
        int unsupportedRequestCount = persistableStore.getBondedRoleRegistrationRequests().size() - requests.size();
        if (unsupportedRequestCount > 0) {
            log.warn("Retaining {} bonded-role registrations with unsupported protocol versions without revalidation",
                    unsupportedRequestCount);
        }
        if (requests.isEmpty()) {
            return;
        }
        if (requests.size() > MAX_REGISTRATIONS) {
            log.error("Cannot revalidate {} bonded-role registrations because the bridge batch limit is {}. " +
                            "Retaining all registrations until a multi-batch snapshot policy is defined.",
                    requests.size(), MAX_REGISTRATIONS);
            return;
        }

        try {
            BondedRolesVerificationResponse response = bondedRoleGrpcService.requestBondedRoleBatchVerification(requests);
            List<BondedRoleVerificationResponse> verifications = response.getVerifications();
            if (verifications.size() != requests.size()) {
                log.warn("Ignoring malformed bonded-role batch response at DAO height {}: expected {} results but received {}",
                        response.getDaoStateBlockHeight(), requests.size(), verifications.size());
                return;
            }

            log.info("Revalidated {} bonded-role registrations at DAO height {}",
                    requests.size(), response.getDaoStateBlockHeight());
            for (int i = 0; i < requests.size(); i++) {
                BondedRoleVerificationResponse verification = verifications.get(i);
                if (verification.getErrorMessage().isPresent()) {
                    BondedRoleRegistrationRequest request = requests.get(i);
                    log.warn("Deactivate invalid bonded-role registration {}. Reason: {}",
                            request, verification.getErrorMessage().get());
                    deactivateRegistration(request);
                }
            }
        } catch (Exception e) {
            // Connectivity loss is deliberately not a fail-closed condition. The next live block or a later
            // subscription retry triggers another authoritative snapshot verification.
            log.warn("Could not revalidate bonded-role registrations against the Bisq 1 bridge", e);
        }
    }

    private void cancelRegistration(BondedRoleRegistrationRequest cancellationRequest) {
        List<BondedRoleRegistrationRequest> matchingRequests = findMatchingRegistrationRequests(cancellationRequest);
        if (matchingRequests.isEmpty()) {
            log.info("No matching persisted bonded-role registration found for cancellation request {}", cancellationRequest);
            return;
        }
        matchingRequests.forEach(this::deactivateRegistration);
    }

    private void deactivateRegistration(BondedRoleRegistrationRequest request) {
        AuthorizedBondedRole data = toAuthorizedBondedRole(request);
        removeAuthorizedData(data).whenComplete((broadcastResult, throwable) -> {
            if (throwable == null) {
                log.info("Tried to broadcast bonded-role removal. Size of broadcastResult: {}", broadcastResult.size());
                removeRegistrationRequest(request);
            } else {
                log.warn("Failed to broadcast bonded-role removal for {}", request, throwable);
            }
        });
    }

    private AuthorizedBondedRole toAuthorizedBondedRole(BondedRoleRegistrationRequest request) {
        return new AuthorizedBondedRole(request.getProfileId(),
                request.getAuthorizedPublicKey(),
                request.getBondedRoleType(),
                request.getBondUserName(),
                request.getSignatureBase64(),
                request.getAddressByTransportTypeMap(),
                request.getNetworkId(),
                Optional.of(myAuthorizedOracleNode),
                false,
                request.getRegistrationProtocolVersion(),
                request.getProposalTxId(),
                request.getLockupTxId());
    }

    private BondedRoleRegistrationRequest toRegistrationRequest(AuthorizedBondedRole role) {
        return new BondedRoleRegistrationRequest(role.getProfileId(),
                role.getAuthorizedPublicKey(),
                role.getBondedRoleType(),
                role.getBondUserName(),
                role.getSignatureBase64(),
                role.getAddressByTransportTypeMap(),
                role.getNetworkId(),
                false,
                role.getRegistrationProtocolVersion(),
                role.getProposalTxId(),
                role.getLockupTxId());
    }

    private boolean persistRegistrationRequest(BondedRoleRegistrationRequest request) {
        synchronized (bondedRoleRegistrationLock) {
            if (request.isCancellationRequest()) {
                throw new IllegalArgumentException("A cancellation request must not be persisted as a registration");
            }
            if (persistableStore.getBondedRoleRegistrationRequests().contains(request)) {
                return true;
            }
            persistableStore.getBondedRoleRegistrationRequests().add(request);
            boolean persisted = persist().join();
            if (!persisted) {
                persistableStore.getBondedRoleRegistrationRequests().remove(request);
            }
            return persisted;
        }
    }

    private boolean containsExactRegistrationRequest(BondedRoleRegistrationRequest request) {
        return persistableStore.getBondedRoleRegistrationRequests().contains(request);
    }

    @VisibleForTesting
    void removeRegistrationRequest(BondedRoleRegistrationRequest request) {
        synchronized (bondedRoleRegistrationLock) {
            if (!persistableStore.getBondedRoleRegistrationRequests().remove(request)) {
                return;
            }
            if (!persist().join()) {
                persistableStore.getBondedRoleRegistrationRequests().add(request);
            }
        }
    }

    private boolean hasMatchingRegistrationRequest(BondedRoleRegistrationRequest request) {
        return persistableStore.getBondedRoleRegistrationRequests().stream()
                .anyMatch(existing -> registrationsMatch(existing, request));
    }

    private List<BondedRoleRegistrationRequest> findMatchingRegistrationRequests(BondedRoleRegistrationRequest request) {
        return persistableStore.getBondedRoleRegistrationRequests().stream()
                .filter(existing -> registrationsMatch(existing, request))
                .toList();
    }

    static boolean registrationsMatch(BondedRoleRegistrationRequest first,
                                      BondedRoleRegistrationRequest second) {
        return first.getBondedRoleType() == second.getBondedRoleType() &&
                first.getProfileId().equals(second.getProfileId()) &&
                first.getAuthorizedPublicKey().equals(second.getAuthorizedPublicKey()) &&
                first.getBondUserName().equals(second.getBondUserName()) &&
                first.getSignatureBase64().equals(second.getSignatureBase64()) &&
                first.getRegistrationProtocolVersion() == second.getRegistrationProtocolVersion() &&
                first.getProposalTxId().equals(second.getProposalTxId()) &&
                first.getLockupTxId().equals(second.getLockupTxId());
    }

    private void processAuthorizeAccountTimestampV1Request(AuthorizeAccountTimestampV1Request request) {
        CompletableFuture.runAsync(() -> {
            try {
                AuthorizeAccountTimestampV1Payload payload = request.getPayload();
                AccountTimestampService.verifyHashV1(payload);
                AccountTimestampService.verifySignatureV1(request);

                byte[] hash = payload.getHash();
                Long persistedDate = persistableStore.getAccountTimestampDateByHash().get(new ByteArray(hash));
                boolean hasPersistedTimestamp = persistedDate != null && persistedDate > 0;
                AccountTimestamp accountTimestamp;
                if (hasPersistedTimestamp) {
                    accountTimestamp = new AccountTimestamp(hash, persistedDate);
                } else {
                    // Fresh request, we look up the account age witness from Bisq 1
                    Result<Long> result = accountTimestampGrpcService.requestAccountTimestamp(hash);
                    if (result.isFailure()) {
                        log.error("requestAccountTimestamp from Bisq 1 failed", result.exceptionOrNull());
                        return;
                    }

                    // The date we get from the request is the account creation date from the imported account.
                    // The account age is the date of the account age witness object.
                    // It is expected that there is a slight difference in those 2 dates.
                    long dateFromBisq1AccountAge = result.getOrThrow();
                    long accountCreationDate = payload.getDate();
                    long ageDiff = dateFromBisq1AccountAge - accountCreationDate;
                    if (dateFromBisq1AccountAge > accountCreationDate) {
                        log.warn("The account age is newer then the account creation date. " +
                                        "This could be due out of sync clock at user who created the account. " +
                                        "dateFromBisq1AccountAge={}; accountCreationDate={}",
                                new Date(dateFromBisq1AccountAge), new Date(accountCreationDate));
                    }
                    if (ageDiff > TimeUnit.HOURS.toMillis(1)) {
                        log.warn("The account creation date is more then 1 hour different to the account age date. " +
                                        "This is probably because the account was created on Bisq 1 before the " +
                                        "account age feature was implemented or there was some unusual delay when publishing the account age. " +
                                        "ageDiff={} sec; dateFromBisq1AccountAge={}; accountCreationDate={}",
                                ageDiff / 1000, new Date(dateFromBisq1AccountAge), new Date(accountCreationDate));
                    }

                    accountTimestamp = new AccountTimestamp(hash, dateFromBisq1AccountAge);
                    persistAccountTimestamp(accountTimestamp);
                }

                publishAuthorizedData(new AuthorizedAccountTimestamp(accountTimestamp, staticPublicKeysProvided));
            } catch (Exception e) {
                log.warn("AuthorizeAccountTimestampV1Request is invalid", e);
            }
        }, executor);
    }


    private void processAuthorizeAccountTimestampV2Request(AuthorizeAccountTimestampV2Request request) {
        CompletableFuture.runAsync(() -> {
            try {
                AuthorizeAccountTimestampV2Payload payload = request.getPayload();
                AccountTimestampService.verifyHashV2(payload);
                AccountTimestampService.verifySignatureV2(request);

                byte[] hash = payload.getHash();
                Long persistedDate = persistableStore.getAccountTimestampDateByHash().get(new ByteArray(hash));
                boolean hasPersistedTimestamp = persistedDate != null && persistedDate > 0;
                AccountTimestamp accountTimestamp;
                if (hasPersistedTimestamp) {
                    accountTimestamp = new AccountTimestamp(hash, persistedDate);
                } else {
                    // Fresh request, we check if users date is inside a tolerance range of +- 2 hours.
                    // We also do not tolerate too far future timestamps, as we need a deterministic date for supporting
                    // multiple oracle nodes.
                    long maxTimeDrift = TimeUnit.HOURS.toMillis(2);
                    long date = payload.getDate();
                    if (Math.abs(System.currentTimeMillis() - date) > maxTimeDrift) {
                        log.warn("AuthorizeAccountTimestampV2Request is invalid, timestamp is too far from our current time." +
                                "date={}, now={}", new Date(date), new Date(System.currentTimeMillis()));
                        return;
                    }
                    accountTimestamp = new AccountTimestamp(hash, date);
                    persistAccountTimestamp(accountTimestamp);
                }

                publishAuthorizedData(new AuthorizedAccountTimestamp(accountTimestamp, staticPublicKeysProvided));
            } catch (Exception e) {
                log.warn("AuthorizeAccountTimestampV2Request is invalid", e);
            }
        }, executor);
    }

    private void persistAccountTimestamp(AccountTimestamp accountTimestamp) {
        ByteArray hash = new ByteArray(accountTimestamp.getHash());
        persistableStore.getAccountTimestampDateByHash().put(hash, accountTimestamp.getDate());
        persist();
    }

    private CompletableFuture<BroadcastResult> publishAuthorizedData(AuthorizedDistributedData data) {
        Identity identity = identityService.getOrCreateDefaultIdentity();
        return networkService.publishAuthorizedData(data,
                identity.getNetworkIdWithKeyPair().getKeyPair(),
                authorizedPrivateKey,
                authorizedPublicKey);
    }

    private CompletableFuture<BroadcastResult> removeAuthorizedData(AuthorizedBondedRole authorizedDistributedData) {
        Identity identity = identityService.getOrCreateDefaultIdentity();
        return networkService.removeAuthorizedData(authorizedDistributedData,
                identity.getNetworkIdWithKeyPair().getKeyPair(),
                authorizedPublicKey);
    }


}
