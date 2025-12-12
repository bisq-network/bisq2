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

package bisq.trade.mu_sig.messages.network.handler.maker;

import bisq.common.encoding.Hex;
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
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.handler.NonceSharesRequestUtil;
import bisq.trade.mu_sig.messages.network.mu_sig_data.NonceShares;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import bisq.trade.mu_sig.protocol.MuSigProtocolException;
import bisq.trade.protobuf.NonceSharesRequest;
import bisq.trade.protobuf.PubKeySharesRequest;
import bisq.trade.protobuf.Role;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class SetupTradeMessage_A_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, SetupTradeMessage_A> {
    private PubKeySharesResponse myPubKeySharesResponse;
    private PubKeyShares peersPubKeyShares;
    private NonceSharesMessage myNonceSharesMessage;
    private ContractSignatureData peersContractSignatureData;
    private ContractSignatureData myContractSignatureData;

    public SetupTradeMessage_A_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(SetupTradeMessage_A message) {
        MuSigContract peersContract = message.getContract();
        peersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        MuSigContract myContract = trade.getContract();
        checkArgument(peersContract.equals(myContract),
                "Peer's contract is not the same as my contract.\n" +
                        "peersContract=" + peersContract + "\n" +
                        "myContract=" + myContract);
        try {
            myContractSignatureData = contractService.signContract(myContract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
        } catch (GeneralSecurityException e) {
            log.error("Signing contract failed", e);
            throw new MuSigProtocolException(e);
        }
        checkArgument(Arrays.equals(peersContractSignatureData.getContractHash(), myContractSignatureData.getContractHash()),
                "Peer's contractHash at contract signature data is not the same as the contractHash at my contract signature data.\n" +
                        "peersContractSignatureData.contractHash=" + Hex.encode(peersContractSignatureData.getContractHash()) + "\n" +
                        "myContractSignatureData.contractHash=" + Hex.encode(myContractSignatureData.getContractHash()));
    }

    @Override
    protected void process(SetupTradeMessage_A message) {
        peersPubKeyShares = message.getPubKeyShares();

        Role role = trade.isBuyer() ? Role.BUYER_AS_MAKER : Role.SELLER_AS_MAKER;
        PubKeySharesRequest pubKeySharesRequest = PubKeySharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyRole(role)
                .build();
        myPubKeySharesResponse = PubKeySharesResponse.fromProto(blockingStub.initTrade(pubKeySharesRequest));

        NonceSharesRequest nonceSharesRequest = NonceSharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getBuyerOutputPubKeyShare()))
                .setSellerOutputPeersPubKeyShare(ByteString.copyFrom(peersPubKeyShares.getSellerOutputPubKeyShare()))
                .setDepositTxFeeRate(NonceSharesRequestUtil.getDepositTxFeeRate())
                .setPreparedTxFeeRate(NonceSharesRequestUtil.getPreparedTxFeeRate())
                .setTradeAmount(NonceSharesRequestUtil.getTradeAmount(trade))
                .setBuyersSecurityDeposit(NonceSharesRequestUtil.getBuyerSecurityDeposit())
                .setSellersSecurityDeposit(NonceSharesRequestUtil.getSellersSecurityDeposit())
                .setTradeFeeReceiver(NonceSharesRequestUtil.getTradeFeeReceiver())
                .build();
        myNonceSharesMessage = NonceSharesMessage.fromProto(blockingStub.getNonceShares(nonceSharesRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        peer.getContractSignatureData().set(peersContractSignatureData);
        mySelf.getContractSignatureData().set(myContractSignatureData);

        peer.setPeersPubKeyShares(peersPubKeyShares);
        mySelf.setMyPubKeySharesResponse(myPubKeySharesResponse);
        mySelf.setMyNonceSharesMessage(myNonceSharesMessage);
    }

    @Override
    protected void sendMessage() {
        PubKeyShares pubKeyShares = PubKeyShares.from(myPubKeySharesResponse);
        NonceShares nonceShares = NonceShares.from(myNonceSharesMessage);

        send(new SetupTradeMessage_B(StringUtils.createUid(),
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
        String role = trade.isBuyer() ? "Maker (as buyer)" : "Maker (as seller)";
        sendLogMessage(role + " received peers pubKeyShares.\n" +
                role + " created his pubKeyShares and nonceShares.\n" +
                role + " sent his pubKeyShares and his nonceShares to taker.");
    }
}
