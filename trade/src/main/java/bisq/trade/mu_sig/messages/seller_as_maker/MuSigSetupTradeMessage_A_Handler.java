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
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.grpc.*;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_A;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_B;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;

@Slf4j
public class MuSigSetupTradeMessage_A_Handler extends TradeMessageHandler<MuSigTrade, MuSigSetupTradeMessage_A>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigSetupTradeMessage_A_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigSetupTradeMessage_A message = (MuSigSetupTradeMessage_A) event;
        verifyMessage(message);

         MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();
        PubKeySharesResponse sellerPubKeyShareResponse = stub.initTrade(PubKeySharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyRole(Role.SELLER_AS_MAKER)
                .build());

        PubKeySharesResponse buyerPubKeySharesResponse = message.getPubKeySharesResponse();
        NonceSharesMessage sellerNonceSharesMessage = stub.getNonceShares(NonceSharesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerOutputPeersPubKeyShare(buyerPubKeySharesResponse.getBuyerOutputPubKeyShare())
                .setSellerOutputPeersPubKeyShare(buyerPubKeySharesResponse.getSellerOutputPubKeyShare())
                .setDepositTxFeeRate(50_000)  // 12.5 sats per vbyte
                .setPreparedTxFeeRate(40_000) // 10.0 sats per vbyte
                .setTradeAmount(200_000)
                .setBuyersSecurityDeposit(30_000)
                .setSellersSecurityDeposit(30_000)
                .build());

        BisqMuSigContract contract = message.getContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            ContractSignatureData makersContractSignatureData = contractService.signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(takersContractSignatureData,
                    makersContractSignatureData,
                    buyerPubKeySharesResponse,
                    sellerPubKeyShareResponse,
                    sellerNonceSharesMessage);

            MuSigSetupTradeMessage_B response = new MuSigSetupTradeMessage_B(StringUtils.createUid(),
                    trade.getId(),
                    trade.getProtocolVersion(),
                    trade.getMyIdentity().getNetworkId(),
                    trade.getPeer().getNetworkId(),
                    contract,
                    makersContractSignatureData,
                    sellerPubKeyShareResponse);
            sendMessage(response, serviceProvider, trade);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(MuSigSetupTradeMessage_A message) {
        super.verifyMessage(message);
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData,
                               ContractSignatureData makersContractSignatureData,
                               PubKeySharesResponse buyerPubKeyShareResponse,
                               PubKeySharesResponse sellerPubKeyShareResponse,
                               NonceSharesMessage sellerNonceShareMessage) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        buyerAsTaker.getContractSignatureData().set(takersContractSignatureData);
        sellerAsMaker.getContractSignatureData().set(makersContractSignatureData);

        buyerAsTaker.setPubKeySharesResponse(buyerPubKeyShareResponse);
        sellerAsMaker.setPubKeySharesResponse(sellerPubKeyShareResponse);
        sellerAsMaker.setNonceSharesMessage(sellerNonceShareMessage);
    }
}
