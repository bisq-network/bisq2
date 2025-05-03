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

package bisq.trade.mu_sig.events.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.grpc.GrpcStubMock;
import bisq.trade.mu_sig.grpc.PubKeySharesRequest;
import bisq.trade.mu_sig.grpc.PubKeySharesResponse;
import bisq.trade.mu_sig.messages.MuSigSetupTradeMessage_A;
import bisq.trade.protocol.events.SendTradeMessageHandler;

import java.security.GeneralSecurityException;

public class MuSigTakeOfferEventHandler extends SendTradeMessageHandler<MuSigTrade> {

    public MuSigTakeOfferEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        // Request PubKeyShares from rust server
        PubKeySharesRequest pubKeySharesRequest = new PubKeySharesRequest(trade.getId(), trade.getTradeRole());
        GrpcStubMock stub = new GrpcStubMock();
        PubKeySharesResponse pubKeySharesResponse = stub.initTrade(pubKeySharesRequest);

        BisqMuSigContract contract = trade.getContract();
        try {
            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(contractSignatureData);
            sendMessage(new MuSigSetupTradeMessage_A(StringUtils.createUid(),
                    trade.getId(),
                    trade.getProtocolVersion(),
                    trade.getMyIdentity().getNetworkId(),
                    trade.getPeer().getNetworkId(),
                    contract,
                    contractSignatureData,
                    pubKeySharesResponse));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitToModel(ContractSignatureData contractSignatureData) {
        trade.getTaker().getContractSignatureData().set(contractSignatureData);
    }
}
