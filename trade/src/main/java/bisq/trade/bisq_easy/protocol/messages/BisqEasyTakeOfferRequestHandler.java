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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.common.fsm.Event;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
public class BisqEasyTakeOfferRequestHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyTakeOfferRequest> implements TradeMessageSender<BisqEasyTrade> {

    public BisqEasyTakeOfferRequestHandler(ServiceProvider serviceProvider,
                                           BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferRequest message = (BisqEasyTakeOfferRequest) event;
        verifyMessage(message);

        BisqEasyContract takersContract = message.getBisqEasyContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        Identity myIdentity = trade.getMyIdentity();
        ContractService contractService = serviceProvider.getContractService();
        BisqEasyContract makersContract = new BisqEasyContract(trade.getOffer(),
                message.getSender(),
                takersContract.getBaseSideAmount(),
                takersContract.getQuoteSideAmount(),
                takersContract.getBaseSidePaymentMethodSpecs(),
                takersContract.getQuoteSidePaymentMethodSpec(),
                takersContract.getMediator());
        try {
            ContractSignatureData makersContractSignatureData = contractService.signContract(makersContract, myIdentity.getKeyPair());
            commitToModel(takersContractSignatureData, makersContractSignatureData);

            BisqEasyTakeOfferResponse response = new BisqEasyTakeOfferResponse(trade.getId(), trade.getMyself().getNetworkId(), makersContract, makersContractSignatureData);
            sendMessage(response, serviceProvider, trade);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferRequest message) {
        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        BisqEasyOffer bisqEasyOffer = bisqEasyContract.getOffer();
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        //todo
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData, ContractSignatureData makersContractSignatureData) {
        trade.getTaker().getContractSignatureData().set(takersContractSignatureData);
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }
}