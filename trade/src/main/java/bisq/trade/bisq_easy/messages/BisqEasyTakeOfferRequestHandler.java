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

package bisq.trade.bisq_easy.messages;

import bisq.common.fsm.Event;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.ServiceProvider;
import bisq.trade.tasks.TradeMessageHandler;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
public class BisqEasyTakeOfferRequestHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyTakeOfferRequest> {

    public BisqEasyTakeOfferRequestHandler(ServiceProvider serviceProvider,
                                           BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferRequest message = (BisqEasyTakeOfferRequest) event;
        verifyMessage(message);

        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        BisqEasyOffer bisqEasyOffer = bisqEasyContract.getOffer();

        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNodeId(bisqEasyOffer.getMakerNetworkId().getNodeId()).orElseThrow();
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());

        try {
            model.getPeer().getContractSignatureData().set(takersContractSignatureData);

            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, myIdentity.getKeyPair());
            model.getMyself().getContractSignatureData().set(contractSignatureData);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferRequest message) {

    }
}