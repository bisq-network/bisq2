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

package bisq.trade.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.mu_sig.MuSigArbitrationResult;
import bisq.support.arbitration.mu_sig.MuSigArbitrationResultService;
import bisq.support.arbitration.mu_sig.MuSigArbitrationStateChangeMessage;
import bisq.support.mediation.MediationCaseState;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultAcceptanceMessage;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService;
import bisq.trade.mu_sig.mediation.MuSigTraderMediationService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import static bisq.trade.MuSigDisputeState.isArbitrationState;
import static bisq.trade.MuSigDisputeState.isMediationState;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@RequiredArgsConstructor
final class MuSigTradeDisputeService {
    private final BannedUserService bannedUserService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final MuSigTraderMediationService muSigTraderMediationService;
    private final MuSigTraderArbitrationService muSigTraderArbitrationService;
    private final Function<String, Optional<MuSigTrade>> findTrade;
    private final Runnable persist;

    private final Map<String, Set<EnvelopePayloadMessage>> pendingDisputeMessagesByTradeId = new ConcurrentHashMap<>();

    public void requestMediation(MuSigTrade trade) {
        checkArgument(!bannedUserService.isUserProfileBanned(trade.getMyIdentity().getNetworkId()));
        MuSigContract contract = trade.getContract();
        Optional<UserProfile> mediator = contract.getMediator();
        if (mediator.isPresent()) {
            MuSigTradeDispute tradeDispute = trade.getTradeDispute();
            MuSigDisputeState current = tradeDispute.getDisputeState();
            if (current != MuSigDisputeState.NO_DISPUTE) {
                log.warn("Cannot request mediation for trade {} because not in the right state.",
                        trade.getId());
                return;
            }

            MuSigOpenTradeChannel channel = findChannelByTradeId(trade.getId()).orElseThrow();

            tradeDispute.setDisputeState(MuSigDisputeState.MEDIATION_REQUESTED);
            persist.run();
            muSigTraderMediationService.requestMediation(trade.getId(), trade.getMyIdentity(),
                    trade.getPeer(), mediator.get(), contract, channel);
        }
    }

    public void acceptMediationResult(MuSigTrade trade) {
        applyMediationResultAcceptance(trade, true);
    }

    public void rejectMediationResult(MuSigTrade trade) {
        applyMediationResultAcceptance(trade, false);
    }

    public void requestArbitration(MuSigTrade trade) {
        checkArgument(!bannedUserService.isUserProfileBanned(trade.getMyIdentity().getNetworkId()));
        MuSigContract contract = trade.getContract();
        Optional<UserProfile> arbitrator = contract.getArbitrator();
        if (arbitrator.isPresent()) {
            MuSigTradeDispute tradeDispute = trade.getTradeDispute();
            MuSigDisputeState current = tradeDispute.getDisputeState();
            if (current != MuSigDisputeState.MEDIATION_CLOSED) {
                log.warn("Cannot request arbitration for trade {} because not in the right state.",
                        trade.getId());
                return;
            }

            MuSigOpenTradeChannel channel = findChannelByTradeId(trade.getId()).orElseThrow();

            tradeDispute.setDisputeState(MuSigDisputeState.ARBITRATION_REQUESTED);
            persist.run();
            muSigTraderArbitrationService.requestArbitration(
                    trade.getId(), trade.getMyIdentity(), trade.getPeer(),
                    arbitrator.get(), contract,
                    tradeDispute.getMuSigMediationResult().orElseThrow(),
                    tradeDispute.getMediationResultSignature().orElseThrow(),
                    channel);
        }
    }

