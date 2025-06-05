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

package bisq.trade.mu_sig.messages.network.handler.taker;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.protobuf.DepositTxSignatureRequest;
import bisq.trade.protobuf.PublishDepositTxRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseMuSigSetupTradeMessage_D_Handler extends MuSigTradeMessageHandler<MuSigTrade, MuSigSetupTradeMessage_D> {
    protected PartialSignatures peersPartialSignatures;
    protected DepositPsbt myDepositPsbt;

    public BaseMuSigSetupTradeMessage_D_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigSetupTradeMessage_D message) {
    }

    @Override
    protected void process(MuSigSetupTradeMessage_D message) {
        peersPartialSignatures = message.getPartialSignatures();

        PartialSignaturesMessage peersPartialSignaturesMessage = PartialSignaturesMessage.from(peersPartialSignatures);
        DepositTxSignatureRequest depositTxSignatureRequest = DepositTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersPartialSignatures(peersPartialSignaturesMessage.toProto(true))
                .build();
        myDepositPsbt = DepositPsbt.fromProto(blockingStub.signDepositTx(depositTxSignatureRequest));

        tradeService.observeDepositTxConfirmationStatus(trade);

        PublishDepositTxRequest publishDepositTxRequest = PublishDepositTxRequest.newBuilder()
                .setTradeId(trade.getId())
                .setDepositPsbt(myDepositPsbt.toProto(true))
                .build();
        blockingStub.publishDepositTx(publishDepositTxRequest);
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        mySelf.setMyDepositPsbt(myDepositPsbt);
        peer.setPeersPartialSignatures(peersPartialSignatures);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Taker received peers partialSignatures.\n" +
                "Taker created depositPsbt.\n" +
                "Taker published deposit tx and start listening for blockchain confirmation.");
    }
}
