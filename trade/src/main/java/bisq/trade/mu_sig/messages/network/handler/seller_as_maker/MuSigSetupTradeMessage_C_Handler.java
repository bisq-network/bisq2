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

package bisq.trade.mu_sig.messages.network.handler.seller_as_maker;

import bisq.common.util.StringUtils;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_C;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_D;
import bisq.trade.protobuf.DepositTxSignatureRequest;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.PartialSignaturesRequest;
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class MuSigSetupTradeMessage_C_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigSetupTradeMessage_C> {
    private NonceSharesMessage peersNonceShares;
    private PartialSignaturesMessage myPartialSignatures;
    private PartialSignaturesMessage peersPartialSignatures;
    private DepositPsbt myDepositPsbt;

    public MuSigSetupTradeMessage_C_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigSetupTradeMessage_C message) {
    }

    @Override
    protected void process(MuSigSetupTradeMessage_C message) {
        peersNonceShares = message.getNonceSharesMessage();
        peersPartialSignatures = message.getPartialSignaturesMessage();

        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        PartialSignaturesRequest partialSignaturesRequest = PartialSignaturesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersNonceShares(peersNonceShares.toProto(true))
                .addAllReceivers(mockReceivers())
                .build();
        myPartialSignatures = PartialSignaturesMessage.fromProto(musigBlockingStub.getPartialSignatures(partialSignaturesRequest));

        DepositTxSignatureRequest depositTxSignatureRequest = DepositTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                // REDACT buyer's swapTxInputPartialSignature:
                .setPeersPartialSignatures(peersPartialSignatures.toProto(true).toBuilder().clearSwapTxInputPartialSignature())
                .build();
        myDepositPsbt = DepositPsbt.fromProto(musigBlockingStub.signDepositTx(depositTxSignatureRequest));

        // We observe the txConfirmationStatus to get informed once the deposit tx is confirmed (gets published by the
        // buyer when they receive the MuSigSetupTradeMessage_D).
        muSigTradeService.observeDepositTxConfirmationStatus(trade);

        // Maybe remove makers offer
        if (serviceProvider.getSettingsService().getCloseMyOfferWhenTaken().get()) {
            MuSigOffer offer = trade.getContract().getOffer();
            serviceProvider.getOfferService().getMuSigOfferService().removeOffer(offer)
                    .whenComplete((deleteChatMessageResult, throwable) -> {
                        if (throwable == null) {
                            log.info("Offer with ID {} removed", offer.getId());
                        } else {
                            log.error("We got an error when removing offer with ID {}", offer.getId(), throwable);
                        }
                    });
        }
    }

    @Override
    protected void commit() {
        MuSigTradeParty peer = trade.getTaker();
        MuSigTradeParty mySelf = trade.getMaker();

        mySelf.setPartialSignaturesMessage(myPartialSignatures);
        mySelf.setDepositPsbt(myDepositPsbt);

        peer.setNonceSharesMessage(peersNonceShares);
        peer.setPartialSignaturesMessage(peersPartialSignatures);
    }

    @Override
    protected void sendMessage() {
        send(new MuSigSetupTradeMessage_D(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                myPartialSignatures));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller received peers nonceShares and partialSignatures\n." +
                "Seller created his nonceShares and partialSignatures.\n " +
                "Seller sent his nonceShares and his partialSignatures to buyer.");
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
