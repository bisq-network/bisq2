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

import bisq.bonded_roles.AuthorizedBondedRole;
import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.dto.BondedReputationDto;
import bisq.oracle_node.bisq1_bridge.dto.ProofOfBurnDto;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import bisq.security.SignatureUtil;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Bisq1BridgeService implements Service, MessageListener, PersistenceClient<Bisq1BridgeStore> {
    @Getter
    public static class Config {
        private final com.typesafe.config.Config httpService;

        public Config(com.typesafe.config.Config httpService) {
            this.httpService = httpService;
        }

        public static Bisq1BridgeService.Config from(com.typesafe.config.Config config) {
            return new Bisq1BridgeService.Config(config.getConfig("httpService"));
        }
    }

    @Getter
    private final Bisq1BridgeStore persistableStore = new Bisq1BridgeStore();
    @Getter
    private final Persistence<Bisq1BridgeStore> persistence;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Bisq1BridgeHttpService httpService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Setter
    private AuthorizedOracleNode authorizedOracleNode;
    @Nullable
    private Scheduler requestDoaDataScheduler, republishAuthorizedBondedRolesScheduler;

    public Bisq1BridgeService(Bisq1BridgeService.Config config,
                              NetworkService networkService,
                              PersistenceService persistenceService,
                              IdentityService identityService,
                              PrivateKey authorizedPrivateKey,
                              PublicKey authorizedPublicKey) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;

        Bisq1BridgeHttpService.Config httpServiceConfig = Bisq1BridgeHttpService.Config.from(config.getHttpService());
        httpService = new Bisq1BridgeHttpService(httpServiceConfig, networkService);

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return httpService.initialize()
                .whenComplete((result, throwable) -> {
                    networkService.addMessageListener(this);
                    requestDoaDataScheduler = Scheduler.run(this::requestDoaData).periodically(0, 5, TimeUnit.SECONDS);
                    republishAuthorizedBondedRolesScheduler = Scheduler.run(this::republishAuthorizedBondedRoles).after(5, TimeUnit.SECONDS);
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (requestDoaDataScheduler != null) {
            requestDoaDataScheduler.stop();
        }
        if (republishAuthorizedBondedRolesScheduler != null) {
            republishAuthorizedBondedRolesScheduler.stop();
        }
        networkService.removeMessageListener(this);
        return httpService.shutdown();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof AuthorizeAccountAgeRequest) {
            processAuthorizeAccountAgeRequest((AuthorizeAccountAgeRequest) networkMessage);
        } else if (networkMessage instanceof AuthorizeSignedWitnessRequest) {
            processAuthorizeSignedWitnessRequest((AuthorizeSignedWitnessRequest) networkMessage);
        } else if (networkMessage instanceof BondedRoleRegistrationRequest) {
            processBondedRoleRegistrationRequest((BondedRoleRegistrationRequest) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<List<ProofOfBurnDto>> requestProofOfBurnTxs() {
        return httpService.requestProofOfBurnTxs();
    }

    public CompletableFuture<List<BondedReputationDto>> requestBondedReputations() {
        return httpService.requestBondedReputations();
    }

    public CompletableFuture<Boolean> publishProofOfBurnDtoSet(List<ProofOfBurnDto> proofOfBurnList) {
        return CompletableFutureUtils.allOf(proofOfBurnList.stream()
                        .map(dto -> new AuthorizedProofOfBurnData(
                                dto.getAmount(),
                                dto.getTime(),
                                Hex.decode(dto.getHash())))
                        .map(this::publishAuthorizedData))
                .thenApply(results -> !results.contains(false));
    }

    public CompletableFuture<Boolean> publishBondedReputationDtoSet(List<BondedReputationDto> bondedReputationList) {
        return CompletableFutureUtils.allOf(bondedReputationList.stream()
                        .map(dto -> new AuthorizedBondedReputationData(
                                dto.getAmount(),
                                dto.getTime(),
                                Hex.decode(dto.getHash()),
                                dto.getLockTime()))
                        .map(this::publishAuthorizedData))
                .thenApply(results -> !results.contains(false));
    }


    public CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return identityService.createAndInitializeDefaultIdentity()
                .thenCompose(identity -> networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair(),
                        authorizedPrivateKey,
                        authorizedPublicKey))
                .thenApply(broadCastDataResult -> true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void republishAuthorizedBondedRoles() {
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedPayload()
                        .forEach(authenticatedData -> {
                            if (authenticatedData.getDistributedData() instanceof AuthorizedBondedRole) {
                                AuthorizedBondedRole authorizedBondedRole = (AuthorizedBondedRole) authenticatedData.getDistributedData();
                                if (authorizedBondedRole.getAuthorizedOracleNode().equals(authorizedOracleNode)) {
                                    publishAuthorizedData(authorizedBondedRole);
                                }

                            }
                        }));
    }

    private CompletableFuture<Boolean> requestDoaData() {
        return requestProofOfBurnTxs()
                .thenCompose(this::publishProofOfBurnDtoSet)
                .thenCompose(result -> requestBondedReputations())
                .thenCompose(this::publishBondedReputationDtoSet);
    }

    private void processAuthorizeAccountAgeRequest(AuthorizeAccountAgeRequest request) {
        long requestDate = request.getDate();
        String profileId = request.getProfileId();
        String hashAsHex = request.getHashAsHex();
        String messageString = profileId + hashAsHex + requestDate;
        byte[] message = messageString.getBytes(StandardCharsets.UTF_8);
        byte[] signature = Base64.getDecoder().decode(request.getSignatureBase64());
        try {
            String pubKeyBase64 = request.getPubKeyBase64();
            PublicKey publicKey = KeyGeneration.generatePublic(Base64.getDecoder().decode(pubKeyBase64), KeyGeneration.DSA);
            boolean isValid = SignatureUtil.verify(message,
                    signature,
                    publicKey,
                    SignatureUtil.SHA256withDSA);
            if (isValid) {
                httpService.requestAccountAgeWitness(hashAsHex)
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                result.ifPresentOrElse(date -> {
                                    if (date == requestDate) {
                                        persistableStore.getAccountAgeRequests().add(request);
                                        persist();
                                        AuthorizedAccountAgeData data = new AuthorizedAccountAgeData(profileId, requestDate);
                                        publishAuthorizedData(data);
                                    } else {
                                        log.warn("Date of account age for {} is not matching the date from the users request. " +
                                                        "Date from bridge service call: {}; Date from users request: {}",
                                                hashAsHex, date, requestDate);
                                    }
                                }, () -> {
                                    log.warn("Result of requestAccountAgeWitness returns empty optional. Request was: {}", request);
                                });
                            } else {
                                log.warn("Error at accountAgeService.findAccountAgeWitness", throwable);
                            }
                        });
            } else {
                log.warn("Signature verification for {} failed", request);
            }
        } catch (GeneralSecurityException e) {
            log.warn("Error at processAuthorizeAccountAgeRequest", e);
        }
    }

    private void processAuthorizeSignedWitnessRequest(AuthorizeSignedWitnessRequest request) {
        long witnessSignDate = request.getWitnessSignDate();
        if (witnessSignDate < 61) {
            log.warn("Age is not at least 60 days");
            return;
        }
        String messageString = request.getProfileId() + request.getHashAsHex() + request.getAccountAgeWitnessDate() + witnessSignDate;
        byte[] message = messageString.getBytes(StandardCharsets.UTF_8);
        byte[] signature = Base64.getDecoder().decode(request.getSignatureBase64());
        try {
            PublicKey publicKey = KeyGeneration.generatePublic(Base64.getDecoder().decode(request.getPubKeyBase64()), KeyGeneration.DSA);
            boolean isValid = SignatureUtil.verify(message,
                    signature,
                    publicKey,
                    SignatureUtil.SHA256withDSA);
            if (isValid) {
                httpService.requestSignedWitnessDate(request.getHashAsHex())
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                result.ifPresentOrElse(date -> {
                                    if (date == witnessSignDate) {
                                        persistableStore.getSignedWitnessRequests().add(request);
                                        persist();
                                        AuthorizedSignedWitnessData data = new AuthorizedSignedWitnessData(request.getProfileId(), request.getWitnessSignDate());
                                        publishAuthorizedData(data);
                                    } else {
                                        log.warn("Date of signed witness for {} is not matching the date from the users request. " +
                                                        "Date from bridge service call: {}; Date from users request: {}",
                                                request.getHashAsHex(), date, witnessSignDate);
                                    }
                                }, () -> {
                                    log.warn("Result of requestSignedWitnessDate returns empty optional. Request was: {}", request);
                                });
                            } else {
                                log.warn("Error at signedWitnessService.requestSignedWitnessDate", throwable);
                            }
                        });
            } else {
                log.warn("Signature verification for {} failed", request);
            }
        } catch (GeneralSecurityException e) {
            log.warn("Error at processAuthorizeSignedWitnessRequest", e);
        }
    }

    private void processBondedRoleRegistrationRequest(BondedRoleRegistrationRequest request) {
        BondedRoleType bondedRoleType = request.getBondedRoleType();
        String bisq1RoleTypeName = toBisq1RoleTypeName(bondedRoleType);
        String bondUserName = request.getBondUserName();
        String profileId = request.getProfileId();
        String signatureBase64 = request.getSignatureBase64();
        httpService.requestBondedRoleVerification(bondUserName, bisq1RoleTypeName, profileId, signatureBase64)
                .whenComplete((bondedRoleVerificationDto, throwable) -> {
                    if (throwable == null) {
                        if (bondedRoleVerificationDto.getErrorMessage() == null) {
                            AuthorizedBondedRole data = new AuthorizedBondedRole(profileId,
                                    request.getAuthorizedPublicKey(),
                                    bondedRoleType,
                                    bondUserName,
                                    signatureBase64,
                                    request.getAddressByNetworkType(),
                                    authorizedOracleNode);
                            publishAuthorizedData(data);
                        } else {
                            log.warn("RequestBondedRole failed. {}", bondedRoleVerificationDto.getErrorMessage());
                        }
                    } else {
                        log.warn("Error at accountAgeService.findAccountAgeWitness", throwable);
                    }
                });
    }

    private String toBisq1RoleTypeName(BondedRoleType bondedRoleType) {
        //todo switch
        String name = bondedRoleType.name();
        if (name.equals("MEDIATOR")) {
            return "MEDIATOR"; // 5k
        } else if (name.equals("ARBITRATOR")) {
            return "MOBILE_NOTIFICATIONS_RELAY_OPERATOR"; // 10k; Bisq 1 ARBITRATOR would require 100k! 
        } else if (name.equals("MODERATOR")) {
            return "YOUTUBE_ADMIN"; // 5k; repurpose unused role
        } else if (name.equals("SECURITY_MANAGER")) {
            return "BITCOINJ_MAINTAINER"; // 10k repurpose unused role
        } else if (name.equals("RELEASE_MANAGER")) {
            return "FORUM_ADMIN"; // 10k; repurpose unused role 
        } else if (name.equals("ORACLE_NODE")) {
            return "NETLAYER_MAINTAINER"; // 10k; repurpose unused role
        } else if (name.equals("SEED_NODE")) {
            return "SEED_NODE_OPERATOR"; // 10k
        } else if (name.equals("EXPLORER_NODE")) {
            return "BSQ_EXPLORER_OPERATOR"; // 10k; Explorer operator
        } else if (name.equals("MARKET_PRICE_NODE")) {
            return "DATA_RELAY_NODE_OPERATOR"; // 10k; price node
        }
        return name;
    }
}