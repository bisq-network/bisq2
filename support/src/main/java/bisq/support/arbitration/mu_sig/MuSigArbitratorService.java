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

package bisq.support.arbitration.mu_sig;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import bisq.support.arbitration.ArbitrationResultReason;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse;
import bisq.support.dispute.mu_sig.MuSigDisputePaymentDetailsVerifier;
import bisq.support.dispute.mu_sig.MuSigDisputeRoleIdentityResolver;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import static bisq.support.dispute.mu_sig.MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent;
import static bisq.support.dispute.mu_sig.MuSigDisputeContractIdentityChecks.hasMatchingContractParties;
import static bisq.support.dispute.mu_sig.MuSigDisputeContractIdentityChecks.resolveSenderRole;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service used by arbitrators.
 */
@Slf4j
public class MuSigArbitratorService extends RateLimitedPersistenceClient<MuSigArbitratorStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final MuSigArbitratorStore persistableStore = new MuSigArbitratorStore();
    @Getter
    private final Persistence<MuSigArbitratorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final Object arbitrationCaseLock = new Object();

    public MuSigArbitratorService(PersistenceService persistenceService,
                                  NetworkService networkService,
                                  ChatService chatService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(this::onMessage);
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigArbitrationRequest message) {
            synchronized (arbitrationCaseLock) {
                verifyArbitrationRequest(message, bannedUserService)
                        .ifPresent(requester -> processArbitrationRequest(message, requester));
            }
        } else if (envelopePayloadMessage instanceof MuSigDisputeCasePaymentDetailsResponse message) {
            synchronized (arbitrationCaseLock) {
                verifyDisputeCasePaymentDetailsResponse(message, this::findArbitrationCase, bannedUserService)
                        .ifPresent(arbitrationCase -> processDisputeCasePaymentDetailsResponse(message, arbitrationCase));
            }
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public static MuSigArbitrationResult createMuSigArbitrationResult(MuSigContract contract,
                                                                      ArbitrationResultReason arbitrationResultReason,
                                                                      ArbitrationPayoutDistributionType arbitrationPayoutDistributionType,
                                                                      long buyerPayoutAmount,
                                                                      long sellerPayoutAmount,
                                                                      Optional<String> summaryNotes) {
        MuSigArbitrationResult muSigArbitrationResult = new MuSigArbitrationResult(ContractService.getContractHash(contract),
                arbitrationResultReason,
                arbitrationPayoutDistributionType,
                buyerPayoutAmount,
                sellerPayoutAmount,
                summaryNotes);
        checkMuSigArbitrationResult(contract, muSigArbitrationResult);
        return muSigArbitrationResult;
    }

    public void closeArbitrationCase(MuSigArbitrationCase muSigArbitrationCase,
                                     MuSigArbitrationResult muSigArbitrationResult) {
        synchronized (arbitrationCaseLock) {
            Optional<MuSigArbitrationResult> existingResult = muSigArbitrationCase.getMuSigArbitrationResult();
            if (existingResult.filter(result -> !result.equals(muSigArbitrationResult)).isPresent()) {
                log.warn("Ignoring changed MuSigArbitrationResult for trade {} because result cannot be changed once set.",
                        muSigArbitrationCase.getMuSigArbitrationRequest().getTradeId());
            }

            MuSigArbitrationResult resultToUse = existingResult.orElse(muSigArbitrationResult);
            checkMuSigArbitrationResult(muSigArbitrationCase.getMuSigArbitrationRequest().getContract(), resultToUse);
            boolean resultChanged = false;
            if (existingResult.isEmpty() || muSigArbitrationCase.getArbitrationResultSignature().isEmpty()) {
                byte[] arbitrationResultSignature = createArbitrationResultSignature(muSigArbitrationCase, resultToUse);
                resultChanged = muSigArbitrationCase.setSignedMuSigArbitrationResult(resultToUse, arbitrationResultSignature);
            }
            boolean stateChanged = muSigArbitrationCase.setArbitrationCaseState(ArbitrationCaseState.CLOSED);
            if (resultChanged || stateChanged) {
                persist();
                sendArbitrationCaseStateChangeMessage(muSigArbitrationCase);
                sendArbitrationCaseStateChangeTradeLogMessage(muSigArbitrationCase);
            }
        }
    }

    private static void checkMuSigArbitrationResult(MuSigContract contract,
                                                    MuSigArbitrationResult muSigArbitrationResult) {
        checkArgument(Arrays.equals(muSigArbitrationResult.getContractHash(), ContractService.getContractHash(contract)),
                "MuSigArbitrationResult contractHash does not match contract");
        Optional<MuSigArbitrationPayoutResolver.PayoutContext> optionalPayoutContext =
                MuSigArbitrationPayoutResolver.createPayoutContext(contract);
        checkArgument(optionalPayoutContext.isPresent(), "CollateralOption not found for MuSigContract");
        MuSigArbitrationPayoutResolver.PayoutContext payoutContext = optionalPayoutContext.orElseThrow();
        MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                muSigArbitrationResult.getArbitrationPayoutDistributionType(),
                payoutContext,
                muSigArbitrationResult.getBuyerPayoutAmount(),
                muSigArbitrationResult.getSellerPayoutAmount());
    }

    public void removeArbitrationCase(MuSigArbitrationCase muSigArbitrationCase) {
        synchronized (arbitrationCaseLock) {
            leaveChannel(muSigArbitrationCase.getMuSigArbitrationRequest().getTradeId());
            getArbitrationCases().remove(muSigArbitrationCase);
            persist();
        }
    }

    public void leaveChat(MuSigArbitrationCase muSigArbitrationCase) {
        MuSigArbitrationRequest muSigArbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        synchronized (arbitrationCaseLock) {
            boolean changed = muSigArbitrationCase.setArbitratorHasLeftChat(true);
            if (changed) {
                persist();
            }
            leaveChannel(muSigArbitrationRequest.getTradeId());
        }
    }

    public boolean requestPaymentDetails(MuSigArbitrationCase muSigArbitrationCase) {
        MuSigArbitrationRequest muSigArbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        Optional<UserIdentity> myArbitratorUserIdentity = findMyArbitratorUserIdentity(muSigArbitrationRequest.getContract().getArbitrator());
        if (myArbitratorUserIdentity.isEmpty()) {
            log.warn("Cannot request payment details for trade {} because arbitrator identity was not found.",
                    muSigArbitrationRequest.getTradeId());
            return false;
        }

        UserIdentity myUserIdentity = myArbitratorUserIdentity.orElseThrow();
        MuSigDisputeCasePaymentDetailsRequest message = new MuSigDisputeCasePaymentDetailsRequest(
                muSigArbitrationRequest.getTradeId(),
                myUserIdentity.getNetworkIdWithKeyPair().getNetworkId()
        );
        NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        networkService.confidentialSend(message,
                muSigArbitrationRequest.getRequester().getNetworkId(),
                networkIdWithKeyPair);
        networkService.confidentialSend(message,
                muSigArbitrationRequest.getPeer().getNetworkId(),
                networkIdWithKeyPair);
        return true;
    }

    public ObservableSet<MuSigArbitrationCase> getArbitrationCases() {
        return persistableStore.getMuSigArbitrationCases();
    }

    public Optional<UserIdentity> findMyArbitratorUserIdentity(Optional<UserProfile> arbitrator) {
        return MuSigDisputeRoleIdentityResolver.findMyUserIdentity(arbitrator,
                authorizedBondedRolesService,
                userIdentityService,
                BondedRoleType.ARBITRATOR);
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    static Optional<UserProfile> verifyArbitrationRequest(MuSigArbitrationRequest message,
                                                          BannedUserService bannedUserService) {
        UserProfile requester = message.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Ignoring MuSigArbitrationRequest as sender is banned");
            return Optional.empty();
        }
        MuSigContract contract = message.getContract();
        UserProfile peer = message.getPeer();
        if (!hasMatchingContractParties(contract, requester, peer)) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because requester {} and peer {} do not match contract parties.",
                    message.getTradeId(), requester.getId(), peer.getId());
            return Optional.empty();
        }
        if (!hasMatchingContractDisputeAgent(contract.getArbitrator(), message.getReceiver())) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because arbitrator does not match contract arbitrator.",
                    message.getTradeId());
            return Optional.empty();
        }
        if (!hasValidChatMessages(message, requester, peer)) {
            return Optional.empty();
        }
        return Optional.of(requester);
    }

    private static boolean hasValidChatMessages(MuSigArbitrationRequest message,
                                                UserProfile requester,
                                                UserProfile peer) {
        String tradeId = message.getTradeId();
        String channelId = MuSigOpenTradeChannel.createId(tradeId);
        return message.getChatMessages().stream()
                .allMatch(chatMessage -> {
                    if (!chatMessage.getTradeId().equals(tradeId)) {
                        log.warn("Ignoring MuSigArbitrationRequest for trade {} because embedded chat message {} has trade ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getTradeId());
                        return false;
                    }
                    if (!chatMessage.getChannelId().equals(channelId)) {
                        log.warn("Ignoring MuSigArbitrationRequest for trade {} because embedded chat message {} has channel ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getChannelId());
                        return false;
                    }
                    String senderUserProfileId = chatMessage.getSenderUserProfile().getId();
                    if (!senderUserProfileId.equals(requester.getId()) && !senderUserProfileId.equals(peer.getId())) {
                        log.warn("Ignoring MuSigArbitrationRequest for trade {} because embedded chat message {} has unexpected sender {}.",
                                tradeId, chatMessage.getId(), senderUserProfileId);
                        return false;
                    }
                    return true;
                });
    }

    private void processArbitrationRequest(MuSigArbitrationRequest message, UserProfile requester) {
        String tradeId = message.getTradeId();
        if (findArbitrationCase(tradeId).isPresent()) {
            log.info("Ignoring duplicate MuSigArbitrationRequest for already existing trade {}.", tradeId);
            return;
        }

        if (!isMediationResultValid(message)) {
            return;
        }

        MuSigContract contract = message.getContract();
        Optional<UserIdentity> myArbitratorUserIdentity = findMyArbitratorUserIdentity(contract.getArbitrator());
        if (myArbitratorUserIdentity.isEmpty()) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because no matching local arbitrator identity was found.",
                    tradeId);
            return;
        }

        UserIdentity myUserIdentity = myArbitratorUserIdentity.orElseThrow();
        UserProfile peer = message.getPeer();
        List<MuSigOpenTradeMessage> chatMessages = message.getChatMessages();
        MuSigOpenTradeChannel channel = muSigOpenTradeChannelService.arbitratorFindOrCreatesChannel(
                tradeId,
                myUserIdentity,
                requester,
                peer
        );

        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.ARBITRATOR);
        chatMessages.forEach(chatMessage -> muSigOpenTradeChannelService.addMessage(chatMessage, channel));

        // We apply the muSigArbitrationCase after the channel is set up as clients will expect a channel
        MuSigArbitrationCase muSigArbitrationCase = new MuSigArbitrationCase(message);
        addNewArbitrationCase(muSigArbitrationCase);

        NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        MuSigArbitrationStateChangeMessage openMessage = new MuSigArbitrationStateChangeMessage(
                StringUtils.createUid(),
                tradeId,
                myUserIdentity.getNetworkIdWithKeyPair().getNetworkId(),
                ArbitrationCaseState.OPEN,
                Optional.empty(),
                Optional.empty());

        // Send to requester
        networkService.confidentialSend(openMessage, requester.getNetworkId(), networkIdWithKeyPair);
        muSigOpenTradeChannelService.addArbitrationOpenedMessage(channel, Res.encode("authorizedRole.arbitrator.message.toRequester"));

        // Send to peer
        networkService.confidentialSend(openMessage, peer.getNetworkId(), networkIdWithKeyPair);
        muSigOpenTradeChannelService.addArbitrationOpenedMessage(channel, Res.encode("authorizedRole.arbitrator.message.toNonRequester"));

        log.info("Processed MuSigArbitrationRequest for trade {} from requester {} to arbitrator {}.",
                tradeId,
                requester.getId(),
                myUserIdentity.getId());
    }

    static Optional<MuSigArbitrationCase> verifyDisputeCasePaymentDetailsResponse(
            MuSigDisputeCasePaymentDetailsResponse response,
            Function<String, Optional<MuSigArbitrationCase>> findArbitrationCase,
            BannedUserService bannedUserService) {
        String tradeId = response.getTradeId();
        return findArbitrationCase.apply(tradeId)
                .<Optional<MuSigArbitrationCase>>map(arbitrationCase -> {
                    MuSigArbitrationRequest muSigArbitrationRequest = arbitrationCase.getMuSigArbitrationRequest();
                    NetworkId senderNetworkId = response.getSenderNetworkId();
                    UserProfile requester = muSigArbitrationRequest.getRequester();
                    UserProfile peer = muSigArbitrationRequest.getPeer();
                    boolean isRequester = requester.getId().equals(senderNetworkId.getId());
                    boolean isPeer = peer.getId().equals(senderNetworkId.getId());
                    if (!isRequester && !isPeer) {
                        log.warn("Ignoring MuSigDisputeCasePaymentDetailsResponse for trade {} with unknown senderNetworkId {}.",
                                tradeId, senderNetworkId);
                        return Optional.empty();
                    }
                    if (bannedUserService.isUserProfileBanned(senderNetworkId)) {
                        log.warn("Ignoring MuSigDisputeCasePaymentDetailsResponse for trade {} from banned senderNetworkId {}.",
                                tradeId, senderNetworkId);
                        return Optional.empty();
                    }
                    return Optional.of(arbitrationCase);
                })
                .orElseGet(() -> {
                    log.warn("Ignoring MuSigDisputeCasePaymentDetailsResponse for unknown trade {}.", tradeId);
                    return Optional.empty();
                });
    }

    private void processDisputeCasePaymentDetailsResponse(MuSigDisputeCasePaymentDetailsResponse response,
                                                          MuSigArbitrationCase arbitrationCase) {
        String tradeId = response.getTradeId();
        MuSigArbitrationRequest muSigArbitrationRequest = arbitrationCase.getMuSigArbitrationRequest();
        Role causingRole = resolveSenderRole(muSigArbitrationRequest.getContract(), response.getSenderNetworkId().getId());
        PaymentDetailsVerification verification = verifyPaymentDetails(muSigArbitrationRequest.getContract(),
                response,
                causingRole);
        boolean changed = false;
        if (verification.takerAccountPayloadMatches()) {
            changed |= arbitrationCase.setTakerPaymentAccountPayload(response.getTakerAccountPayload());
        }
        if (verification.makerAccountPayloadMatches()) {
            changed |= arbitrationCase.setMakerPaymentAccountPayload(response.getMakerAccountPayload());
        }
        if (!verification.issues().isEmpty()) {
            changed |= arbitrationCase.addIssues(verification.issues());
            log.warn("MuSigDisputeCasePaymentDetailsResponse for trade {} has verification issues: {}",
                    tradeId, verification.issues());
        }
        if (changed) {
            persist();
        }
    }

    private void addNewArbitrationCase(MuSigArbitrationCase muSigArbitrationCase) {
        getArbitrationCases().add(muSigArbitrationCase);
        persist();
    }

    private Optional<MuSigArbitrationCase> findArbitrationCase(String tradeId) {
        return getArbitrationCases().stream()
                .filter(item -> item.getMuSigArbitrationRequest().getTradeId().equals(tradeId))
                .findAny();
    }

    private boolean isMediationResultValid(MuSigArbitrationRequest message) {
        String tradeId = message.getTradeId();
        MuSigContract contract = message.getContract();
        MuSigMediationResult mediationResult = message.getMuSigMediationResult();
        if (!Arrays.equals(mediationResult.getContractHash(), ContractService.getContractHash(contract))) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because MuSigMediationResult contract hash does not match request contract.",
                    tradeId);
            return false;
        }
        Optional<UserProfile> mediator = contract.getMediator();
        if (mediator.isEmpty()) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because request contract has no mediator.",
                    tradeId);
            return false;
        }
        try {
            if (!MuSigMediationResultService.verifyMediationResult(
                    mediationResult,
                    message.getMediationResultSignature(),
                    contract,
                    mediator.orElseThrow().getPublicKey())) {
                log.warn("Ignoring MuSigArbitrationRequest for trade {} because MuSigMediationResult signature verification failed.",
                        tradeId);
                return false;
            }
        } catch (GeneralSecurityException e) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because MuSigMediationResult signature verification failed.",
                    tradeId, e);
            return false;
        }
        return true;
    }

    private PaymentDetailsVerification verifyPaymentDetails(MuSigContract contract,
                                                            MuSigDisputeCasePaymentDetailsResponse response,
                                                            Role causingRole) {
        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract,
                response.getTakerAccountPayload(),
                response.getMakerAccountPayload());
        List<MuSigArbitrationIssue> issues = Stream.concat(
                        result.takerMismatchDetails().stream()
                                .map(details -> new MuSigArbitrationIssue(
                                        causingRole,
                                        MuSigArbitrationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                                        Optional.of(details))),
                        result.makerMismatchDetails().stream()
                                .map(details -> new MuSigArbitrationIssue(
                                        causingRole,
                                        MuSigArbitrationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                                        Optional.of(details))))
                .toList();

        return new PaymentDetailsVerification(result.takerAccountPayloadMatches(),
                result.makerAccountPayloadMatches(),
                issues);
    }

    private void sendArbitrationCaseStateChangeMessage(MuSigArbitrationCase muSigArbitrationCase) {
        MuSigArbitrationRequest muSigArbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        ArbitrationCaseState arbitrationCaseState = muSigArbitrationCase.getArbitrationCaseState();
        Optional<MuSigArbitrationResult> muSigArbitrationResult = muSigArbitrationCase.getMuSigArbitrationResult();
        findMyArbitratorUserIdentity(muSigArbitrationRequest.getContract().getArbitrator())
                .ifPresent(myUserIdentity -> {
                    String id = StringUtils.createUid();
                    MuSigArbitrationStateChangeMessage message = new MuSigArbitrationStateChangeMessage(id,
                            muSigArbitrationRequest.getTradeId(),
                            myUserIdentity.getNetworkIdWithKeyPair().getNetworkId(),
                            arbitrationCaseState,
                            muSigArbitrationResult,
                            muSigArbitrationCase.getArbitrationResultSignature());
                    NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

                    networkService.confidentialSend(message,
                            muSigArbitrationRequest.getRequester().getNetworkId(),
                            networkIdWithKeyPair);

                    networkService.confidentialSend(message,
                            muSigArbitrationRequest.getPeer().getNetworkId(),
                            networkIdWithKeyPair);
                });
    }

    private void sendArbitrationCaseStateChangeTradeLogMessage(MuSigArbitrationCase muSigArbitrationCase) {
        if (muSigArbitrationCase.getArbitrationCaseState() != ArbitrationCaseState.CLOSED) {
            return;
        }
        muSigOpenTradeChannelService.findChannelByTradeId(muSigArbitrationCase.getMuSigArbitrationRequest().getTradeId())
                .ifPresent(channel -> {
                    muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.ARBITRATOR);
                    muSigOpenTradeChannelService.sendTradeLogMessage(Res.encode("authorizedRole.arbitrator.message.arbitrationCaseClosed"), channel);
                });
    }

    private byte[] createArbitrationResultSignature(MuSigArbitrationCase muSigArbitrationCase,
                                                    MuSigArbitrationResult muSigArbitrationResult) {
        MuSigArbitrationRequest arbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        return findMyArbitratorUserIdentity(arbitrationRequest.getContract().getArbitrator())
                .map(myUserIdentity -> {
                    try {
                        return MuSigArbitrationResultService.signArbitrationResult(
                                muSigArbitrationResult,
                                myUserIdentity.getNetworkIdWithKeyPair().getKeyPair());
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException("Could not sign MuSigArbitrationStateChangeMessage for trade " + arbitrationRequest.getTradeId(), e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Could not sign MuSigArbitrationStateChangeMessage because arbitrator identity was not found."));
    }

    private record PaymentDetailsVerification(boolean takerAccountPayloadMatches,
                                              boolean makerAccountPayloadMatches,
                                              List<MuSigArbitrationIssue> issues) {
    }

    private void leaveChannel(String tradeId) {
        muSigOpenTradeChannelService.findChannelByTradeId(tradeId)
                .ifPresentOrElse(leavePrivateChatManager::leaveChannel,
                        () -> log.warn("Ignoring leaveChat for unknown channel on trade {}.",
                                tradeId));
    }
}
