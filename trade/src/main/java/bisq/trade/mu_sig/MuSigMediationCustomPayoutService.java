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

import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationPayoutResolver;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import bisq.trade.mu_sig.messages.network.MuSigTradeMessage;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
final class MuSigMediationCustomPayoutService {
    private static final Set<MuSigTradeState> CUSTOM_PAYOUT_SIGNING_STATES = Set.of(
            MuSigTradeState.DEPOSIT_TX_CONFIRMED,
            MuSigTradeState.BUYER_INITIATED_PAYMENT,
            MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE);

    private final Map<String, CopyOnWriteArrayList<MuSigTradeMessage>> pendingMessagesByTradeId =
            new ConcurrentHashMap<>();

    boolean canSignCustomPayout(MuSigTrade trade) {
        if (!CUSTOM_PAYOUT_SIGNING_STATES.contains(trade.getTradeState())) {
            return false;
        }

        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        if (tradeDispute.getDisputeState() != MuSigDisputeState.MEDIATION_CLOSED) {
            return false;
        }

        Optional<MuSigMediationResult> optionalMediationResult = tradeDispute.getMuSigMediationResult();
        Optional<byte[]> optionalMediationResultSignature = tradeDispute.getMediationResultSignature();
        if (optionalMediationResult.isEmpty() || optionalMediationResultSignature.isEmpty()) {
            return false;
        }

        MuSigMediationResult mediationResult = optionalMediationResult.orElseThrow();
        if (mediationResult.getMediationPayoutDistributionType() == MediationPayoutDistributionType.NO_PAYOUT ||
                trade.getMyself().isMediationResultRejected() ||
                trade.getPeer().isMediationResultRejected() ||
                trade.getMyself().getCustomPayoutData().isPresent()) {
            return false;
        }

        Optional<UserProfile> optionalMediator = trade.getContract().getMediator();
        if (optionalMediator.isEmpty()) {
            log.warn("Cannot sign custom payout for trade {} because the mediator is missing from the contract.",
                    trade.getId());
            return false;
        }
        if (!isMediationResultValid(
                trade,
                mediationResult,
                optionalMediationResultSignature.orElseThrow(),
                optionalMediator.orElseThrow())) {
            return false;
        }

        try {
            Optional<MuSigMediationPayoutResolver.PayoutContext> optionalPayoutContext =
                    MuSigMediationPayoutResolver.createPayoutContext(trade.getContract());
            if (optionalPayoutContext.isEmpty()) {
                log.warn("Cannot sign custom payout for trade {} because the payout context is unavailable.",
                        trade.getId());
                return false;
            }
            MuSigMediationPayoutResolver.checkPayoutAmounts(
                    mediationResult.getMediationPayoutDistributionType(),
                    optionalPayoutContext.orElseThrow(),
                    mediationResult.getProposedBuyerPayoutAmount(),
                    mediationResult.getProposedSellerPayoutAmount(),
                    mediationResult.getPayoutAdjustmentPercentage());
        } catch (IllegalArgumentException e) {
            log.warn("Cannot sign custom payout for trade {} because the mediation payout is invalid.",
                    trade.getId(), e);
            return false;
        }

        return true;
    }

