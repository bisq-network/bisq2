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

package bisq.trade.mu_sig.messages.p2p.handler.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.grpc.*;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.p2p.MuSigSetupTradeMessage_D;
import bisq.trade.protocol.events.TradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class MuSigSetupTradeMessage_D_Handler extends TradeMessageHandler<MuSigTrade, MuSigSetupTradeMessage_D> {

    public MuSigSetupTradeMessage_D_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigSetupTradeMessage_D message = (MuSigSetupTradeMessage_D) event;
        verifyMessage(message);

        PartialSignaturesMessage sellerPartialSignaturesMessage = message.getPartialSignaturesMessage();
        MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();
        DepositPsbt buyerDepositPsbt = DepositPsbt.fromProto(stub.signDepositTx(DepositTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersPartialSignatures(sellerPartialSignaturesMessage.toProto(true))
                .build()));

        // *** BUYER BROADCASTS DEPOSIT TX ***
        Iterator<TxConfirmationStatus> depositTxConfirmationIter = stub.publishDepositTx(PublishDepositTxRequest.newBuilder()
                .setTradeId(trade.getId())
                .setDepositPsbt(buyerDepositPsbt.toProto(true))
                .build());

        commitToModel(buyerDepositPsbt, sellerPartialSignaturesMessage);
    }

    @Override
    protected void verifyMessage(MuSigSetupTradeMessage_D message) {
        super.verifyMessage(message);
    }

    private void commitToModel(DepositPsbt buyerDepositPsbt,
                               PartialSignaturesMessage sellerPartialSignaturesMessage) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        buyerAsTaker.setDepositPsbt(buyerDepositPsbt);
        sellerAsMaker.setPartialSignaturesMessage(sellerPartialSignaturesMessage);
    }
}
