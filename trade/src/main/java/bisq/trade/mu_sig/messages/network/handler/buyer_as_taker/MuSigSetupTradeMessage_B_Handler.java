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

import bisq.common.util.StringUtils;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_C;
import bisq.trade.mu_sig.messages.network.vo.NonceShares;
import bisq.trade.mu_sig.messages.network.vo.PartialSignatures;
import bisq.trade.mu_sig.messages.network.vo.PubKeyShares;
import bisq.trade.protobuf.NonceSharesRequest;
import bisq.trade.protobuf.PartialSignaturesRequest;
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class MuSigSetupTradeMessage_B_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigSetupTradeMessage_B> {
    private ContractSignatureData peersContractSignatureData;
    private NonceSharesMessage myNonceShares;
    private PubKeyShares peersPubKeyShares;
    private PartialSignaturesMessage myPartialSignatures;
    private NonceShares peersNonceShares;

    public MuSigSetupTradeMessage_B_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigSetupTradeMessage_B message) {
        // TODO verify both contracts are the same, and verify peers signature
    }

    @Override
    protected void process(MuSigSetupTradeMessage_B message) {
        peersNonceShares = message.getNonceShares();
        peersPubKeyShares = message.getPubKeyShares();
        MuSigContract peersContract = message.getContract(); //todo needed?
        peersContractSignatureData = message.getContractSignatureData();

        // Request NonceSharesMessage from rust server

        NonceSharesRequest nonceSharesRequest = NonceSharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getBuyerOutputPubKeyShare()))
                .setSellerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getSellerOutputPubKeyShare()))
                .setDepositTxFeeRate(50_000)  // 12.5 sats per vbyte
                .setPreparedTxFeeRate(40_000) // 10.0 sats per vbyte
                .setTradeAmount(200_000)
                .setBuyersSecurityDeposit(30_000)
                .setSellersSecurityDeposit(30_000)
                .build();
        myNonceShares = NonceSharesMessage.fromProto(musigBlockingStub.getNonceShares(nonceSharesRequest));

        // todo maybe adjust grpc api
        NonceSharesMessage peersNonceSharesMessage = new NonceSharesMessage(
                peersNonceShares.getWarningTxFeeBumpAddress(),
                peersNonceShares.getRedirectTxFeeBumpAddress(),
                peersNonceShares.getHalfDepositPsbt(),
                peersNonceShares.getSwapTxInputNonceShare(),
                peersNonceShares.getBuyersWarningTxBuyerInputNonceShare(),
                peersNonceShares.getBuyersWarningTxSellerInputNonceShare(),
                peersNonceShares.getSellersWarningTxBuyerInputNonceShare(),
                peersNonceShares.getSellersWarningTxSellerInputNonceShare(),
                peersNonceShares.getBuyersRedirectTxInputNonceShare(),
                peersNonceShares.getSellersRedirectTxInputNonceShare()
        );

        PartialSignaturesRequest partialSignaturesRequest = PartialSignaturesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersNonceShares(peersNonceSharesMessage.toProto(true))
                .addAllReceivers(mockReceivers())
                .build();
        myPartialSignatures = PartialSignaturesMessage.fromProto(musigBlockingStub.getPartialSignatures(partialSignaturesRequest));


    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getTaker();
        MuSigTradeParty peer = trade.getMaker();

        mySelf.setMyNonceSharesMessage(myNonceShares);
        mySelf.setMyPartialSignaturesMessage(myPartialSignatures);

        peer.getContractSignatureData().set(peersContractSignatureData);
        peer.setPeersPubKeySharesResponse(peersPubKeyShares);
        peer.setPeersNonceShares(peersNonceShares);
    }

    @Override
    protected void sendMessage() {
        NonceShares nonceShares = new NonceShares(
                myNonceShares.getWarningTxFeeBumpAddress(),
                myNonceShares.getRedirectTxFeeBumpAddress(),
                myNonceShares.getHalfDepositPsbt().clone(),
                myNonceShares.getSwapTxInputNonceShare().clone(),
                myNonceShares.getBuyersWarningTxBuyerInputNonceShare().clone(),
                myNonceShares.getBuyersWarningTxSellerInputNonceShare().clone(),
                myNonceShares.getSellersWarningTxBuyerInputNonceShare().clone(),
                myNonceShares.getSellersWarningTxSellerInputNonceShare().clone(),
                myNonceShares.getBuyersRedirectTxInputNonceShare().clone(),
                myNonceShares.getSellersRedirectTxInputNonceShare().clone()
        );

        // TODO redacting swapTxInputPartialSignature fails at MuSigPaymentReceiptConfirmedEventHandler
        PartialSignatures partialSignatures = new PartialSignatures(
                myPartialSignatures.getPeersWarningTxBuyerInputPartialSignature().clone(),
                myPartialSignatures.getPeersWarningTxSellerInputPartialSignature().clone(),
                myPartialSignatures.getPeersRedirectTxInputPartialSignature().clone(),
                myPartialSignatures.getSwapTxInputPartialSignature().clone()
                // new byte[]{}
        );

        send(new MuSigSetupTradeMessage_C(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                nonceShares,
                partialSignatures));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Buyer received peers pubKeyShares.\n" +
                "Buyer created his nonceShares and partialSignatures.\n " +
                "Buyer sent his nonceShares and his partialSignatures to seller.");
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
