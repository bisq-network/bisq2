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

package bisq.trade.mu_sig.messages.network.handler;

import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public final class MuSigMediationResultRejectionMessageHandler
        extends MuSigTradeMessageHandler<MuSigTrade, MuSigMediationResultRejectionMessage> {
    private boolean shouldProcess = true;

    public MuSigMediationResultRejectionMessageHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verifyInternal(MuSigMediationResultRejectionMessage message) {
        // Intentionally do not call super.verifyInternal(): invalid or stale rejection messages
        // must be ignored without failing the trade.
        // A rejection can arrive asynchronously after another settlement path has progressed.
        if (!message.getTradeId().equals(trade.getId()) ||
                !message.getSender().equals(trade.getPeer().getNetworkId()) ||
                !message.getReceiver().equals(trade.getMyself().getNetworkId()) ||
                !Objects.equals(message.getProtocolVersion(), trade.getProtocolVersion())) {
            ignoreMessage(message, "unexpected trade-message context");
        }
    }

    @Override
    protected void verify(MuSigMediationResultRejectionMessage message) {
        if (!shouldProcess) {
            return;
        }

        Optional<MuSigMediationResult> optionalMediationResult =
                trade.getTradeDispute().getMuSigMediationResult();
        if (optionalMediationResult.isEmpty() ||
                trade.getTradeDispute().getMediationResultSignature().isEmpty()) {
            ignoreMessage(message, "the signed mediation result is unavailable");
            return;
        }
        if (trade.getTradeDispute().getDisputeState() != MuSigDisputeState.MEDIATION_CLOSED) {
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
            shouldProcess = false;
            return;
        }
        if (trade.getPeer().getCustomPayoutData().isPresent()) {
            ignoreMessage(message, "peer custom-payout data already exists");
        }
    }

    @Override
    protected void process(MuSigMediationResultRejectionMessage message) {
    }

    @Override
    protected void commit() {
        if (shouldProcess) {
            trade.getPeer().setMediationResultRejected();
        }
    }

    @Override
    protected void sendLogMessage() {
    }

    private void ignoreMessage(MuSigMediationResultRejectionMessage message, String reason) {
        shouldProcess = false;
        log.warn("Ignoring MuSigMediationResultRejectionMessage {} for trade {} because {}.",
                message.getId(), message.getTradeId(), reason);
    }
}
