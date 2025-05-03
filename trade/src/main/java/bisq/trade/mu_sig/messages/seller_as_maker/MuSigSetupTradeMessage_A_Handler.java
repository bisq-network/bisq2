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

        // Request PubKeyShares from rust server
        PubKeySharesRequest pubKeySharesRequest = new PubKeySharesRequest(trade.getId(), trade.getTradeRole());
        GrpcStubMock stub = new GrpcStubMock();
        PubKeySharesResponse sellerPubKeyShareResponse = stub.initTrade(pubKeySharesRequest);

        // Request NonceSharesMessage from rust server
        PubKeySharesResponse buyerPubKeyShareResponse = message.getPubKeySharesResponse();
        NonceSharesRequest sellerNonceSharesRequest = new NonceSharesRequest(trade.getId(),
                buyerPubKeyShareResponse.getBuyerOutputPubKeyShare(),
                buyerPubKeyShareResponse.getSellerOutputPubKeyShare(),
                50_000,// setDepositTxFeeRate 12.5 sats per vbyte
                40_000,// setPreparedTxFeeRate 10.0 sats per vbyte
                200_000, //setTradeAmount
                30_000, //setBuyersSecurityDeposit
                30_000 //setSellersSecurityDeposit
        );
        NonceSharesMessage sellerNonceShareMessage = stub.getNonceShares(sellerNonceSharesRequest);

        BisqMuSigContract contract = message.getContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            ContractSignatureData makersContractSignatureData = contractService.signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(takersContractSignatureData,
                    makersContractSignatureData,
                    buyerPubKeyShareResponse,
                    sellerPubKeyShareResponse,
                    sellerNonceShareMessage);

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
