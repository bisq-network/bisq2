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

package bisq.oracle.node.bisq1_bridge;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle.node.AuthorizedOracleNode;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedAccountAgeData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedBondedReputationData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedProofOfBurnData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedSignedWitnessData;
import bisq.oracle.node.bisq1_bridge.dto.BondedReputationDto;
import bisq.oracle.node.bisq1_bridge.dto.ProofOfBurnDto;
import bisq.oracle.node.bisq1_bridge.requests.AuthorizeAccountAgeRequest;
import bisq.oracle.node.bisq1_bridge.requests.AuthorizeSignedWitnessRequest;
import bisq.oracle.service.OracleService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import bisq.security.SignatureUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Prepares a service for distributing authorized DAO data. Could be useful for providing existing proof or burns or
 * bonded role data as long we do not have the DAO integrated.
 * An authorized developer could add those data. Verification is done by verifying the signature and pubKey against
 * the provided hard-coded pubKeys.
 * Similar concept is used in Bisq 1 for Filter, Alert or mediator/arbitrator registration.
 * The user who is authorized for publishing that data need to pass the private key as VM argument.
 * For development we use the keys defined in DevMode.AUTHORIZED_DEV_PUBLIC_KEYS:
 * -Dbisq.oracle.privateKey=30818d020100301006072a8648ce3d020106052b8104000a04763074020101042010c2ea3b2b1f1787f8a57d074e550b120cc04b326b43c545214434e474e5cde2a00706052b8104000aa14403420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63
 * -Dbisq.oracle.publicKey=3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63
 */
@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class Bisq1BridgeService implements Service, MessageListener, PersistenceClient<Bisq1BridgeStore> {
    @Getter
    private final Bisq1BridgeStore persistableStore = new Bisq1BridgeStore();
    @Getter
    private final Persistence<Bisq1BridgeStore> persistence;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Bisq1BridgeHttpService bisq1BridgeHttpService;
    private Optional<PrivateKey> authorizedPrivateKey = Optional.empty();
    private Optional<PublicKey> authorizedPublicKey = Optional.empty();

    public Bisq1BridgeService(OracleService.Config config,
                              NetworkService networkService,
                              IdentityService identityService,
                              PersistenceService persistenceService,
                              Bisq1BridgeHttpService bisq1BridgeHttpService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.bisq1BridgeHttpService = bisq1BridgeHttpService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        if (privateKey != null && !privateKey.isEmpty() && publicKey != null && !publicKey.isEmpty()) {
            try {
                authorizedPrivateKey = Optional.of(KeyGeneration.generatePrivate(Hex.decode(privateKey)));
                authorizedPublicKey = Optional.of(KeyGeneration.generatePublic(Hex.decode(publicKey)));
            } catch (GeneralSecurityException e) {
                log.error("Invalid authorization keys", e);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (authorizedPrivateKey.isPresent() && authorizedPublicKey.isPresent()) {
            networkService.addMessageListener(this);
            identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                    .whenComplete((identity, throwable) -> {
                        AuthorizedOracleNode data = new AuthorizedOracleNode(identity.getNetworkId());
                        publishAuthorizedData(data);
                    });
        }
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (authorizedPrivateKey.isPresent() && authorizedPublicKey.isPresent()) {
            networkService.removeMessageListener(this);
        }
        return CompletableFuture.completedFuture(true);
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
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<List<ProofOfBurnDto>> requestProofOfBurnTxs() {
        return bisq1BridgeHttpService.requestProofOfBurnTxs();
    }

    public CompletableFuture<List<BondedReputationDto>> requestBondedReputations() {
        return bisq1BridgeHttpService.requestBondedReputations();
    }

    public CompletableFuture<Boolean> publishProofOfBurnDtoSet(List<ProofOfBurnDto> proofOfBurnList) {
        checkArgument(authorizedPrivateKey.isPresent(), "authorizedPrivateKey must be present");
        checkArgument(authorizedPublicKey.isPresent(), "authorizedPublicKey must be present");
        return CompletableFutureUtils.allOf(proofOfBurnList.stream()
                        .map(AuthorizedProofOfBurnData::from)
                        .map(this::publishAuthorizedData))
                .thenApply(results -> !results.contains(false));
    }

    public CompletableFuture<Boolean> publishBondedReputationDtoSet(List<BondedReputationDto> bondedReputationList) {

        return CompletableFutureUtils.allOf(bondedReputationList.stream()
                        .map(AuthorizedBondedReputationData::from)
                        .map(this::publishAuthorizedData))
                .thenApply(results -> !results.contains(false));
    }


    public CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                .thenCompose(identity -> networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair(),
                        authorizedPrivateKey.orElseThrow(),
                        authorizedPublicKey.orElseThrow()))
                .thenApply(broadCastDataResult -> true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
                bisq1BridgeHttpService.requestAccountAgeWitness(hashAsHex)
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
                bisq1BridgeHttpService.requestSignedWitnessDate(request.getHashAsHex())
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
}