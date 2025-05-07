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

package bisq.trade.mu_sig.messages.network.handler.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.NonceSharesRequest;
import bisq.trade.protobuf.PartialSignaturesRequest;
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.grpc.PubKeySharesResponse;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_C;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.handler.TradeMessageSender;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MuSigSetupTradeMessage_B_Handler extends TradeMessageHandler<MuSigTrade, MuSigSetupTradeMessage_B>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigSetupTradeMessage_B_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigSetupTradeMessage_B message = (MuSigSetupTradeMessage_B) event;
        verifyMessage(message);

        NonceSharesMessage sellerAsMakerNonceSharesMessage = message.getNonceSharesMessage();

        // Request NonceSharesMessage from rust server
        PubKeySharesResponse sellerPubKeySharesResponse = message.getPubKeySharesResponse();
        MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();
        NonceSharesMessage buyerNonceSharesMessage = NonceSharesMessage.fromProto(stub.getNonceShares(NonceSharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerOutputPeersPubKeyShare(ByteString.copyFrom(sellerPubKeySharesResponse.getBuyerOutputPubKeyShare()))
                .setSellerOutputPeersPubKeyShare(ByteString.copyFrom(sellerPubKeySharesResponse.getSellerOutputPubKeyShare()))
                .setDepositTxFeeRate(50_000)  // 12.5 sats per vbyte
                .setPreparedTxFeeRate(40_000) // 10.0 sats per vbyte
                .setTradeAmount(200_000)
                .setBuyersSecurityDeposit(30_000)
                .setSellersSecurityDeposit(30_000)
                .build()));

        PartialSignaturesMessage buyerPartialSignaturesMessage = PartialSignaturesMessage.fromProto(stub.getPartialSignatures(PartialSignaturesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersNonceShares(sellerAsMakerNonceSharesMessage.toProto(true))
                .addAllReceivers(mockReceivers())
                .build()));

        MuSigContract makersContract = message.getContract();
        ContractSignatureData makersContractSignatureData = message.getContractSignatureData();

        // TODO verify both contracts are the same, and verify peers signature

        commitToModel(makersContractSignatureData,
                buyerNonceSharesMessage,
                buyerPartialSignaturesMessage,
                sellerPubKeySharesResponse,
                sellerAsMakerNonceSharesMessage
        );

        MuSigSetupTradeMessage_C response = new MuSigSetupTradeMessage_C(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                buyerNonceSharesMessage,
                buyerPartialSignaturesMessage); // TODO we probably don't want to send all the data here
        sendMessage(response, serviceProvider, trade);
    }

    @Override
    protected void verifyMessage(MuSigSetupTradeMessage_B message) {
        super.verifyMessage(message);
    }

    private void commitToModel(ContractSignatureData makersContractSignatureData,
                               NonceSharesMessage buyerNonceSharesMessage,
                               PartialSignaturesMessage buyerPartialSignaturesMessage,
                               PubKeySharesResponse sellerPubKeySharesResponse,
                               NonceSharesMessage sellerAsMakerNonceSharesMessage) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        sellerAsMaker.getContractSignatureData().set(makersContractSignatureData);

        buyerAsTaker.setNonceSharesMessage(buyerNonceSharesMessage);
        buyerAsTaker.setPartialSignaturesMessage(buyerPartialSignaturesMessage);
        sellerAsMaker.setPubKeySharesResponse(sellerPubKeySharesResponse);
        sellerAsMaker.setNonceSharesMessage(sellerAsMakerNonceSharesMessage);
    }

    private static List<ReceiverAddressAndAmount> mockReceivers() {
        return ImmutableMap.of(
                        "tb1pwxlp4v9v7v03nx0e7vunlc87d4936wnyqegw0fuahudypan64wys5stxh7", 200_000,
                        "tb1qpg889v22f3gefuvwpe3963t5a00nvfmkhlgqw5", 80_000,
                        "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 12_345
                )
                .entrySet().stream()
                .map(e -> ReceiverAddressAndAmount.newBuilder().setAddress(e.getKey()).setAmount(e.getValue()).build())
                .collect(Collectors.toList());
    }
}
