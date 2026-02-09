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
import bisq.account.timestamp.AuthorizeAccountTimestampRequest;
import bisq.account.timestamp.AuthorizedAccountTimestamp;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.data.Result;
import bisq.common.threading.DiscardOldestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.oracle_node.bisq1_bridge.grpc.services.AccountAgeWitnessGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.AccountTimestampGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.BondedRoleGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.SignedWitnessGrpcService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Bisq1BridgeRequestService extends RateLimitedPersistenceClient<Bisq1BridgeRequestStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final Bisq1BridgeRequestStore persistableStore = new Bisq1BridgeRequestStore();
    @Getter
    private final Persistence<Bisq1BridgeRequestStore> persistence;
    private final BondedRoleGrpcService bondedRoleGrpcService;
    private final SignedWitnessGrpcService signedWitnessGrpcService;
    private final AccountAgeWitnessGrpcService accountAgeWitnessGrpcService;
    private final AccountTimestampGrpcService accountTimestampGrpcService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final AuthorizedOracleNode myAuthorizedOracleNode;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final boolean staticPublicKeysProvided;

    private ExecutorService executor;

    public Bisq1BridgeRequestService(PersistenceService persistenceService,
                                     IdentityService identityService,
                                     NetworkService networkService,
                                     AuthorizedBondedRolesService authorizedBondedRolesService,
                                     PrivateKey authorizedPrivateKey,
                                     PublicKey authorizedPublicKey,
                                     boolean staticPublicKeysProvided,
                                     AuthorizedOracleNode myAuthorizedOracleNode,
                                     GrpcClient grpcClient) {
        this.identityService = identityService;
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.myAuthorizedOracleNode = myAuthorizedOracleNode;

        accountAgeWitnessGrpcService = new AccountAgeWitnessGrpcService(grpcClient);
        accountTimestampGrpcService = new AccountTimestampGrpcService(grpcClient);
        signedWitnessGrpcService = new SignedWitnessGrpcService(grpcClient);
        bondedRoleGrpcService = new BondedRoleGrpcService(grpcClient);

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
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

        return accountAgeWitnessGrpcService.initialize()
                .thenCompose(result -> accountTimestampGrpcService.initialize())
                .thenCompose(result -> signedWitnessGrpcService.initialize())
                .thenCompose(result -> bondedRoleGrpcService.initialize())
                .thenApply(result -> {
                    networkService.addConfidentialMessageListener(this);
                    return result;
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeConfidentialMessageListener(this);

        ExecutorFactory.shutdownAndAwaitTermination(executor, 100);
        executor = null;
        return bondedRoleGrpcService.shutdown()
                .thenCompose(result -> signedWitnessGrpcService.shutdown())
                .thenCompose(result -> accountTimestampGrpcService.shutdown())
                .thenCompose(result -> accountAgeWitnessGrpcService.shutdown());
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof AuthorizeAccountAgeRequest request) {
            processAuthorizeAccountAgeRequest(request);
        } else if (envelopePayloadMessage instanceof AuthorizeSignedWitnessRequest request) {
            processAuthorizeSignedWitnessRequest(request);
        } else if (envelopePayloadMessage instanceof AuthorizeAccountTimestampRequest request) {
            processAuthorizeAccountTimestampRequest(request);
        }
    }

    @Override
    public void onConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PublicKey senderPublicKey) {
        if (envelopePayloadMessage instanceof BondedRoleRegistrationRequest request) {
            processBondedRoleRegistrationRequest(senderPublicKey, request);
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processAuthorizeAccountAgeRequest(AuthorizeAccountAgeRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                long date = accountAgeWitnessGrpcService.verifyAndRequestDate(request);

                persistableStore.getAccountAgeRequests().add(request);
                persist();

                publishAuthorizedData(new AuthorizedAccountAgeData(request.getProfileId(), date, staticPublicKeysProvided));
            } catch (Exception e) {
                log.error("processAuthorizeAccountAgeRequest failed", e);
            }
        }, executor);
    }

    private void processAuthorizeSignedWitnessRequest(AuthorizeSignedWitnessRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                long date = signedWitnessGrpcService.verifyAndRequestDate(request);

                persistableStore.getSignedWitnessRequests().add(request);
                persist();

                publishAuthorizedData(new AuthorizedSignedWitnessData(request.getProfileId(), date, staticPublicKeysProvided));
            } catch (Exception e) {
                log.error("processAuthorizeSignedWitnessRequest failed", e);
            }
        }, executor);
    }

    private void processAuthorizeAccountTimestampRequest(AuthorizeAccountTimestampRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                AccountTimestampService.verifyHash(request);
                AccountTimestampService.verifySignature(request);

                byte[] hash = request.getAccountTimestamp().getHash();
                ByteArray requestHash = new ByteArray(hash);
                Long persistedDate = persistableStore.getAccountTimestampDateByHash().get(requestHash);
                boolean hasPersistedTimestamp = persistedDate != null && persistedDate > 0;
                AccountTimestamp accountTimestamp;
                if (hasPersistedTimestamp) {
                    accountTimestamp = new AccountTimestamp(hash, persistedDate);
                } else {
                    switch (request.getTimestampType()) {
                        case BISQ2_NEW -> {
                            // Fresh request, we check if users date is inside a tolerance range of +- 2 hours.
                            // We tolerate future timestamps, as we need a deterministic date for supporting
                            // multiple oracle nodes. A future time does not provide a benefit to the user.
                            long maxTimeDrift = TimeUnit.HOURS.toMillis(2);
                            long date = request.getAccountTimestamp().getDate();
                            if (Math.abs(System.currentTimeMillis() - date) > maxTimeDrift) {
                                log.warn("AuthorizeAccountTimestampRequest is invalid, timestamp is too far from our current time");
                                return;
                            }
                            accountTimestamp = request.getAccountTimestamp();
                            persistAccountTimestamp(accountTimestamp);
                        }
                        case BISQ1_IMPORTED -> {
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
                            long accountCreationDate = request.getAccountTimestamp().getDate();
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
                        default -> throw new IllegalArgumentException("Unsupported timestamp type: " +
                                request.getTimestampType());
                    }
                }

                publishAuthorizedData(new AuthorizedAccountTimestamp(accountTimestamp, staticPublicKeysProvided));
            } catch (Exception e) {
                log.warn("AuthorizeAccountTimestampRequest is invalid", e);
            }
        }, executor);
    }

    private void persistAccountTimestamp(AccountTimestamp accountTimestamp) {
        ByteArray hash = new ByteArray(accountTimestamp.getHash());
        persistableStore.getAccountTimestampDateByHash().put(hash, accountTimestamp.getDate());
        persist();
    }

    private void processBondedRoleRegistrationRequest(PublicKey senderPublicKey,
                                                      BondedRoleRegistrationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("processBondedRoleRegistrationRequest {}", request);
                BondedRoleVerificationResponse response = bondedRoleGrpcService.requestBondedRoleVerification(request, senderPublicKey);
                log.info("BondedRoleVerificationResponse {}", response);
                if (response.getErrorMessage().isPresent()) {
                    log.warn("Request BondedRoleVerification from Bisq 1 failed with error message {}", response.getErrorMessage().get());
                    return;
                }

                AuthorizedBondedRole data = new AuthorizedBondedRole(request.getProfileId(),
                        request.getAuthorizedPublicKey(),
                        request.getBondedRoleType(),
                        request.getBondUserName(),
                        request.getSignatureBase64(),
                        request.getAddressByTransportTypeMap(),
                        request.getNetworkId(),
                        Optional.of(myAuthorizedOracleNode),
                        false);
                if (request.isCancellationRequest()) {
                    log.info("Remove authorizedBondedRole if matching data found");
                    authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                            .filter(authorizedBondedRole -> {
                                // We do not use authorizedBondedRole.equals(data) as that contains the networkId of the sender who did the registration.
                                // This can be the node itself, and can change when I2P address got added.
                                return authorizedBondedRole.getBondedRoleType() == data.getBondedRoleType() &&
                                        authorizedBondedRole.getProfileId().equals(data.getProfileId()) &&
                                        authorizedBondedRole.getBondUserName().equals(data.getBondUserName()) &&
                                        authorizedBondedRole.getSignatureBase64().equals(data.getSignatureBase64()) &&
                                        authorizedBondedRole.getAddressByTransportTypeMap().equals(data.getAddressByTransportTypeMap()) &&
                                        authorizedBondedRole.getAuthorizedPublicKey().equals(data.getAuthorizedPublicKey());
                            })
                            .forEach(authorizedBondedRole -> {
                                        log.info("Remove authorizedBondedRole {}", data);
                                        removeAuthorizedData(authorizedBondedRole)
                                                .whenComplete((broadcastResult, throwable) -> {
                                                    if (throwable == null) {
                                                        log.info("Tried to broadcast removeAuthorizedData message. Size of broadcastResult: {}", broadcastResult.size());
                                                    } else {
                                                        log.warn("Failed to broadcast removeAuthorizedData message");
                                                    }
                                                });
                                    }
                            );
                } else {
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
