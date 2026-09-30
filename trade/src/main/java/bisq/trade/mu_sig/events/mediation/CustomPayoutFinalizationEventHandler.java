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

package bisq.trade.mu_sig.events.mediation;

import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigCustomPayoutPartyData;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.PeerCustomPayoutPsbt;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandler;
import bisq.trade.mu_sig.messages.grpc.CustomCloseTradeRequest;
import bisq.trade.mu_sig.messages.grpc.CustomCloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public final class CustomPayoutFinalizationEventHandler
        extends MuSigTradeEventHandler<MuSigTrade, CustomPayoutFinalizationEvent> {
    private CustomCloseTradeResponse customCloseTradeResponse;

    public CustomPayoutFinalizationEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verifyIntegrity(CustomPayoutFinalizationEvent event) {
        checkState(trade.getTradeDispute().getDisputeState() == MuSigDisputeState.MEDIATION_CLOSED,
                "Cannot finalize custom payout before mediation is closed");
        MuSigMediationResult mediationResult = trade.getTradeDispute().getMuSigMediationResult()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot finalize custom payout without a mediation result"));
        checkState(trade.getTradeDispute().getMediationResultSignature().isPresent(),
                "Cannot finalize custom payout without a signed mediation result");
        checkState(mediationResult.getMediationPayoutDistributionType() != MediationPayoutDistributionType.NO_PAYOUT,
                "Cannot finalize a mediation result without a payout");
        checkState(!trade.getMyself().isMediationResultRejected(),
                "Cannot finalize custom payout after rejecting the mediation result");
        checkState(!trade.getPeer().isMediationResultRejected(),
                "Cannot finalize custom payout after the peer rejected the mediation result");
        checkState(trade.getMyself().getCustomPayoutData()
                        .flatMap(MuSigCustomPayoutPartyData::getMyCustomCloseTradeResponse).isEmpty(),
                "Cannot finalize custom payout when a custom close response is already stored");

        CustomPayoutPsbt myCustomPayoutPsbt = trade.getMyself().getCustomPayoutData()
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .orElseThrow(() -> new IllegalStateException("Cannot finalize custom payout without a local PSBT"));
        PeerCustomPayoutPsbt peersCustomPayoutPsbt = trade.getPeer().getCustomPayoutData()
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .orElseThrow(() -> new IllegalStateException("Cannot finalize custom payout without the peer PSBT"));
        checkState(myCustomPayoutPsbt.getTxId().equals(peersCustomPayoutPsbt.getTxId()),
                "Cannot finalize custom payout with mismatching transaction IDs");
    }

    @Override
    protected void process(CustomPayoutFinalizationEvent event) {
        PeerCustomPayoutPsbt peersCustomPayoutPsbt = trade.getPeer().getCustomPayoutData()
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .orElseThrow();
        CustomCloseTradeRequest request = new CustomCloseTradeRequest(
                trade.getId(),
                peersCustomPayoutPsbt.getPsbt());
        customCloseTradeResponse = CustomCloseTradeResponse.fromProto(
                blockingStub.customCloseTrade(request.toProto(false)));
        checkArgument(customCloseTradeResponse.getCustomPayoutTx().length > 0,
                "Custom payout transaction must not be empty");

        tradeService.stopCloseTradeTimeout(trade);
    }

    @Override
    protected void commit() {
        trade.getMyself().setMyCustomCloseTradeResponse(customCloseTradeResponse);
    }

    @Override
    protected void sendLogMessage() {
    }
}