    boolean canFinalizeCustomPayout(MuSigTrade trade) {
        if (trade.getTradeState() != MuSigTradeState.CUSTOM_PAYOUT_SIGNED) {
            return false;
        }

        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        if (tradeDispute.getDisputeState() != MuSigDisputeState.MEDIATION_CLOSED ||
                tradeDispute.getMediationResultSignature().isEmpty()) {
            return false;
        }

        Optional<MuSigMediationResult> optionalMediationResult = tradeDispute.getMuSigMediationResult();
        if (optionalMediationResult.isEmpty() ||
                optionalMediationResult.orElseThrow().getMediationPayoutDistributionType() ==
                        MediationPayoutDistributionType.NO_PAYOUT ||
                trade.getMyself().isMediationResultRejected() ||
                trade.getPeer().isMediationResultRejected()) {
            return false;
        }

        Optional<MuSigCustomPayoutPartyData> optionalLocalData = trade.getMyself().getCustomPayoutData();
        Optional<MuSigCustomPayoutPartyData> optionalPeerData = trade.getPeer().getCustomPayoutData();
        if (optionalLocalData.isEmpty() || optionalPeerData.isEmpty()) {
            return false;
        }

        MuSigCustomPayoutPartyData localData = optionalLocalData.orElseThrow();
        MuSigCustomPayoutPartyData peerData = optionalPeerData.orElseThrow();
        if (localData.getMyCustomCloseTradeResponse().isPresent()) {
            return false;
        }

        Optional<String> optionalLocalTxId = localData.getMyCustomPayoutPsbt()
                .map(customPayoutPsbt -> customPayoutPsbt.getTxId());
        Optional<String> optionalPeerTxId = peerData.getPeersCustomPayoutPsbt()
                .map(peersCustomPayoutPsbt -> peersCustomPayoutPsbt.getTxId());
        if (optionalLocalTxId.isEmpty() || optionalPeerTxId.isEmpty()) {
            return false;
        }
        return optionalLocalTxId.orElseThrow().equals(optionalPeerTxId.orElseThrow());
    }

    boolean hasRequiredMessageContext(MuSigTrade trade) {
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        return trade.getProtocolVersion() != null &&
                tradeDispute.getMuSigMediationResult().isPresent() &&
                tradeDispute.getMediationResultSignature().isPresent();
    }

    // Must be called while holding the trade's protocol monitor.
    List<MuSigTradeMessage> getPendingMessagesInReplayOrder(String tradeId) {
        List<MuSigTradeMessage> messages = pendingMessagesByTradeId.get(tradeId);
        if (messages == null) {
            return List.of();
        }

        // A buffered rejection wins over a buffered PSBT, while arrival order is preserved
        // between messages of the same type.
        List<MuSigTradeMessage> orderedMessages = new ArrayList<>(messages.size());
        messages.stream()
                .filter(MuSigMediationResultRejectionMessage.class::isInstance)
                .forEach(orderedMessages::add);
        messages.stream()
                .filter(MuSigCustomPayoutPsbtMessage.class::isInstance)
                .forEach(orderedMessages::add);
        return List.copyOf(orderedMessages);
    }

    void clear() {
        pendingMessagesByTradeId.clear();
    }

    void clearTrade(String tradeId) {
        pendingMessagesByTradeId.remove(tradeId);
    }

    // Before protocol registration, the caller holds pendingMessagesLock; afterwards it holds
    // the trade's protocol monitor. Registration coordinates the handoff between those locks.
    void addPendingMessage(MuSigTradeMessage message) {
        pendingMessagesByTradeId
                .computeIfAbsent(message.getTradeId(), ignored -> new CopyOnWriteArrayList<>())
                .addIfAbsent(message);
    }

    // Must be called while holding the trade's protocol monitor.
    void removePendingMessage(MuSigTradeMessage message) {
        pendingMessagesByTradeId.computeIfPresent(message.getTradeId(), (tradeId, messages) -> {
            messages.remove(message);
            return messages.isEmpty() ? null : messages;
        });
    }

    private boolean isMediationResultValid(MuSigTrade trade,
                                           MuSigMediationResult mediationResult,
                                           byte[] mediationResultSignature,
                                           UserProfile mediator) {
        try {
            if (MuSigMediationResultService.verifyMediationResult(
                    mediationResult,
                    mediationResultSignature,
                    trade.getContract(),
                    mediator.getPublicKey())) {
                return true;
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Cannot sign custom payout for trade {} because mediation result verification failed.",
                    trade.getId(), e);
            return false;
        }

        log.warn("Cannot sign custom payout for trade {} because the mediation result is invalid.", trade.getId());
        return false;
    }

}
