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

import bisq.account.age_witness.AccountAgeWitness;
import bisq.account.age_witness.AuthorizeAccountAgeWitnessRequest;
import bisq.account.age_witness.AuthorizedAccountAgeWitness;
import bisq.account.age_witness.KeyAlgorithm;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.threading.DiscardOldestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.ByteArrayUtils;
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
import bisq.oracle_node.bisq1_bridge.grpc.services.BondedRoleGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.SignedWitnessGrpcService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class Bisq1BridgeRequestService extends RateLimitedPersistenceClient<Bisq1BridgeRequestStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final Bisq1BridgeRequestStore persistableStore = new Bisq1BridgeRequestStore();
    @Getter
    private final Persistence<Bisq1BridgeRequestStore> persistence;
    private final BondedRoleGrpcService bondedRoleGrpcService;
    private final SignedWitnessGrpcService signedWitnessGrpcService;
    private final AccountAgeWitnessGrpcService accountAgeWitnessGrpcService;
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
        } else if (envelopePayloadMessage instanceof AuthorizeAccountAgeWitnessRequest request) {
            processAuthorizeAccountAgeWitnessRequest(request);
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

    private void processAuthorizeAccountAgeWitnessRequest(AuthorizeAccountAgeWitnessRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                AccountAgeWitness accountAgeWitness = request.getAccountAgeWitness();
                if (request.getKeyAlgorithm() == KeyAlgorithm.DSA) {
                    // todo grpc request to bisq 1
                    //long date = accountAgeWitnessGrpcService.verifyAndRequestDate(request);
                } else {
                    long date = accountAgeWitness.getDate();
                    if (Math.abs(System.currentTimeMillis() - date) > TimeUnit.HOURS.toMillis(2)) {
                        log.warn("Date in request is outside tolerance. Date= {}", new Date(date));
                        return;
                    }

                    // Check if pubkey is matching the one in the hash
                    byte[] blindedAgeWitnessInputData = request.getBlindedAgeWitnessInputData();
                    byte[] publicKeyBytes = request.getPublicKey();
                    byte[] hashFromAccountPayload = ByteArrayUtils.concat(blindedAgeWitnessInputData, publicKeyBytes);
                    checkArgument(Arrays.equals(accountAgeWitness.getHash(), hashFromAccountPayload),
                            "AgeWitnessHash not matching hashFromAccountPayload");

                    KeyAlgorithm keyAlgorithm = request.getKeyAlgorithm();
                    PublicKey publicKey = KeyGeneration.generatePublic(publicKeyBytes, keyAlgorithm.getAlgorithm());
                    byte[] message = accountAgeWitness.toProto(true).toByteArray();
                    SignatureUtil.verify(message, request.getSignature(), publicKey, keyAlgorithm.getAlgorithm());
                }

                //todo persist
                //   persistableStore.getAccountAgeRequests().add(request);
                persist();

                publishAuthorizedData(new AuthorizedAccountAgeWitness(accountAgeWitness, staticPublicKeysProvided));
            } catch (Exception e) {
                log.error("processAuthorizeAccountAgeRequest failed", e);
            }
        }, executor);
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