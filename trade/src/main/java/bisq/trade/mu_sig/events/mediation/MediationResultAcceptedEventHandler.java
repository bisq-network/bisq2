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

package bisq.trade.mu_sig.events.mediation;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigCustomPayoutPartyData;
import bisq.trade.mu_sig.MuSigCustomPayoutValidation;
import bisq.trade.mu_sig.MuSigFeeRateProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.PeerCustomPayoutPsbt;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbtRequest;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public final class MediationResultAcceptedEventHandler
        extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, MediationResultAcceptedEvent> {
    private CustomPayoutPsbt customPayoutPsbt;

    public MediationResultAcceptedEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verifyIntegrity(MediationResultAcceptedEvent event) {
        checkState(!trade.getMyself().isMediationResultRejected(),
                "Cannot sign custom payout after rejecting the mediation result");
        checkState(trade.getMyself().getCustomPayoutData().isEmpty(),
                "Cannot sign custom payout when local custom-payout data already exists");
    }

    @Override
    protected void process(MediationResultAcceptedEvent event) {
        MuSigMediationResult mediationResult = trade.getTradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        long sellersPayoutAmountExcludingFee = mediationResult.getProposedSellerPayoutAmount().orElseThrow();
        CustomPayoutPsbtRequest request = new CustomPayoutPsbtRequest(
                trade.getId(),
                sellersPayoutAmountExcludingFee,
                MuSigFeeRateProvider.getPreparedTxFeeRate());
        customPayoutPsbt = CustomPayoutPsbt.fromProto(
                blockingStub.signCustomPayoutTx(request.toProto(false)));
        verifySigningResponse(customPayoutPsbt, mediationResult);
    }

    @Override
    protected void commit() {
        trade.getMyself().setMyCustomPayoutPsbt(customPayoutPsbt);
    }

    @Override
    protected void sendMessage() {
        MuSigMediationResult mediationResult = trade.getTradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        trade.getPeer().getCustomPayoutData()
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .map(PeerCustomPayoutPsbt::getTxId)
                .filter(peerTxId -> !peerTxId.equals(customPayoutPsbt.getTxId()))
                .ifPresent(peerTxId -> log.warn(
                        "Sending custom payout PSBT for trade {} despite mismatching peer transaction ID {}.",
                        trade.getId(), peerTxId));
        send(new MuSigCustomPayoutPsbtMessage(
                StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult),
                customPayoutPsbt.getTxId(),
                customPayoutPsbt.getPsbt()));
    }

    @Override
    protected void sendLogMessage() {
        MuSigOpenTradeChannelService openTradeChannelService =
                serviceProvider.getChatService().getMuSigOpenTradeChannelService();
        openTradeChannelService.findChannelByTradeId(trade.getId())
                .ifPresent(channel -> {
                    String encoded = Res.encode("muSig.mediation.result.accepted.tradeLogMessage",
                            channel.getMyUserIdentity().getUserName());
                    openTradeChannelService.sendTradeLogMessage(encoded, channel);
                });
    }

    private void verifySigningResponse(CustomPayoutPsbt customPayoutPsbt,
                                       MuSigMediationResult mediationResult) {
        MuSigCustomPayoutValidation.validatePsbt(customPayoutPsbt.getPsbt());

        long proposedBuyerPayoutAmount = mediationResult.getProposedBuyerPayoutAmount().orElseThrow();
        long proposedSellerPayoutAmount = mediationResult.getProposedSellerPayoutAmount().orElseThrow();
        long buyersPayoutAmountIncludingFee = customPayoutPsbt.getBuyersPayoutAmountIncludingFee();
        long sellersPayoutAmountIncludingFee = customPayoutPsbt.getSellersPayoutAmountIncludingFee();
        // TODO: add checks as soon as deposit is used the right way
//        checkArgument(buyersPayoutAmountIncludingFee <= proposedBuyerPayoutAmount,
//                "Returned buyer payout must not exceed the proposed buyer payout");
//        checkArgument(sellersPayoutAmountIncludingFee <= proposedSellerPayoutAmount,
//                "Returned seller payout must not exceed the proposed seller payout");
    }
}
