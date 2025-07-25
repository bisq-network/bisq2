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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.platform.MemoryReportService;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.ThreadName;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.dto.BondedReputationDto;
import bisq.oracle_node.bisq1_bridge.dto.ProofOfBurnDto;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class Bisq1BridgeService implements Service, ConfidentialMessageService.Listener,
        AuthorizedBondedRolesService.Listener, PersistenceClient<Bisq1BridgeStore> {
    // Currently as July 2025 we have about 150 messages we republish at startup.
    // To avoid stress to the network we throttle publishing rate for proofOfBurn and bondedReputation data.
    // With 4 seconds pause we get about 10 minutes until all data is published.
    private static final long THROTTLE_DELAY = 4000;
    public static ExecutorService EXECUTOR;

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
    private final Bisq1BridgeHttpService httpService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MemoryReportService memoryReportService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final boolean ignoreSecurityManager;
    private final boolean staticPublicKeysProvided;

    @Setter
    private AuthorizedOracleNode authorizedOracleNode;
    @Setter
    private Identity identity;

    @Nullable
    private Scheduler requestDoaDataScheduler, initialDelayScheduler;

    public Bisq1BridgeService(Config config,
                              NetworkService networkService,
                              PersistenceService persistenceService,
                              AuthorizedBondedRolesService authorizedBondedRolesService,
                              MemoryReportService memoryReportService,
                              PrivateKey authorizedPrivateKey,
                              PublicKey authorizedPublicKey,
                              boolean ignoreSecurityManager,
                              boolean staticPublicKeysProvided) {
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;
        this.ignoreSecurityManager = ignoreSecurityManager;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.memoryReportService = memoryReportService;

        Bisq1BridgeHttpService.Config httpServiceConfig = Bisq1BridgeHttpService.Config.from(config.getHttpService());
        httpService = new Bisq1BridgeHttpService(httpServiceConfig, networkService);

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        EXECUTOR = ExecutorFactory.newSingleThreadExecutor("Bisq1BridgeService");
        return httpService.initialize()
                .whenComplete((result, throwable) -> {
                    networkService.getConfidentialMessageServices().stream()
                            .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                            .forEach(this::onMessage);
                    networkService.addConfidentialMessageListener(this);
                    authorizedBondedRolesService.addListener(this);

                    int delay = DevMode.isDevMode() ? 1 : 60;
                    initialDelayScheduler = Scheduler.run(this::initialRepublish)
                            .host(this)
                            .runnableName("initialRepublish")
                            .after(delay, TimeUnit.SECONDS);
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (requestDoaDataScheduler != null) {
            requestDoaDataScheduler.stop();
            requestDoaDataScheduler = null;
        }
        if (initialDelayScheduler != null) {
            initialDelayScheduler.stop();
            initialDelayScheduler = null;
        }

        ExecutorFactory.shutdownAndAwaitTermination(EXECUTOR, 100);

        networkService.removeConfidentialMessageListener(this);
        authorizedBondedRolesService.removeListener(this);
        return httpService.shutdown();
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof AuthorizeAccountAgeRequest) {
            processAuthorizeAccountAgeRequest((AuthorizeAccountAgeRequest) envelopePayloadMessage);
        } else if (envelopePayloadMessage instanceof AuthorizeSignedWitnessRequest) {
            processAuthorizeSignedWitnessRequest((AuthorizeSignedWitnessRequest) envelopePayloadMessage);
        }
    }

    @Override
    public void onConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PublicKey senderPublicKey) {
        if (envelopePayloadMessage instanceof BondedRoleRegistrationRequest) {
            processBondedRoleRegistrationRequest((BondedRoleRegistrationRequest) envelopePayloadMessage, senderPublicKey);
        }
    }


    /* --------------------------------------------------------------------- */
    // AuthorizedBondedRolesService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data instanceof AuthorizedAlertData authorizedAlertData) {
            if (authorizedAlertData.getAlertType() == AlertType.BAN &&
                    isAuthorized(authorizedData) &&
                    authorizedAlertData.getBannedRole().isPresent()) {
                BondedRoleType bannedBondedRoleType = authorizedAlertData.getBannedRole().get().getBondedRoleType();
                authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                        .filter(authorizedBondedRole -> authorizedBondedRole.getBondedRoleType() == bannedBondedRoleType)
                        .forEach(bannedRole -> {
                            if (ignoreSecurityManager) {
                                log.warn("We received an alert message from the security manager to ban a bonded role but " +
                                                "you have set ignoreSecurityManager to true, so we do not remove the role data from the network.\n" +
                                                "bannedRole={}\nauthorizedData sent by security manager={}",
                                        bannedRole, authorizedData);
                            } else {
                                //todo (Critical)
                                //removeAuthorizedData(...);
                            }
                        });
            }
        }
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void initialRepublish() {
        log.info("Start republishAuthorizedBondedRoles");
        republishAuthorizedBondedRoles();
        log.info("Completed republishAuthorizedBondedRoles");

        log.info("Start request and publish DaoData");
        requestDaoData().join();
        memoryReportService.logReport();
        log.info("Completed request and publish DaoData");

        startRequestDoaDataScheduler();
    }

    private void startRequestDoaDataScheduler() {
        if (requestDoaDataScheduler != null) {
            requestDoaDataScheduler.stop();
        }
        requestDoaDataScheduler = Scheduler.run(this::periodicRepublish)
                .host(this)
                .runnableName("periodicRepublish")
                .after(5, TimeUnit.SECONDS);
    }

    private void periodicRepublish() {
        log.info("periodicRequestDoaDataScheduler: Start requestDoaData");
        requestDaoData().join(); // As we use 4 second throttle this can take quite some time at first request when we get all data.
        startRequestDoaDataScheduler();
        log.info("periodicRequestDoaDataScheduler: Completed requestDoaData");
    }

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.SECURITY_MANAGER);
    }

    private CompletableFuture<List<ProofOfBurnDto>> requestProofOfBurnTxs() {
        log.info("requestProofOfBurnTxs");
        try {
            return httpService.requestProofOfBurnTxs();
        } catch (Exception e) {
            log.error("requestProofOfBurnTxs failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<List<BondedReputationDto>> requestBondedReputations() {
        log.info("requestBondedReputations");
        try {
            return httpService.requestBondedReputations();
        } catch (Exception e) {
            log.error("requestBondedReputations failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Boolean> publishProofOfBurnDtoSet(List<ProofOfBurnDto> proofOfBurnList) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                ThreadName.from("publishProofOfBurnDtoSet");
                log.info("publishProofOfBurnDtoSet: proofOfBurnList={}", proofOfBurnList);
                Stream<AuthorizedProofOfBurnData> stream = proofOfBurnList.stream()
                        .map(dto -> new AuthorizedProofOfBurnData(
                                dto.getBlockTime(),
                                dto.getAmount(),
                                Hex.decode(dto.getHash()),
                                dto.getBlockHeight(),
                                dto.getTxId(),
                                staticPublicKeysProvided));
                return CompletableFutureUtils.allOf(stream
                                .map(data -> publishAuthorizedData(data, THROTTLE_DELAY)))
                        .whenComplete((r, t) -> {
                            if (t != null) {
                                log.error("publishProofOfBurnDtoSet failed", t);
                            }
                        })
                        .thenApply(results -> {
                            boolean b = !results.contains(false);
                            return b;
                        })
                        .join();
            }, EXECUTOR);
        } catch (Exception e) {
            log.error("publishProofOfBurnDtoSet failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Boolean> publishBondedReputationDtoSet(List<BondedReputationDto> bondedReputationList) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                ThreadName.from("publishBondedReputationDtoSet");
                log.info("publishBondedReputationDtoSet: bondedReputationList={}", bondedReputationList);
                Stream<AuthorizedBondedReputationData> stream = bondedReputationList.stream()
                        .map(dto -> new AuthorizedBondedReputationData(
                                dto.getBlockTime(),
                                dto.getAmount(),
                                Hex.decode(dto.getHash()),
                                dto.getLockTime(),
                                dto.getBlockHeight(),
                                dto.getTxId(),
                                staticPublicKeysProvided));
                return CompletableFutureUtils.allOf(stream
                                .map(data -> publishAuthorizedData(data, THROTTLE_DELAY)))
                        .thenApply(results -> {
                            boolean b = !results.contains(false);
                            return b;
                        })
                        .join();
            }, EXECUTOR);
        } catch (Exception e) {
            log.error("publishBondedReputationDtoSet failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return publishAuthorizedData(data, 0L);
    }

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data, long throttleDelay) {
        try {
            return networkService.publishAuthorizedData(data,
                            identity.getNetworkIdWithKeyPair().getKeyPair(),
                            authorizedPrivateKey,
                            authorizedPublicKey)
                    .thenApply(broadCastDataResult -> {
                        int numSuccess = broadCastDataResult.stream()
                                .mapToInt(e -> {
                                    try {
                                        e.join();
                                        return 1;
                                    } catch (Exception ex) {
                                        return 0;
                                    }
                                })
                                .sum();
                        if (throttleDelay > 0) {
                            try {
                                Thread.sleep(throttleDelay);
                            } catch (InterruptedException ignore) {
                            }
                        }
                        return numSuccess == broadCastDataResult.size();
                    });
        } catch (Exception e) {
            log.error("publishAuthorizedData failed", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> removeAuthorizedData(AuthorizedDistributedData authorizedDistributedData) {
        return networkService.removeAuthorizedData(authorizedDistributedData,
                        identity.getNetworkIdWithKeyPair().getKeyPair(),
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    private boolean republishAuthorizedBondedRoles() {
        List<AuthorizedBondedRole> list = networkService.getDataService().stream()
                .flatMap(dataService -> dataService.getAuthorizedData()
                        .map(AuthorizedData::getAuthorizedDistributedData)
                        .filter(authorizedDistributedData -> authorizedDistributedData instanceof AuthorizedBondedRole)
                        .map(authorizedDistributedData -> (AuthorizedBondedRole) authorizedDistributedData))
                .toList();
        int numSuccess = list.stream()
                .map(authorizedBondedRole -> {
                            Optional<AuthorizedOracleNode> authorizingOracleNode = authorizedBondedRole.getAuthorizingOracleNode();
                            String bondUserName = authorizedBondedRole.getBondUserName();
                            if (authorizingOracleNode.isPresent()) {
                                if (authorizingOracleNode.get().getProfileId().equals(authorizedOracleNode.getProfileId())) {
                                    log.info("Republish AuthorizedBondedRole with bondUserName {}. authorizedOracleNode={}",
                                            bondUserName, authorizedOracleNode.getBondUserName());
                                    try {
                                        return publishAuthorizedData(authorizedBondedRole).get();
                                    } catch (Exception e) {
                                        return false;
                                    }
                                } else {
                                    log.info("Cannot republish AuthorizedBondedRole with bondUserName {} because we are not the authorizedOracleNode for that data",
                                            bondUserName);
                                }
                            } else {
                                log.info("Cannot republish AuthorizedBondedRole with bondUserName {} because authorizedOracleNode is missing",
                                        bondUserName);
                            }
                            return false;
                        }
                ).mapToInt(success -> success ? 1 : 0)
                .sum();
        return list.size() == numSuccess;
    }

    private CompletableFuture<Boolean> requestDaoData() {
        log.info("requestDaoData");
        return requestProofOfBurnTxs()
                .thenCompose(this::publishProofOfBurnDtoSet)
                .thenCompose(result -> requestBondedReputations())
                .thenCompose(this::publishBondedReputationDtoSet);
    }

    private void processAuthorizeAccountAgeRequest(AuthorizeAccountAgeRequest request) {
        log.info("processAuthorizeAccountAgeRequest {}", request);
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
                                        AuthorizedAccountAgeData data = new AuthorizedAccountAgeData(profileId,
                                                requestDate,
                                                staticPublicKeysProvided);
                                        publishAuthorizedData(data);
                                    } else {
                                        log.warn("Date of account age for {} is not matching the date from the users request. " +
                                                        "Date from bridge service call: {}; Date from users request: {}",
                                                hashAsHex, date, requestDate);
                                    }
                                }, () -> log.warn("Result of requestAccountAgeWitness returns empty optional. Request was: {}", request));
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
        log.info("processAuthorizeSignedWitnessRequest {}", request);
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
                                        AuthorizedSignedWitnessData data = new AuthorizedSignedWitnessData(request.getProfileId(),
                                                request.getWitnessSignDate(),
                                                staticPublicKeysProvided);
                                        publishAuthorizedData(data);
                                    } else {
                                        log.warn("Date of signed witness for {} is not matching the date from the users request. " +
                                                        "Date from bridge service call: {}; Date from users request: {}",
                                                request.getHashAsHex(), date, witnessSignDate);
                                    }
                                }, () -> log.warn("Result of requestSignedWitnessDate returns empty optional. Request was: {}", request));
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

    private void processBondedRoleRegistrationRequest(BondedRoleRegistrationRequest request,
                                                      PublicKey senderPublicKey) {
        log.info("processBondedRoleRegistrationRequest {}", request);
        String profileId = request.getProfileId();

        // Verify if message sender is owner of the profileId
        String sendersProfileId = Hex.encode(DigestUtil.hash(senderPublicKey.getEncoded()));
        checkArgument(profileId.equals(sendersProfileId), "Senders pub key is not matching the profile ID");

        BondedRoleType bondedRoleType = request.getBondedRoleType();
        String bisq1RoleTypeName = toBisq1RoleTypeName(bondedRoleType);
        String bondUserName = request.getBondUserName();
        String signatureBase64 = request.getSignatureBase64();
        log.info("Received BondedRoleRegistrationRequest {}", request);
        httpService.requestBondedRoleVerification(bondUserName, bisq1RoleTypeName, profileId, signatureBase64)
                .whenComplete((bondedRoleVerificationDto, throwable) -> {
                    if (throwable == null) {
                        if (bondedRoleVerificationDto.getErrorMessage() == null) {
                            AuthorizedBondedRole data = new AuthorizedBondedRole(profileId,
                                    request.getAuthorizedPublicKey(),
                                    bondedRoleType,
                                    bondUserName,
                                    signatureBase64,
                                    request.getAddressByTransportTypeMap(),
                                    request.getNetworkId(),
                                    Optional.of(authorizedOracleNode),
                                    false);
                            if (request.isCancellationRequest()) {
                                authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                                        .filter(authorizedBondedRole -> authorizedBondedRole.equals(data))
                                        .forEach(this::removeAuthorizedData);
                            } else {
                                publishAuthorizedData(data);
                            }
                        } else {
                            log.warn("RequestBondedRole failed. {}", bondedRoleVerificationDto.getErrorMessage());
                        }
                    } else {
                        log.warn("Error at accountAgeService.findAccountAgeWitness", throwable);
                    }
                });
    }

    private String toBisq1RoleTypeName(BondedRoleType bondedRoleType) {
        String name = bondedRoleType.name();
        return switch (name) {
            case "MEDIATOR" -> "MEDIATOR"; // 5k
            case "MODERATOR" -> "YOUTUBE_ADMIN"; // 5k; repurpose unused role
            case "ARBITRATOR" -> "MOBILE_NOTIFICATIONS_RELAY_OPERATOR"; // 10k; Bisq 1 ARBITRATOR would require 100k!
            case "SECURITY_MANAGER" -> "BITCOINJ_MAINTAINER"; // 10k; repurpose unused role
            case "RELEASE_MANAGER" -> "FORUM_ADMIN"; // 10k; repurpose unused role
            case "ORACLE_NODE" -> "NETLAYER_MAINTAINER"; // 10k; repurpose unused role
            case "SEED_NODE" -> "SEED_NODE_OPERATOR"; // 10k
            case "EXPLORER_NODE" -> "BSQ_EXPLORER_OPERATOR"; // 10k; Explorer operator
            case "MARKET_PRICE_NODE" -> "DATA_RELAY_NODE_OPERATOR";
            default -> // 10k; price node
                    name;
        };
    }
}