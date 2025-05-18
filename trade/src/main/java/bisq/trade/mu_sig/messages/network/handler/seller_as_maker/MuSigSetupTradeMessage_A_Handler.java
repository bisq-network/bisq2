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
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PubKeySharesResponse;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.vo.NonceShares;
import bisq.trade.mu_sig.messages.network.vo.PubKeyShares;
import bisq.trade.protobuf.NonceSharesRequest;
import bisq.trade.protobuf.PubKeySharesRequest;
import bisq.trade.protobuf.Role;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;

@Slf4j
public final class MuSigSetupTradeMessage_A_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigSetupTradeMessage_A> {
    private PubKeySharesResponse myPubKeySharesResponse;
    private PubKeyShares peersPubKeyShares;
    private NonceSharesMessage myNonceShares;
    private ContractSignatureData takersContractSignatureData;
    private ContractSignatureData myContractSignatureData;

    public MuSigSetupTradeMessage_A_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigSetupTradeMessage_A message) {
        MuSigContract takersContract = message.getContract();
        takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            MuSigContract makersContract = trade.getContract();
            myContractSignatureData = contractService.signContract(makersContract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());

            // TODO verify both contracts are the same, and verify peers signature
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void process(MuSigSetupTradeMessage_A message) {
        peersPubKeyShares = message.getPubKeyShares();

        PubKeySharesRequest pubKeySharesRequest = PubKeySharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyRole(Role.SELLER_AS_MAKER)
                .build();
        myPubKeySharesResponse = PubKeySharesResponse.fromProto(musigBlockingStub.initTrade(pubKeySharesRequest));

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
    }

    @Override
    protected void commit() {
        MuSigTradeParty peer = trade.getTaker();
        MuSigTradeParty mySelf = trade.getMaker();

        peer.getContractSignatureData().set(takersContractSignatureData);
        mySelf.getContractSignatureData().set(myContractSignatureData);

        peer.setPeersPubKeySharesResponse(peersPubKeyShares);
        mySelf.setMyPubKeySharesResponse(myPubKeySharesResponse);
        mySelf.setMyNonceSharesMessage(myNonceShares);
    }

    @Override
    protected void sendMessage() {
        PubKeyShares pubKeyShares = new PubKeyShares(myPubKeySharesResponse.getBuyerOutputPubKeyShare().clone(),
                myPubKeySharesResponse.getSellerOutputPubKeyShare().clone());

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

        send(new MuSigSetupTradeMessage_B(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                trade.getContract(),
                myContractSignatureData,
                pubKeyShares,
                nonceShares));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller received peers pubKeyShares.\n" +
                "Seller created his pubKeyShares and nonceShares.\n " +
                "Seller sent his pubKeyShares and his nonceShares to buyer.");
    }
}
