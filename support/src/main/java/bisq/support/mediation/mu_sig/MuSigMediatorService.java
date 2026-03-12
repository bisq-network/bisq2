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
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.options.OfferOptionUtil;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

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
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

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
        if (envelopePayloadMessage instanceof MuSigMediationRequest) {
            processMediationRequest((MuSigMediationRequest) envelopePayloadMessage);
        } else if (envelopePayloadMessage instanceof MuSigPaymentDetailsResponse) {
            processPaymentDetailsResponse((MuSigPaymentDetailsResponse) envelopePayloadMessage);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public MuSigMediationResult createMuSigMediationResult(MediationResultReason mediationResultReason,
                                                           MediationPayoutDistributionType mediationPayoutDistributionType,
                                                           Optional<Long> proposedBuyerPayoutAmount,
                                                           Optional<Long> proposedSellerPayoutAmount,
                                                           Optional<Double> payoutAdjustmentPercentage,
                                                           Optional<String> summaryNotes) {
        return new MuSigMediationResult(mediationResultReason, mediationPayoutDistributionType,
                proposedBuyerPayoutAmount, proposedSellerPayoutAmount,
                payoutAdjustmentPercentage, summaryNotes);
    }

    public void closeMediationCase(MuSigMediationCase muSigMediationCase, MuSigMediationResult muSigMediationResult) {
        Optional<MuSigMediationResult> existingResult = muSigMediationCase.getMuSigMediationResult().get();
        if (existingResult.filter(result -> !result.equals(muSigMediationResult)).isPresent()) {
            log.warn("Ignoring changed MuSigMediationResult for trade {} because result cannot be changed once set.",
                    muSigMediationCase.getMuSigMediationRequest().getTradeId());
        }

        MuSigMediationResult resultToUse = existingResult.orElse(muSigMediationResult);
        boolean resultChanged = muSigMediationCase.setMuSigMediationResult(resultToUse);
        boolean stateChanged = muSigMediationCase.setMediationCaseState(MediationCaseState.CLOSED);
        if (resultChanged || stateChanged) {
            persist();
            sendMediationCaseStateChangeMessage(muSigMediationCase, Optional.of(resultToUse));
            sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
        }
    }

    public void removeMediationCase(MuSigMediationCase muSigMediationCase) {
        getMediationCases().remove(muSigMediationCase);
        persist();
    }

    public void reOpenMediationCase(MuSigMediationCase muSigMediationCase) {
        if (muSigMediationCase.setMediationCaseState(MediationCaseState.RE_OPENED)) {
            persist();
            sendMediationCaseStateChangeMessage(muSigMediationCase, Optional.empty());
            sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
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
        MuSigPaymentDetailsRequest message = new MuSigPaymentDetailsRequest(
                muSigMediationRequest.getTradeId(),
                myUserIdentity.getUserProfile().getId()
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

    public void closeReOpenedMediationCase(MuSigMediationCase muSigMediationCase) {
        Optional<MuSigMediationResult> existingResult = muSigMediationCase.getMuSigMediationResult().get();
        if (existingResult.isEmpty()) {
            log.warn("Cannot close re-opened mediation case for trade {} because MuSigMediationResult is missing.",
                    muSigMediationCase.getMuSigMediationRequest().getTradeId());
            return;
        }
        if (muSigMediationCase.setMediationCaseState(MediationCaseState.CLOSED)) {
            persist();
            sendMediationCaseStateChangeMessage(muSigMediationCase, existingResult);
            sendMediationCaseStateChangeTradeLogMessage(muSigMediationCase);
        }
    }

    public ObservableSet<MuSigMediationCase> getMediationCases() {
        return persistableStore.getMuSigMediationCases();
    }

    public Optional<UserIdentity> findMyMediatorUserIdentity(Optional<UserProfile> mediator) {
        return findMyMediatorUserIdentities()
                .filter(e -> mediator.isPresent())
                .filter(userIdentity -> userIdentity.getUserProfile().getId().equals(mediator.orElseThrow().getId()))
                .findAny();
    }

    public Stream<UserIdentity> findMyMediatorUserIdentities() {
        // If we got banned we still want to show the admin UI
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processMediationRequest(MuSigMediationRequest muSigMediationRequest) {
        UserProfile requester = muSigMediationRequest.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        MuSigContract contract = muSigMediationRequest.getContract();
        findMyMediatorUserIdentity(contract.getMediator()).ifPresent(myUserIdentity -> {
            String tradeId = muSigMediationRequest.getTradeId();
            UserProfile peer = muSigMediationRequest.getPeer();
            List<MuSigOpenTradeMessage> chatMessages = muSigMediationRequest.getChatMessages();
            MuSigOpenTradeChannel channel = muSigOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    tradeId,
                    myUserIdentity,
                    requester,
                    peer
            );

            muSigOpenTradeChannelService.setIsInMediation(channel, true);

            chatMessages.forEach(chatMessage ->
                    muSigOpenTradeChannelService.addMessage(chatMessage, channel));

            // We apply the muSigMediationCase after the channel is set up as clients will expect a channel.
            MuSigMediationCase muSigMediationCase = new MuSigMediationCase(muSigMediationRequest);
            addNewMediationCase(muSigMediationCase);

            NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

            // Send to requester
            networkService.confidentialSend(new MuSigMediatorsResponse(tradeId),
                    requester.getNetworkId(),
                    networkIdWithKeyPair);
            muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));

            // Send to peer
            networkService.confidentialSend(new MuSigMediatorsResponse(tradeId),
                    peer.getNetworkId(),
                    networkIdWithKeyPair);
            muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
        });
    }

    private void processPaymentDetailsResponse(MuSigPaymentDetailsResponse response) {
        String tradeId = response.getTradeId();
        findMediationCase(tradeId).ifPresentOrElse(mediationCase -> {
                    String senderUserProfileId = response.getSenderUserProfileId();
                    MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
                    boolean isRequester = muSigMediationRequest.getRequester().getId().equals(senderUserProfileId);
                    boolean isPeer = muSigMediationRequest.getPeer().getId().equals(senderUserProfileId);
                    if (!isRequester && !isPeer) {
                        log.warn("Ignoring MuSigPaymentDetailsResponse for trade {} with unknown senderUserProfileId {}.",
                                tradeId, senderUserProfileId);
                        return;
                    }
                    if (bannedUserService.isUserProfileBanned(senderUserProfileId)) {
                        log.warn("Ignoring MuSigPaymentDetailsResponse for trade {} from banned senderUserProfileId {}.",
                                tradeId, senderUserProfileId);
                        return;
                    }
                    Role causingRole = resolveCausingRole(muSigMediationRequest.getContract(), senderUserProfileId);
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
                        log.warn("MuSigPaymentDetailsResponse for trade {} has verification issues: {}",
                                tradeId, verification.issues());
                    }
                    if (changed) {
                        persist();
                    }
                },
                () -> log.warn("Ignoring MuSigPaymentDetailsResponse for unknown trade {}.", tradeId));
    }

    private PaymentDetailsVerification verifyPaymentDetails(MuSigContract contract,
                                                            MuSigPaymentDetailsResponse response,
                                                            Role causingRole) {
        List<MuSigMediationIssue> issues = new ArrayList<>();
        String offerId = contract.getOffer().getId();
        boolean takerAccountPayloadMatches = true;
        boolean makerAccountPayloadMatches = true;

        byte[] takerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(response.getTakerAccountPayload(), offerId);
        if (contract.getTaker().getSaltedAccountPayloadHash()
                .filter(expectedHash -> Arrays.equals(takerSaltedAccountPayloadHash, expectedHash))
                .isEmpty()) {
            takerAccountPayloadMatches = false;
            issues.add(new MuSigMediationIssue(
                    causingRole,
                    MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                    Optional.of(response.getTakerAccountPayload().getAccountDataDisplayString())));
        }

        byte[] makerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(response.getMakerAccountPayload(), offerId);
        if (contract.getMaker().getSaltedAccountPayloadHash()
                .filter(expectedHash -> Arrays.equals(makerSaltedAccountPayloadHash, expectedHash))
                .isEmpty()) {
            makerAccountPayloadMatches = false;
            issues.add(new MuSigMediationIssue(
                    causingRole,
                    MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH,
                    Optional.of(response.getMakerAccountPayload().getAccountDataDisplayString())));
        }

        return new PaymentDetailsVerification(takerAccountPayloadMatches, makerAccountPayloadMatches, issues);
    }

    private Role resolveCausingRole(MuSigContract contract, String senderUserProfileId) {
        return senderUserProfileId.equals(contract.getOffer().getMakersUserProfileId()) ? Role.MAKER : Role.TAKER;
    }

    private record PaymentDetailsVerification(boolean takerAccountPayloadMatches,
                                              boolean makerAccountPayloadMatches,
                                              List<MuSigMediationIssue> issues) {
    }

    private Optional<MuSigMediationCase> findMediationCase(String tradeId) {
        return getMediationCases().stream()
                .filter(mediationCase -> mediationCase.getMuSigMediationRequest().getTradeId().equals(tradeId))
                .findAny();
    }

    private void addNewMediationCase(MuSigMediationCase muSigMediationCase) {
        getMediationCases().add(muSigMediationCase);
        persist();
    }

    private void sendMediationCaseStateChangeTradeLogMessage(MuSigMediationCase muSigMediationCase) {
        MediationCaseState mediationCaseState = muSigMediationCase.getMediationCaseState().get();
        muSigOpenTradeChannelService.findChannelByTradeId(muSigMediationCase.getMuSigMediationRequest().getTradeId())
                .ifPresent(channel -> {
                    String message;
                    if (mediationCaseState == MediationCaseState.RE_OPENED) {
                        muSigOpenTradeChannelService.setIsInMediation(channel, true);
                        message = Res.encode("authorizedRole.mediator.message.mediationCaseReOpened");
                    } else if (mediationCaseState == MediationCaseState.CLOSED) {
                        // Closed mediation case still keeps mediator chat participation active.
                        muSigOpenTradeChannelService.setIsInMediation(channel, true);
                        message = Res.encode("authorizedRole.mediator.message.mediationCaseClosed");
                    } else {
                        return;
                    }

                    muSigOpenTradeChannelService.sendTradeLogMessage(message, channel);
                });
    }

    private void sendMediationCaseStateChangeMessage(MuSigMediationCase muSigMediationCase,
                                                     Optional<MuSigMediationResult> muSigMediationResult) {
        MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
        MediationCaseState mediationCaseState = muSigMediationCase.getMediationCaseState().get();
        findMyMediatorUserIdentity(muSigMediationRequest.getContract().getMediator())
                .ifPresent(myUserIdentity -> {
                    NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
                    MuSigMediationStateChangeMessage message = new MuSigMediationStateChangeMessage(
                            StringUtils.createUid(),
                            muSigMediationRequest.getTradeId(),
                            mediationCaseState,
                            muSigMediationResult
                    );

                    networkService.confidentialSend(message,
                            muSigMediationRequest.getRequester().getNetworkId(),
                            networkIdWithKeyPair);

                    networkService.confidentialSend(message,
                            muSigMediationRequest.getPeer().getNetworkId(),
                            networkIdWithKeyPair);
                });
    }
}
