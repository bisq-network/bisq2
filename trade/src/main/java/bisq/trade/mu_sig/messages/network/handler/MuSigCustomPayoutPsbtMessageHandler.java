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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig.messages.network.handler;

import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigCustomPayoutPartyData;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeDispute;
import bisq.trade.mu_sig.PeerCustomPayoutPsbt;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public final class MuSigCustomPayoutPsbtMessageHandler
        extends MuSigTradeMessageHandler<MuSigTrade, MuSigCustomPayoutPsbtMessage> {
    private boolean shouldProcess = true;
    private PeerCustomPayoutPsbt peersCustomPayoutPsbt;

    public MuSigCustomPayoutPsbtMessageHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verifyInternal(MuSigCustomPayoutPsbtMessage message) {
        // Intentionally do not call super.verifyInternal(): invalid or stale
        // custom-payout messages must be ignored without failing the trade.
        // A custom payout PSBT can arrive asynchronously after another settlement path has progressed.
        // Invalid or stale messages must therefore be consumed without failing the trade.
        if (!message.getTradeId().equals(trade.getId()) ||
                !message.getSender().equals(trade.getPeer().getNetworkId()) ||
                !message.getReceiver().equals(trade.getMyself().getNetworkId()) ||
                !Objects.equals(message.getProtocolVersion(), trade.getProtocolVersion())) {
            ignoreMessage(message, "unexpected trade-message context");
        }
    }

    @Override
    protected void verify(MuSigCustomPayoutPsbtMessage message) {
        if (!shouldProcess) {
            return;
        }

        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        Optional<MuSigMediationResult> optionalMediationResult = tradeDispute.getMuSigMediationResult();
        if (optionalMediationResult.isEmpty() || tradeDispute.getMediationResultSignature().isEmpty()) {
            ignoreMessage(message, "the signed mediation result is unavailable");
            return;
        }
        if (tradeDispute.getDisputeState() != MuSigDisputeState.MEDIATION_CLOSED) {
            ignoreMessage(message, "mediation is not closed");
            return;
        }

        MuSigMediationResult mediationResult = optionalMediationResult.orElseThrow();
        if (mediationResult.getMediationPayoutDistributionType() == MediationPayoutDistributionType.NO_PAYOUT) {
            ignoreMessage(message, "the mediation result has no payout");
            return;
        }
        if (!Arrays.equals(MuSigMediationResultService.getMediationResultHash(mediationResult),
                message.getMediationResultHash())) {
            ignoreMessage(message, "the mediation result hash does not match");
            return;
        }
        if (trade.getPeer().isMediationResultRejected()) {
            ignoreMessage(message, "the peer already rejected the mediation result");
        }
    }

    @Override
    protected void process(MuSigCustomPayoutPsbtMessage message) {
        if (!shouldProcess) {
            return;
        }

        PeerCustomPayoutPsbt candidate = new PeerCustomPayoutPsbt(message.getTxId(), message.getPsbt());
        Optional<MuSigCustomPayoutPartyData> existingPeerData = trade.getPeer().getCustomPayoutData();
        if (existingPeerData.isPresent()) {
            if (existingPeerData.orElseThrow().getPeersCustomPayoutPsbt()
                    .filter(candidate::equals)
                    .isEmpty()) {
                ignoreMessage(message, "peer custom-payout data conflicts with the first accepted value");
            } else {
                shouldProcess = false;
            }
            return;
        }

        Optional<String> localTxId = trade.getMyself().getCustomPayoutData()
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .map(customPayoutPsbt -> customPayoutPsbt.getTxId());
        if (localTxId.isPresent() && !localTxId.orElseThrow().equals(message.getTxId())) {
            ignoreMessage(message, "the transaction ID does not match the local custom payout");
            return;
        }

        peersCustomPayoutPsbt = candidate;
    }

    @Override
    protected void commit() {
        if (peersCustomPayoutPsbt != null) {
            trade.getPeer().setPeersCustomPayoutPsbt(peersCustomPayoutPsbt);
        }
    }

    @Override
    protected void sendLogMessage() {
    }

    private void ignoreMessage(MuSigCustomPayoutPsbtMessage message, String reason) {
        shouldProcess = false;
        log.warn("Ignoring MuSigCustomPayoutPsbtMessage {} for trade {} because {}.",
                message.getId(), message.getTradeId(), reason);
    }
}
