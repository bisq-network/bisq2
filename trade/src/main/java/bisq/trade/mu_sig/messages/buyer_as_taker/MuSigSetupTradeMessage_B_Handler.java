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

package bisq.trade.mu_sig.messages.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.grpc.*;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_C;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.List;

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

        // Request NonceSharesMessage from rust server
        PubKeySharesResponse sellerPubKeySharesResponse = message.getPubKeySharesResponse();
        NonceSharesRequest nonceSharesRequest = new NonceSharesRequest(trade.getId(),
                sellerPubKeySharesResponse.getBuyerOutputPubKeyShare(),
                sellerPubKeySharesResponse.getSellerOutputPubKeyShare(),
                50_000,// setDepositTxFeeRate 12.5 sats per vbyte
                40_000,// setPreparedTxFeeRate 10.0 sats per vbyte
                200_000, //setTradeAmount
                30_000, //setBuyersSecurityDeposit
                30_000 //setSellersSecurityDeposit
        );
        GrpcStubMock stub = new GrpcStubMock();
        NonceSharesMessage buyerNonceSharesMessage = stub.getNonceShares(nonceSharesRequest);

        MuSigTradeParty sellerAsMaker = trade.getMaker();

        PartialSignaturesRequest partialSignaturesRequest = new PartialSignaturesRequest(trade.getId(),
                sellerAsMaker.getNonceSharesMessage(),
                mockReceivers());
        PartialSignaturesMessage buyerPartialSignaturesMessage = stub.getPartialSignatures(partialSignaturesRequest);


        BisqMuSigContract contract = message.getContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            ContractSignatureData makersContractSignatureData = contractService.signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(takersContractSignatureData,
                    makersContractSignatureData,
                    buyerNonceSharesMessage,
                    buyerPartialSignaturesMessage,
                    sellerPubKeySharesResponse
            );

            MuSigSetupTradeMessage_C response = new MuSigSetupTradeMessage_C(StringUtils.createUid(),
                    trade.getId(),
                    trade.getProtocolVersion(),
                    trade.getMyself().getNetworkId(),
                    trade.getPeer().getNetworkId(),
                    buyerPartialSignaturesMessage);
            sendMessage(response, serviceProvider, trade);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(MuSigSetupTradeMessage_B message) {
        super.verifyMessage(message);
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData,
                               ContractSignatureData makersContractSignatureData,
                               NonceSharesMessage buyerNonceSharesMessage,
                               PartialSignaturesMessage buyerPartialSignaturesMessage,
                               PubKeySharesResponse sellerPubKeySharesResponse) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        buyerAsTaker.getContractSignatureData().set(takersContractSignatureData);
        sellerAsMaker.getContractSignatureData().set(makersContractSignatureData);

        buyerAsTaker.setNonceSharesMessage(buyerNonceSharesMessage);
        buyerAsTaker.setPartialSignaturesMessage(buyerPartialSignaturesMessage);
        sellerAsMaker.setPubKeySharesResponse(sellerPubKeySharesResponse);
    }

    private List<ReceiverAddressAndAmount> mockReceivers() {
        return List.of(
                new ReceiverAddressAndAmount("tb1pwxlp4v9v7v03nx0e7vunlc87d4936wnyqegw0fuahudypan64wys5stxh7", 200_000),
                new ReceiverAddressAndAmount("tb1qpg889v22f3gefuvwpe3963t5a00nvfmkhlgqw5", 80_000),
                new ReceiverAddressAndAmount("2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 12_345)
        );
    }
}
