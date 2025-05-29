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
import bisq.trade.mu_sig.messages.network.handler.NonceSharesRequestUtil;
import bisq.trade.mu_sig.messages.network.handler.PartialSignaturesRequestUtil;
import bisq.trade.mu_sig.messages.network.mu_sig_data.NonceShares;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import bisq.trade.protobuf.NonceSharesRequest;
import bisq.trade.protobuf.PartialSignaturesRequest;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MuSigSetupTradeMessage_B_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigSetupTradeMessage_B> {
    protected ContractSignatureData peersContractSignatureData;
    protected NonceSharesMessage myNonceSharesMessage;
    protected PubKeyShares peersPubKeyShares;
    protected PartialSignaturesMessage myPartialSignaturesMessage;
    protected NonceShares peersNonceShares;

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

        NonceSharesRequest nonceSharesRequest = NonceSharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getBuyerOutputPubKeyShare()))
                .setSellerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getSellerOutputPubKeyShare()))
                .setDepositTxFeeRate(NonceSharesRequestUtil.getDepositTxFeeRate())
                .setPreparedTxFeeRate(NonceSharesRequestUtil.getPreparedTxFeeRate())
                .setTradeAmount(NonceSharesRequestUtil.getTradeAmount(trade))
                .setBuyersSecurityDeposit(NonceSharesRequestUtil.getBuyerSecurityDeposit())
                .setSellersSecurityDeposit(NonceSharesRequestUtil.getSellersSecurityDeposit())
                .build();
        myNonceSharesMessage = NonceSharesMessage.fromProto(blockingStub.getNonceShares(nonceSharesRequest));

        NonceSharesMessage peersNonceSharesMessage = NonceSharesMessage.from(peersNonceShares);
        PartialSignaturesRequest partialSignaturesRequest = PartialSignaturesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersNonceShares(peersNonceSharesMessage.toProto(true))
                .addAllReceivers(PartialSignaturesRequestUtil.getBurningMenDPTReceivers())
                .build();
        myPartialSignaturesMessage = PartialSignaturesMessage.fromProto(blockingStub.getPartialSignatures(partialSignaturesRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        mySelf.setMyNonceSharesMessage(myNonceSharesMessage);
        mySelf.setMyPartialSignaturesMessage(myPartialSignaturesMessage);

        peer.getContractSignatureData().set(peersContractSignatureData);
        peer.setPeersPubKeyShares(peersPubKeyShares);
        peer.setPeersNonceShares(peersNonceShares);
    }

    @Override
    protected void sendMessage() {
        NonceShares nonceShares = NonceShares.from(myNonceSharesMessage);
        send(new MuSigSetupTradeMessage_C(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                nonceShares,
                getPartialSignatures()));
    }

    protected abstract PartialSignatures getPartialSignatures();

    @Override
    protected void sendLogMessage() {
        String role = trade.isBuyer() ? "Taker (as buyer)" : "Taker (as seller)";
        String peerRole = trade.isBuyer() ? "seller" : "buyer";
        sendLogMessage(role + " received peers pubKeyShares and nonceShares.\n" +
                role + " created his nonceShares and partialSignatures.\n" +
                role + " sent his nonceShares and his partialSignatures to " + peerRole + ".");
    }
}
