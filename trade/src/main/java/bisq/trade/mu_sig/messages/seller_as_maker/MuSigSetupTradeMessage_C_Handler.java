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

package bisq.trade.mu_sig.messages.seller_as_maker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.grpc.*;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_C;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_D;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MuSigSetupTradeMessage_C_Handler extends TradeMessageHandler<MuSigTrade, MuSigSetupTradeMessage_C>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigSetupTradeMessage_C_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigSetupTradeMessage_C message = (MuSigSetupTradeMessage_C) event;
        verifyMessage(message);

        MuSigTradeParty buyerAsTake = trade.getTaker();
        PartialSignaturesMessage buyerPartialSignaturesMessage = message.getPartialSignaturesMessage();

        PartialSignaturesRequest partialSignaturesRequest = new PartialSignaturesRequest(trade.getId(),
                buyerAsTake.getNonceSharesMessage(),
                mockReceivers());
        GrpcStubMock stub = new GrpcStubMock();
        PartialSignaturesMessage sellerPartialSignaturesMessage = stub.getPartialSignatures(partialSignaturesRequest);

        PartialSignaturesMessage redeactedBuyerPartialSignaturesMessage = clearSwapTxInputPartialSignature(buyerPartialSignaturesMessage);
        DepositTxSignatureRequest depositTxSignatureRequest = new DepositTxSignatureRequest(
                trade.getId(),
                redeactedBuyerPartialSignaturesMessage);
        DepositPsbt sellerDepositPsbt = stub.signDepositTx(depositTxSignatureRequest);

        commitToModel(sellerPartialSignaturesMessage, sellerDepositPsbt, buyerPartialSignaturesMessage);

         MuSigSetupTradeMessage_D response = new MuSigSetupTradeMessage_D(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                sellerPartialSignaturesMessage);
        sendMessage(response, serviceProvider, trade);
    }

    @Override
    protected void verifyMessage(MuSigSetupTradeMessage_C message) {
        super.verifyMessage(message);
    }

    private void commitToModel(PartialSignaturesMessage sellerPartialSignaturesMessage,
                               DepositPsbt sellerDepositPsbt,
                               PartialSignaturesMessage buyerPartialSignaturesMessage) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        sellerAsMaker.setPartialSignaturesMessage(sellerPartialSignaturesMessage);
        sellerAsMaker.setDepositPsbt(sellerDepositPsbt);

        buyerAsTaker.setPartialSignaturesMessage(buyerPartialSignaturesMessage);
    }

    private PartialSignaturesMessage clearSwapTxInputPartialSignature(PartialSignaturesMessage partialSignaturesMessage) {
        // REDACT buyer's swapTxInputPartialSignature:
        return new PartialSignaturesMessage(
                partialSignaturesMessage.getPeersWarningTxBuyerInputPartialSignature(),
                partialSignaturesMessage.getPeersWarningTxSellerInputPartialSignature(),
                partialSignaturesMessage.getPeersRedirectTxInputPartialSignature(),
                Optional.empty()
        );
    }

    private List<ReceiverAddressAndAmount> mockReceivers() {
        return List.of(
                new ReceiverAddressAndAmount("tb1pwxlp4v9v7v03nx0e7vunlc87d4936wnyqegw0fuahudypan64wys5stxh7", 200_000),
                new ReceiverAddressAndAmount("tb1qpg889v22f3gefuvwpe3963t5a00nvfmkhlgqw5", 80_000),
                new ReceiverAddressAndAmount("2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 12_345)
        );
    }
}
