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

package bisq.support.mediation.mu_sig;

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
import bisq.common.encoding.Hex;
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
import bisq.support.dispute.mu_sig.MuSigDisputeCaseDataMessage;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse;
import bisq.support.dispute.mu_sig.MuSigDisputePaymentDetailsVerifier;
import bisq.support.dispute.mu_sig.MuSigDisputeRoleIdentityResolver;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
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
 * Service used by mediators
 */
@Slf4j
public class MuSigMediatorService extends RateLimitedPersistenceClient<MuSigMediatorStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final MuSigMediatorStore persistableStore = new MuSigMediatorStore();
    @Getter
    private final Persistence<MuSigMediatorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final Object mediationCaseLock = new Object();

    public MuSigMediatorService(PersistenceService persistenceService,
                                NetworkService networkService,
                                ChatService chatService,
                                UserService userService,
                                BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();
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
        if (envelopePayloadMessage instanceof MuSigMediationRequest message) {
            synchronized (mediationCaseLock) {
                verifyMediationRequest(message, bannedUserService)
                        .ifPresent(sender -> processMediationRequest(message, sender));
            }
        } else if (envelopePayloadMessage instanceof MuSigDisputeCaseDataMessage message) {
            synchronized (mediationCaseLock) {
                verifyDisputeCaseDataMessage(message, this::findMediationCase, bannedUserService)
                        .ifPresent(mediationCase -> processDisputeCaseDataMessage(message, mediationCase));
            }
        } else if (envelopePayloadMessage instanceof MuSigDisputeCasePaymentDetailsResponse message) {
            synchronized (mediationCaseLock) {
                verifyDisputeCasePaymentDetailsResponse(message, this::findMediationCase, bannedUserService)
                        .ifPresent(mediationCase -> processDisputeCasePaymentDetailsResponse(message, mediationCase));
            }
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public static MuSigMediationResult createMuSigMediationResult(MuSigContract contract,
                                                                  MediationResultReason mediationResultReason,
                                                                  MediationPayoutDistributionType mediationPayoutDistributionType,
                                                                  Optional<Long> proposedBuyerPayoutAmount,
                                                                  Optional<Long> proposedSellerPayoutAmount,
                                                                  Optional<Double> payoutAdjustmentPercentage,
                                                                  Optional<String> summaryNotes) {
        MuSigMediationResult muSigMediationResult = new MuSigMediationResult(ContractService.getContractHash(contract),
                mediationResultReason, mediationPayoutDistributionType,
                proposedBuyerPayoutAmount, proposedSellerPayoutAmount,
                payoutAdjustmentPercentage, summaryNotes);
        checkMuSigMediationResult(contract, muSigMediationResult);
        return muSigMediationResult;
    }

    public void closeMediationCase(MuSigMediationCase muSigMediationCase, MuSigMediationResult muSigMediationResult) {
        synchronized (mediationCaseLock) {
            Optional<MuSigMediationResult> existingResult = muSigMediationCase.getMuSigMediationResult();
            if (existingResult.filter(result -> !result.equals(muSigMediationResult)).isPresent()) {
                log.warn("Ignoring changed MuSigMediationResult for trade {} because result cannot be changed once set.",
                        muSigMediationCase.getMuSigMediationRequest().getTradeId());
            }

            MuSigMediationResult resultToUse = existingResult.orElse(muSigMediationResult);
            checkMuSigMediationResult(muSigMediationCase.getMuSigMediationRequest().getContract(), resultToUse);
            boolean resultChanged = false;
            if (existingResult.isEmpty() || muSigMediationCase.getMediationResultSignature().isEmpty()) {
                byte[] mediationResultSignature = createMediationResultSignature(muSigMediationCase, resultToUse);
                resultChanged = muSigMediationCase.setSignedMuSigMediationResult(resultToUse, mediationResultSignature);
            }
            boolean stateChanged = muSigMediationCase.setMediationCaseState(MediationCaseState.CLOSED);
            if (resultChanged || stateChanged) {
                persist();
                sendMediationCaseStateChangeMessage(muSigMediationCase);
                sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
            }
        }
    }

    public void removeMediationCase(MuSigMediationCase muSigMediationCase) {
        synchronized (mediationCaseLock) {
            leaveChannel(muSigMediationCase.getMuSigMediationRequest().getTradeId());
            getMediationCases().remove(muSigMediationCase);
            persist();
        }
    }

    public void reOpenMediationCase(MuSigMediationCase muSigMediationCase) {
        synchronized (mediationCaseLock) {
            if (muSigMediationCase.setMediationCaseState(MediationCaseState.RE_OPENED)) {
                persist();
                sendMediationCaseStateChangeMessage(muSigMediationCase);
                sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
            }
        }
    }

    public void closeReOpenedMediationCase(MuSigMediationCase muSigMediationCase) {
        synchronized (mediationCaseLock) {
            Optional<MuSigMediationResult> existingResult = muSigMediationCase.getMuSigMediationResult();
            if (existingResult.isEmpty()) {
                log.warn("Cannot close re-opened mediation case for trade {} because MuSigMediationResult is missing.",
                        muSigMediationCase.getMuSigMediationRequest().getTradeId());
                return;
            }
            checkMuSigMediationResult(muSigMediationCase.getMuSigMediationRequest().getContract(), existingResult.orElseThrow());
            if (muSigMediationCase.setMediationCaseState(MediationCaseState.CLOSED)) {
                persist();
                sendMediationCaseStateChangeMessage(muSigMediationCase);
                sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
            }
        }
    }

    private static void checkMuSigMediationResult(MuSigContract contract,
                                                  MuSigMediationResult muSigMediationResult) {
        checkArgument(Arrays.equals(muSigMediationResult.getContractHash(), ContractService.getContractHash(contract)),
                "MuSigMediationResult contractHash does not match contract");
        Optional<MuSigMediationPayoutResolver.PayoutContext> optionalPayoutContext =
                MuSigMediationPayoutResolver.createPayoutContext(contract);
        checkArgument(optionalPayoutContext.isPresent(), "CollateralOption not found for MuSigContract");
        MuSigMediationPayoutResolver.PayoutContext payoutContext = optionalPayoutContext.orElseThrow();
        MuSigMediationPayoutResolver.checkPayoutAmounts(
                muSigMediationResult.getMediationPayoutDistributionType(),
                payoutContext,
                muSigMediationResult.getProposedBuyerPayoutAmount(),
                muSigMediationResult.getProposedSellerPayoutAmount(),
                muSigMediationResult.getPayoutAdjustmentPercentage());
    }

    public void leaveChat(MuSigMediationCase muSigMediationCase) {
        MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
        synchronized (mediationCaseLock) {
            boolean changed = muSigMediationCase.setMediatorHasLeftChat(true);
            if (changed) {
                persist();
            }
            leaveChannel(muSigMediationRequest.getTradeId());
        }
    }

    public boolean requestPaymentDetails(MuSigMediationCase muSigMediationCase) {
        MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
        Optional<UserIdentity> myMediatorUserIdentity = findMyMediatorUserIdentity(muSigMediationRequest.getContract().getMediator());
        if (myMediatorUserIdentity.isEmpty()) {
            log.warn("Cannot request payment details for trade {} because mediator identity was not found.",
                    muSigMediationRequest.getTradeId());
            return false;
        }

        UserIdentity myUserIdentity = myMediatorUserIdentity.orElseThrow();
        MuSigDisputeCasePaymentDetailsRequest message = new MuSigDisputeCasePaymentDetailsRequest(
                muSigMediationRequest.getTradeId(),
                myUserIdentity.getNetworkIdWithKeyPair().getNetworkId()
        );
        NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        networkService.confidentialSend(message,
                muSigMediationRequest.getRequester().getNetworkId(),
                networkIdWithKeyPair);
        networkService.confidentialSend(message,
                muSigMediationRequest.getPeer().getNetworkId(),
                networkIdWithKeyPair);
        return true;
    }

    public ObservableSet<MuSigMediationCase> getMediationCases() {
        return persistableStore.getMuSigMediationCases();
    }

    public Optional<UserIdentity> findMyMediatorUserIdentity(Optional<UserProfile> mediator) {
        return MuSigDisputeRoleIdentityResolver.findMyUserIdentity(mediator,
                authorizedBondedRolesService,
                userIdentityService,
                BondedRoleType.MEDIATOR);
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    static Optional<UserProfile> verifyMediationRequest(MuSigMediationRequest muSigMediationRequest,
                                                        BannedUserService bannedUserService) {
        UserProfile requester = muSigMediationRequest.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Message ignored as sender is banned");
            return Optional.empty();
        }
        MuSigContract contract = muSigMediationRequest.getContract();
        UserProfile peer = muSigMediationRequest.getPeer();
        if (!hasMatchingContractParties(contract, requester, peer)) {
            log.warn("Ignoring MuSigMediationRequest for trade {} because requester {} and peer {} do not match contract parties.",
                    muSigMediationRequest.getTradeId(), requester.getId(), peer.getId());
            return Optional.empty();
        }
        if (!hasMatchingContractDisputeAgent(contract.getMediator(), muSigMediationRequest.getReceiver())) {
            log.warn("Ignoring MuSigMediationRequest for trade {} because mediator does not match contract mediator.",
                    muSigMediationRequest.getTradeId());
            return Optional.empty();
        }
        if (!hasValidChatMessages(muSigMediationRequest, requester, peer)) {
            return Optional.empty();
        }
        return Optional.of(requester);
    }

    private static boolean hasValidChatMessages(MuSigMediationRequest muSigMediationRequest,
                                                UserProfile requester,
                                                UserProfile peer) {
        String tradeId = muSigMediationRequest.getTradeId();
        String channelId = MuSigOpenTradeChannel.createId(tradeId);
        return muSigMediationRequest.getChatMessages().stream()
                .allMatch(chatMessage -> {
                    if (!chatMessage.getTradeId().equals(tradeId)) {
                        log.warn("Ignoring MuSigMediationRequest for trade {} because embedded chat message {} has trade ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getTradeId());
                        return false;
                    }
                    if (!chatMessage.getChannelId().equals(channelId)) {
                        log.warn("Ignoring MuSigMediationRequest for trade {} because embedded chat message {} has channel ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getChannelId());
                        return false;
                    }
                    String senderUserProfileId = chatMessage.getSenderUserProfile().getId();
                    if (!senderUserProfileId.equals(requester.getId()) && !senderUserProfileId.equals(peer.getId())) {
                        log.warn("Ignoring MuSigMediationRequest for trade {} because embedded chat message {} has unexpected sender {}.",
                                tradeId, chatMessage.getId(), senderUserProfileId);
                        return false;
                    }
                    return true;
                });
    }

    private void processMediationRequest(MuSigMediationRequest muSigMediationRequest, UserProfile requester) {
        MuSigContract contract = muSigMediationRequest.getContract();
        String tradeId = muSigMediationRequest.getTradeId();
        if (findMediationCase(tradeId).isPresent()) {
            log.info("Ignoring duplicate MuSigMediationRequest for already existing trade {}.", tradeId);
            return;
        }

        Optional<UserIdentity> myMediatorUserIdentity = findMyMediatorUserIdentity(contract.getMediator());
        if (myMediatorUserIdentity.isEmpty()) {
            log.warn("Ignoring MuSigMediationRequest for trade {} because no matching local mediator identity was found.",
                    tradeId);
            return;
        }

        UserIdentity myUserIdentity = myMediatorUserIdentity.orElseThrow();
        UserProfile peer = muSigMediationRequest.getPeer();
        List<MuSigOpenTradeMessage> chatMessages = muSigMediationRequest.getChatMessages();
        MuSigOpenTradeChannel channel = muSigOpenTradeChannelService.mediatorFindOrCreatesChannel(
                tradeId,
                myUserIdentity,
                requester,
                peer
        );

        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);

        chatMessages.forEach(chatMessage ->
                muSigOpenTradeChannelService.addMessage(chatMessage, channel));

        // We apply the muSigMediationCase after the channel is set up as clients will expect a channel
        MuSigMediationCase muSigMediationCase = new MuSigMediationCase(muSigMediationRequest);
        addNewMediationCase(muSigMediationCase);

        NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

        MuSigMediationStateChangeMessage openMessage = new MuSigMediationStateChangeMessage(
                StringUtils.createUid(),
                tradeId,
                myUserIdentity.getNetworkIdWithKeyPair().getNetworkId(),
                MediationCaseState.OPEN,
                Optional.empty(),
                Optional.empty());

        // Send to requester
        networkService.confidentialSend(openMessage,
                requester.getNetworkId(),
                networkIdWithKeyPair);
        muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));

        // Send to peer
        networkService.confidentialSend(openMessage,
                peer.getNetworkId(),
                networkIdWithKeyPair);
        // TODO: check whether we could remove the following line due to the fact that the peer will also send the chat history
        muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
    }

    static Optional<MuSigMediationCase> verifyDisputeCasePaymentDetailsResponse(
            MuSigDisputeCasePaymentDetailsResponse response,
            Function<String, Optional<MuSigMediationCase>> findMediationCase,
            BannedUserService bannedUserService) {
        String tradeId = response.getTradeId();
        return findMediationCase.apply(tradeId)
                .<Optional<MuSigMediationCase>>map(mediationCase -> {
                    MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
                    NetworkId senderNetworkId = response.getSenderNetworkId();
                    UserProfile requester = muSigMediationRequest.getRequester();
                    UserProfile peer = muSigMediationRequest.getPeer();
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
                    return Optional.of(mediationCase);
                })
                .orElseGet(() -> {
                    log.warn("Ignoring MuSigDisputeCasePaymentDetailsResponse for unknown trade {}.", tradeId);
                    return Optional.empty();
                });
    }

    private void processDisputeCasePaymentDetailsResponse(MuSigDisputeCasePaymentDetailsResponse response,
                                                          MuSigMediationCase mediationCase) {
        String tradeId = response.getTradeId();
        MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
        Role causingRole = resolveSenderRole(muSigMediationRequest.getContract(), response.getSenderNetworkId().getId());
        PaymentDetailsVerification verification = verifyPaymentDetails(muSigMediationRequest.getContract(),
                response,
                causingRole);
        boolean changed = false;
        if (verification.takerAccountPayloadMatches()) {
            changed |= mediationCase.setTakerPaymentAccountPayload(response.getTakerAccountPayload());
        }
        if (verification.makerAccountPayloadMatches()) {
            changed |= mediationCase.setMakerPaymentAccountPayload(response.getMakerAccountPayload());
        }
        if (!verification.issues().isEmpty()) {
            changed |= mediationCase.addIssues(verification.issues());
            log.warn("MuSigDisputeCasePaymentDetailsResponse for trade {} has verification issues: {}",
                    tradeId, verification.issues());
        }
        if (changed) {
            persist();
        }
    }

    static Optional<MuSigMediationCase> verifyDisputeCaseDataMessage(MuSigDisputeCaseDataMessage message,
                                                                     Function<String, Optional<MuSigMediationCase>> findMediationCase,
                                                                     BannedUserService bannedUserService) {
        String tradeId = message.getTradeId();
        return findMediationCase.apply(tradeId)
                .<Optional<MuSigMediationCase>>map(mediationCase -> {
                    NetworkId senderNetworkId = message.getSenderNetworkId();
                    UserProfile peer = mediationCase.getMuSigMediationRequest().getPeer();
                    if (!peer.getId().equals(senderNetworkId.getId())) {
                        log.warn("Ignoring MuSigDisputeCaseDataMessage for trade {} with unexpected senderNetworkId {}.",
                                tradeId, senderNetworkId);
                        return Optional.empty();
                    }
                    if (bannedUserService.isUserProfileBanned(senderNetworkId)) {
                        log.warn("Ignoring MuSigDisputeCaseDataMessage for trade {} from banned senderNetworkId {}.",
                                tradeId, senderNetworkId);
                        return Optional.empty();
                    }
                    return Optional.of(mediationCase);
                })
                .orElseGet(() -> {
                    log.warn("Ignoring MuSigDisputeCaseDataMessage for unknown trade {}.", tradeId);
                    return Optional.empty();
                });
    }

    private void processDisputeCaseDataMessage(MuSigDisputeCaseDataMessage message,
                                               MuSigMediationCase mediationCase) {
        String tradeId = message.getTradeId();
        MuSigMediationRequest mediationRequest = mediationCase.getMuSigMediationRequest();
        boolean changed = mediationCase.setPeerReportedContractHash(message.getContractHash());
        byte[] expectedContractHash = ContractService.getContractHash(mediationRequest.getContract());
        if (!Arrays.equals(expectedContractHash, message.getContractHash())) {
            Role causingRole = resolveSenderRole(mediationRequest.getContract(), message.getSenderNetworkId().getId());
            changed |= mediationCase.addIssues(List.of(new MuSigMediationIssue(
                    causingRole,
                    MuSigMediationIssueType.PEER_CONTRACT_HASH_MISMATCH,
                    Optional.of("expected=" + Hex.encode(expectedContractHash) + ", reported=" + Hex.encode(message.getContractHash()))
            )));
            log.warn("MuSigDisputeCaseDataMessage for trade {} has mismatching contract hash. expected={}, reported={}",
                    tradeId,
                    Hex.encode(expectedContractHash),
                    Hex.encode(message.getContractHash()));
        }

        muSigOpenTradeChannelService.findChannelByTradeId(tradeId)
                .ifPresentOrElse(channel -> message.getChatMessages().forEach(chatMessage ->
                                muSigOpenTradeChannelService.addMessage(chatMessage, channel)),
                        () -> log.warn("Ignoring chat messages from MuSigDisputeCaseDataMessage for unknown channel on trade {}.",
                                tradeId));

        if (changed) {
            persist();
        }
    }

    private PaymentDetailsVerification verifyPaymentDetails(MuSigContract contract,
                                                            MuSigDisputeCasePaymentDetailsResponse response,
                                                            Role causingRole) {
        MuSigDisputePaymentDetailsVerifier.Result result = MuSigDisputePaymentDetailsVerifier.verify(contract,
                response.getTakerAccountPayload(),
                response.getMakerAccountPayload());
        List<MuSigMediationIssue> issues = Stream.concat(
                        result.takerMismatchDetails().stream()
                                .map(details -> new MuSigMediationIssue(
                                        causingRole,
                                        MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                                        Optional.of(details))),
                        result.makerMismatchDetails().stream()
                                .map(details -> new MuSigMediationIssue(
                                        causingRole,
                                        MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                                        Optional.of(details))))
                .toList();

        return new PaymentDetailsVerification(result.takerAccountPayloadMatches(),
                result.makerAccountPayloadMatches(),
                issues);
    }

    private record PaymentDetailsVerification(boolean takerAccountPayloadMatches,
                                              boolean makerAccountPayloadMatches,
                                              List<MuSigMediationIssue> issues) {
    }

    private Optional<MuSigMediationCase> findMediationCase(String tradeId) {
        return getMediationCases().stream()
                .filter(item -> item.getMuSigMediationRequest().getTradeId().equals(tradeId))
                .findAny();
    }

    private void addNewMediationCase(MuSigMediationCase muSigMediationCase) {
        getMediationCases().add(muSigMediationCase);
        persist();
    }

    private void sendMediationCaseStateChangeTradeLogMessage(MuSigMediationCase muSigMediationCase) {
        MediationCaseState mediationCaseState = muSigMediationCase.getMediationCaseState();
        muSigOpenTradeChannelService.findChannelByTradeId(muSigMediationCase.getMuSigMediationRequest().getTradeId())
                .ifPresent(channel -> {
                    String message;
                    if (mediationCaseState == MediationCaseState.RE_OPENED) {
                        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                        message = Res.encode("authorizedRole.mediator.message.mediationCaseReOpened");
                    } else if (mediationCaseState == MediationCaseState.CLOSED) {
                        // Closed mediation case still keeps mediator chat participation active.
                        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                        message = Res.encode("authorizedRole.mediator.message.mediationCaseClosed");
                    } else {
                        return;
                    }

                    muSigOpenTradeChannelService.sendTradeLogMessage(message, channel);
                });
    }

    private void sendMediationCaseStateChangeMessage(MuSigMediationCase muSigMediationCase) {
        MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
        MediationCaseState mediationCaseState = muSigMediationCase.getMediationCaseState();
        Optional<MuSigMediationResult> muSigMediationResult = muSigMediationCase.getMuSigMediationResult();
        findMyMediatorUserIdentity(muSigMediationRequest.getContract().getMediator())
                .ifPresent(myUserIdentity -> {
                    String id = StringUtils.createUid();
                    MuSigMediationStateChangeMessage message = new MuSigMediationStateChangeMessage(id,
                            muSigMediationRequest.getTradeId(),
                            myUserIdentity.getNetworkIdWithKeyPair().getNetworkId(),
                            mediationCaseState,
                            muSigMediationResult,
                            muSigMediationCase.getMediationResultSignature());
                    NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

                    networkService.confidentialSend(message,
                            muSigMediationRequest.getRequester().getNetworkId(),
                            networkIdWithKeyPair);

                    networkService.confidentialSend(message,
                            muSigMediationRequest.getPeer().getNetworkId(),
                            networkIdWithKeyPair);
                });
    }

    private byte[] createMediationResultSignature(MuSigMediationCase muSigMediationCase,
                                                  MuSigMediationResult muSigMediationResult) {
        MuSigMediationRequest mediationRequest = muSigMediationCase.getMuSigMediationRequest();
        return findMyMediatorUserIdentity(mediationRequest.getContract().getMediator())
                .map(myUserIdentity -> {
                    try {
                        return MuSigMediationResultService.signMediationResult(
                                muSigMediationResult,
                                myUserIdentity.getNetworkIdWithKeyPair().getKeyPair());
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException("Could not sign MuSigMediationStateChangeMessage for trade " + mediationRequest.getTradeId(), e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Could not sign MuSigMediationStateChangeMessage because mediator identity was not found."));
    }

    private void leaveChannel(String tradeId) {
        muSigOpenTradeChannelService.findChannelByTradeId(tradeId)
                .ifPresentOrElse(leavePrivateChatManager::leaveChannel,
                        () -> log.warn("Ignoring leaveChat for unknown channel on trade {}.",
                                tradeId));
    }
}
