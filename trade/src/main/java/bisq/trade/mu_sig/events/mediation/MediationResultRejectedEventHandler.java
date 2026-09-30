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
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public final class MediationResultRejectedEventHandler
        extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, MediationResultRejectedEvent> {
    private boolean shouldProcess;
    private byte[] mediationResultHash;

    public MediationResultRejectedEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void process(MediationResultRejectedEvent event) {
        shouldProcess = canProcessRejection();
        if (!shouldProcess) {
            return;
        }

        MuSigMediationResult mediationResult = trade.getTradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        mediationResultHash = MuSigMediationResultService.getMediationResultHash(mediationResult);
    }

    @Override
    protected void commit() {
        if (!shouldProcess) {
            return;
        }

        trade.getMyself().setMediationResultRejected();
    }

    @Override
    protected void sendMessage() {
        if (!shouldProcess) {
            return;
        }
        send(new MuSigMediationResultRejectionMessage(
                StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                mediationResultHash));
    }

    @Override
    protected void sendLogMessage() {
        if (!shouldProcess) {
            return;
        }
        MuSigOpenTradeChannelService openTradeChannelService =
                serviceProvider.getChatService().getMuSigOpenTradeChannelService();
        openTradeChannelService.findChannelByTradeId(trade.getId())
                .ifPresent(channel -> {
                    String encoded = Res.encode("muSig.mediation.result.rejected.tradeLogMessage",
                            channel.getMyUserIdentity().getUserName());
                    openTradeChannelService.sendTradeLogMessage(encoded, channel);
                });
    }

    private boolean canProcessRejection() {
        if (trade.getTradeDispute().getDisputeState() != MuSigDisputeState.MEDIATION_CLOSED ||
                trade.getTradeDispute().getMediationResultSignature().isEmpty()) {
            log.info("Ignoring MediationResultRejectedEvent for trade {} because a signed mediation result is unavailable.",
                    trade.getId());
            return false;
        }

        Optional<MuSigMediationResult> optionalMediationResult = trade.getTradeDispute()
                .getMuSigMediationResult();
        if (optionalMediationResult.isEmpty()) {
            log.info("Ignoring MediationResultRejectedEvent for trade {} because the mediation result is unavailable.",
                    trade.getId());
            return false;
        }
        MuSigMediationResult mediationResult = optionalMediationResult.orElseThrow();
        if (mediationResult.getMediationPayoutDistributionType() == MediationPayoutDistributionType.NO_PAYOUT) {
            log.info("Ignoring MediationResultRejectedEvent for trade {} because the result has no payout.",
                    trade.getId());
            return false;
        }
        if (trade.getMyself().isMediationResultRejected()) {
            return false;
        }
        if (trade.getMyself().getCustomPayoutData().isPresent()) {
            log.info("Ignoring MediationResultRejectedEvent for trade {} because local custom-payout data already exists.",
                    trade.getId());
            return false;
        }
        return true;
    }
}