    // Must be called while holding the outer disputeStateLock.
    public void onDisputeMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigMediationStateChangeMessage message) {
            findTradeAndChannelOrQueue(message.getTradeId(), envelopePayloadMessage)
                    .flatMap(tradeAndChannel -> verifyMediationStateChangeMessage(message, tradeAndChannel, bannedUserService))
                    .ifPresent(tradeAndChannel -> processMediationStateChangeMessage(message, tradeAndChannel));
        } else if (envelopePayloadMessage instanceof MuSigMediationResultAcceptanceMessage message) {
            findTradeAndChannelOrQueue(message.getTradeId(), envelopePayloadMessage)
                    .flatMap(tradeAndChannel -> verifyMediationResultAcceptanceMessage(message, tradeAndChannel, bannedUserService))
                    .ifPresent(tradeAndChannel -> processMediationResultAcceptanceMessage(message, tradeAndChannel));
        } else if (envelopePayloadMessage instanceof MuSigDisputeCasePaymentDetailsRequest message) {
            findTradeAndChannelOrQueue(message.getTradeId(), envelopePayloadMessage)
                    .flatMap(tradeAndChannel -> verifyDisputeCasePaymentDetailsRequest(message, tradeAndChannel, bannedUserService))
                    .ifPresent(tradeAndChannel -> processDisputeCasePaymentDetailsRequest(message, tradeAndChannel));
        } else if (envelopePayloadMessage instanceof MuSigArbitrationStateChangeMessage message) {
            findTradeAndChannelOrQueue(message.getTradeId(), envelopePayloadMessage)
                    .flatMap(tradeAndChannel -> verifyArbitrationStateChangeMessage(message, tradeAndChannel, bannedUserService))
                    .ifPresent(tradeAndChannel -> processArbitrationStateChangeMessage(message, tradeAndChannel));
        }
    }

    // Must be called while holding the outer disputeStateLock.
    public void maybeProcessPendingDisputeMessages(String tradeId) {
        Set<EnvelopePayloadMessage> pendingMessages = pendingDisputeMessagesByTradeId.remove(tradeId);
        if (pendingMessages != null) {
            pendingMessages.forEach(this::onDisputeMessage);
        }
    }

    private void applyMediationResultAcceptance(MuSigTrade trade, boolean mediationResultAccepted) {
        checkArgument(trade.getTradeDispute().getMuSigMediationResult().isPresent());
        MuSigOpenTradeChannel channel = findChannelByTradeId(trade.getId()).orElseThrow();
        if (trade.getMyself().setMediationResultAccepted(mediationResultAccepted)) {
            persist.run();
            muSigTraderMediationService.sendMediationResultAcceptanceMessage(
                    trade.getId(), trade.getMyIdentity(), trade.getPeer(), mediationResultAccepted, channel);
        }
    }

    private static Optional<MuSigTradeAndChannel> verifyMediationStateChangeMessage(
            MuSigMediationStateChangeMessage message,
            MuSigTradeAndChannel tradeAndChannel,
            BannedUserService bannedUserService) {
        MuSigTrade trade = tradeAndChannel.trade();
        Optional<UserProfile> mediator = trade.getContract().getMediator();
        if (mediator.isEmpty()) {
            log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because mediator is missing in contract.",
                    message.getTradeId());
            return Optional.empty();
        }
        if (!mediator.orElseThrow().getId().equals(message.getSenderNetworkId().getId())) {
            log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} with unexpected senderNetworkId {}.",
                    message.getTradeId(), message.getSenderNetworkId());
            return Optional.empty();
        }

        if (bannedUserService.isUserProfileBanned(message.getSenderNetworkId())) {
            log.warn("Ignoring MuSigMediationStateChangeMessage as sender is banned");
            return Optional.empty();
        }
        return Optional.of(tradeAndChannel);
    }

    private void processMediationStateChangeMessage(MuSigMediationStateChangeMessage message,
                                                    MuSigTradeAndChannel tradeAndChannel) {
        MuSigTrade trade = tradeAndChannel.trade();
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        MuSigDisputeState current = tradeDispute.getDisputeState();
        if (isArbitrationState(current)) {
            return;
        }

        MediationCaseState mediationCaseState = message.getMediationCaseState();
        boolean shouldSendPeerReport = false;
        MuSigDisputeState next = current;

        if (mediationCaseState == MediationCaseState.OPEN) {
            if (current == MuSigDisputeState.NO_DISPUTE || current == MuSigDisputeState.MEDIATION_REQUESTED) {
                next = MuSigDisputeState.MEDIATION_OPEN;
                shouldSendPeerReport = current == MuSigDisputeState.NO_DISPUTE;
            } else if (current == MuSigDisputeState.MEDIATION_OPEN) {
                return;
            }
        } else if (mediationCaseState == MediationCaseState.RE_OPENED) {
            if (current == MuSigDisputeState.MEDIATION_CLOSED) {
                next = MuSigDisputeState.MEDIATION_RE_OPENED;
            } else if (current == MuSigDisputeState.MEDIATION_RE_OPENED) {
                return;
            } else {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        } else if (mediationCaseState == MediationCaseState.CLOSED) {
            if (current == MuSigDisputeState.MEDIATION_OPEN || current == MuSigDisputeState.MEDIATION_RE_OPENED) {
                next = MuSigDisputeState.MEDIATION_CLOSED;
                Optional<MuSigMediationResult> incomingResult = message.getMuSigMediationResult();
                Optional<byte[]> incomingResultSignature = message.getMediationResultSignature();
                if (incomingResult.isEmpty() || incomingResultSignature.isEmpty()) {
                    log.warn("Ignoring CLOSED MuSigMediationStateChangeMessage without MuSigMediationResult/Signature for trade {}.",
                            message.getTradeId());
                    return;
                }

                Optional<MuSigMediationResult> currentResult = tradeDispute.getMuSigMediationResult();
                Optional<byte[]> currentResultSignature = tradeDispute.getMediationResultSignature();
                if (!isMediationResultMessageValid(trade.getContract(), message)) {
                    return;
                }
                if (currentResult.isEmpty() && currentResultSignature.isEmpty()) {
                    tradeDispute.setMuSigMediationResult(incomingResult.orElseThrow());
                    tradeDispute.setMediationResultSignature(incomingResultSignature.orElseThrow());
                } else if (!currentResult.orElseThrow().equals(incomingResult.orElseThrow()) ||
                        !Arrays.equals(currentResultSignature.orElseThrow(), incomingResultSignature.orElseThrow())) {
                    log.warn("Ignoring changed MuSigMediationResult/Signature for trade {} because result/signature cannot be changed once set.",
                            message.getTradeId());
                }
            } else if (current == MuSigDisputeState.MEDIATION_CLOSED) {
                return;
            } else {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        }

        if (next != current) {
            tradeDispute.setDisputeState(next);
            persist.run();
            muSigTraderMediationService.applyMediationStateToChannel(trade.getId(), next, current, tradeAndChannel.channel());

            if (shouldSendPeerReport) {
                MuSigContract contract = trade.getContract();
                muSigTraderMediationService.sendDisputeCaseDataMessage(
                        trade.getId(), trade.getMyIdentity(), contract.getMediator().orElseThrow(), contract);
            }
            maybeProcessPendingDisputeMessages(trade.getId());
        }
    }

    private boolean isMediationResultMessageValid(MuSigContract contract,
                                                  MuSigMediationStateChangeMessage message) {
        try {
            if (message.getMediationResultSignature().isEmpty()) {
                log.warn("Ignoring MuSigMediationResult for trade {} because mediator signature is missing.",
                        message.getTradeId());
                return false;
            }
            if (!MuSigMediationResultService.verifyMediationResult(message.getMuSigMediationResult().orElseThrow(),
                    message.getMediationResultSignature().orElseThrow(),
                    contract,
                    contract.getMediator().orElseThrow().getPublicKey())) {
                log.warn("Ignoring MuSigMediationResult for trade {} because mediator signature verification failed.",
                        message.getTradeId());
                return false;
            }
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Ignoring MuSigMediationResult for trade {} because mediator signature verification failed.",
                    message.getTradeId(), e);
            return false;
        }
    }

    private static Optional<MuSigTradeAndChannel> verifyMediationResultAcceptanceMessage(
            MuSigMediationResultAcceptanceMessage message,
            MuSigTradeAndChannel tradeAndChannel,
            BannedUserService bannedUserService) {
        if (!tradeAndChannel.trade().getPeer().getNetworkId().getId().equals(message.getSenderNetworkId().getId())) {
            log.warn("Ignoring MuSigMediationResultAcceptanceMessage with unexpected senderNetworkId {} for trade {}.",
                    message.getSenderNetworkId(), message.getTradeId());
            return Optional.empty();
        }

        if (bannedUserService.isUserProfileBanned(message.getSenderNetworkId())) {
            log.warn("Ignoring MuSigMediationResultAcceptanceMessage as sender is banned");
            return Optional.empty();
        }
        return Optional.of(tradeAndChannel);
    }

    private void processMediationResultAcceptanceMessage(MuSigMediationResultAcceptanceMessage message,
                                                         MuSigTradeAndChannel tradeAndChannel) {
        MuSigTrade trade = tradeAndChannel.trade();
        if (trade.getTradeDispute().getMuSigMediationResult().isEmpty()) {
            addPendingDisputeMessage(trade.getId(), message);
            return;
        }

        if (trade.getPeer().setMediationResultAccepted(message.isMediationResultAccepted())) {
            persist.run();
        }
    }

    private static Optional<MuSigTradeAndChannel> verifyDisputeCasePaymentDetailsRequest(
            MuSigDisputeCasePaymentDetailsRequest message,
            MuSigTradeAndChannel tradeAndChannel,
            BannedUserService bannedUserService) {
        MuSigTrade trade = tradeAndChannel.trade();
        NetworkId sender = message.getSenderNetworkId();
        if (!isSenderMediator(sender, trade) && !isSenderArbitrator(sender, trade)) {
            log.warn("Ignoring MuSigDisputeCasePaymentDetailsRequest for trade {} with unexpected senderNetworkId {}.",
                    message.getTradeId(), message.getSenderNetworkId());
            return Optional.empty();
        }

        if (bannedUserService.isUserProfileBanned(message.getSenderNetworkId())) {
            log.warn("Ignoring MuSigDisputeCasePaymentDetailsRequest as sender is banned");
            return Optional.empty();
        }
        return Optional.of(tradeAndChannel);
    }

    private void processDisputeCasePaymentDetailsRequest(MuSigDisputeCasePaymentDetailsRequest message,
                                                         MuSigTradeAndChannel tradeAndChannel) {
        MuSigTrade trade = tradeAndChannel.trade();
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        NetworkId sender = message.getSenderNetworkId();

        if (isSenderMediator(sender, trade)) {
            if (isArbitrationState(tradeDispute.getDisputeState())) {
                log.warn("Ignoring MuSigDisputeCasePaymentDetailsRequest for trade {} from mediator {} because trade is already in arbitration state {}.",
                        message.getTradeId(), sender, tradeDispute.getDisputeState());
                return;
            }
            if (!isMediationState(tradeDispute.getDisputeState())) {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        } else if (isSenderArbitrator(sender, trade)) {
            if (!isArbitrationState(tradeDispute.getDisputeState())) {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        } else {
            log.warn("Ignoring MuSigDisputeCasePaymentDetailsRequest for trade {} with unexpected senderNetworkId {}.",
                    message.getTradeId(), sender);
            return;
        }

        if (trade.getMyself().getAccountPayload().isEmpty() || trade.getPeer().getAccountPayload().isEmpty()) {
            log.warn("Ignoring MuSigDisputeCasePaymentDetailsRequest for trade {} because account payloads are incomplete.",
                    message.getTradeId());
            return;
        }
        muSigTraderMediationService.sendDisputeCasePaymentDetailsResponse(
                trade.getId(), trade.getMyIdentity(), message.getSenderNetworkId(),
                trade.getTaker().getAccountPayload().orElseThrow(),
                trade.getMaker().getAccountPayload().orElseThrow());
    }

    private static Optional<MuSigTradeAndChannel> verifyArbitrationStateChangeMessage(
            MuSigArbitrationStateChangeMessage message,
            MuSigTradeAndChannel tradeAndChannel,
            BannedUserService bannedUserService) {
        MuSigTrade trade = tradeAndChannel.trade();
        Optional<UserProfile> arbitrator = trade.getContract().getArbitrator();
        if (arbitrator.isEmpty()) {
            log.warn("Ignoring MuSigArbitrationStateChangeMessage for trade {} because arbitrator is missing in contract.",
                    message.getTradeId());
            return Optional.empty();
        }
        if (!arbitrator.orElseThrow().getId().equals(message.getSenderNetworkId().getId())) {
            log.warn("Ignoring MuSigArbitrationStateChangeMessage for trade {} with unexpected senderNetworkId {}.",
                    message.getTradeId(), message.getSenderNetworkId());
            return Optional.empty();
        }

        if (bannedUserService.isUserProfileBanned(message.getSenderNetworkId())) {
            log.warn("Ignoring MuSigArbitrationStateChangeMessage as sender is banned");
            return Optional.empty();
        }
        return Optional.of(tradeAndChannel);
    }

    private void processArbitrationStateChangeMessage(MuSigArbitrationStateChangeMessage message,
                                                      MuSigTradeAndChannel tradeAndChannel) {
        MuSigTrade trade = tradeAndChannel.trade();
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        MuSigDisputeState current = tradeDispute.getDisputeState();

        ArbitrationCaseState arbitrationCaseState = message.getArbitrationCaseState();
        MuSigDisputeState next = current;

        if (arbitrationCaseState == ArbitrationCaseState.OPEN) {
            if (current == MuSigDisputeState.ARBITRATION_REQUESTED || current == MuSigDisputeState.MEDIATION_CLOSED) {
                next = MuSigDisputeState.ARBITRATION_OPEN;
            } else if (current == MuSigDisputeState.ARBITRATION_OPEN) {
                return;
            } else {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        } else if (arbitrationCaseState == ArbitrationCaseState.CLOSED) {
            if (current == MuSigDisputeState.ARBITRATION_OPEN) {
                next = MuSigDisputeState.ARBITRATION_CLOSED;
                Optional<MuSigArbitrationResult> incomingResult = message.getMuSigArbitrationResult();
                Optional<byte[]> incomingResultSignature = message.getArbitrationResultSignature();
                if (incomingResult.isEmpty() || incomingResultSignature.isEmpty()) {
                    log.warn("Ignoring CLOSED MuSigArbitrationStateChangeMessage without MuSigArbitrationResult/Signature for trade {}.",
                            message.getTradeId());
                    return;
                }

                Optional<MuSigArbitrationResult> currentResult = tradeDispute.getMuSigArbitrationResult();
                Optional<byte[]> currentResultSignature = tradeDispute.getArbitrationResultSignature();
                if (!isArbitrationResultMessageValid(trade.getContract(), message)) {
                    return;
                }
                if (currentResult.isEmpty() && currentResultSignature.isEmpty()) {
                    tradeDispute.setMuSigArbitrationResult(incomingResult.orElseThrow());
                    tradeDispute.setArbitrationResultSignature(incomingResultSignature.orElseThrow());
                } else if (!currentResult.orElseThrow().equals(incomingResult.orElseThrow()) ||
                        !Arrays.equals(currentResultSignature.orElseThrow(), incomingResultSignature.orElseThrow())) {
                    log.warn("Ignoring changed MuSigArbitrationResult/Signature for trade {} because result/signature cannot be changed once set.",
                            message.getTradeId());
                }
            } else if (current == MuSigDisputeState.ARBITRATION_CLOSED) {
                return;
            } else {
                addPendingDisputeMessage(trade.getId(), message);
                return;
            }
        }

        if (next != current) {
            tradeDispute.setDisputeState(next);
            persist.run();
            muSigTraderArbitrationService.applyArbitrationStateToChannel(trade.getId(), next, current, tradeAndChannel.channel());
            maybeProcessPendingDisputeMessages(trade.getId());
        }
    }

    private boolean isArbitrationResultMessageValid(MuSigContract contract,
                                                    MuSigArbitrationStateChangeMessage message) {
        try {
            if (message.getArbitrationResultSignature().isEmpty()) {
                log.warn("Ignoring MuSigArbitrationResult for trade {} because arbitrator signature is missing.",
                        message.getTradeId());
                return false;
            }
            if (!MuSigArbitrationResultService.verifyArbitrationResult(message.getMuSigArbitrationResult().orElseThrow(),
                    message.getArbitrationResultSignature().orElseThrow(),
                    contract,
                    contract.getArbitrator().orElseThrow().getPublicKey())) {
                log.warn("Ignoring MuSigArbitrationResult for trade {} because arbitrator signature verification failed.",
                        message.getTradeId());
                return false;
            }
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Ignoring MuSigArbitrationResult for trade {} because arbitrator signature verification failed.",
                    message.getTradeId(), e);
            return false;
        }
    }

    private static boolean isSenderMediator(NetworkId sender, MuSigTrade trade) {
        return trade.getContract().getMediator()
                .map(profile -> profile.getId().equals(sender.getId()))
                .orElse(false);
    }

    private static boolean isSenderArbitrator(NetworkId sender, MuSigTrade trade) {
        return trade.getContract().getArbitrator()
                .map(profile -> profile.getId().equals(sender.getId()))
                .orElse(false);
    }

    private Optional<MuSigOpenTradeChannel> findChannelByTradeId(String tradeId) {
        return muSigOpenTradeChannelService.findChannelByTradeId(tradeId);
    }

    private Optional<MuSigTradeAndChannel> findTradeAndChannelOrQueue(String tradeId,
                                                                      EnvelopePayloadMessage message) {
        Optional<MuSigTrade> trade = findTrade.apply(tradeId);
        Optional<MuSigOpenTradeChannel> channel = findChannelByTradeId(tradeId);
        if (trade.isPresent() && channel.isPresent()) {
            removePendingDisputeMessage(tradeId, message);
            return Optional.of(new MuSigTradeAndChannel(trade.get(), channel.get()));
        }
        addPendingDisputeMessage(tradeId, message);
        return Optional.empty();
    }

    private void addPendingDisputeMessage(String tradeId, EnvelopePayloadMessage message) {
        pendingDisputeMessagesByTradeId
                .computeIfAbsent(tradeId, key -> new CopyOnWriteArraySet<>())
                .add(message);
    }

    private void removePendingDisputeMessage(String tradeId, EnvelopePayloadMessage message) {
        pendingDisputeMessagesByTradeId.computeIfPresent(tradeId, (key, pendingMessages) -> {
            pendingMessages.remove(message);
            return pendingMessages.isEmpty() ? null : pendingMessages;
        });
    }

    private record MuSigTradeAndChannel(MuSigTrade trade, MuSigOpenTradeChannel channel) {
    }
}
