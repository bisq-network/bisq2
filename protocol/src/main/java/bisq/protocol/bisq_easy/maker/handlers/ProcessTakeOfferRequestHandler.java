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

package bisq.protocol.bisq_easy.maker.handlers;

import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ProtocolParty;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.taker.messages.BisqEasyTakeOfferRequest;
import bisq.protocol.fsm.EventHandler;
import bisq.user.profile.UserProfile;

import java.security.GeneralSecurityException;
import java.util.Optional;

public class ProcessTakeOfferRequestHandler implements EventHandler {
    private final ServiceProvider serviceProvider;
    private final BisqEasyProtocolModel model;
    private final BisqEasyTakeOfferRequest message;

    public ProcessTakeOfferRequestHandler(ServiceProvider serviceProvider,
                                          BisqEasyProtocolModel model,
                                          BisqEasyTakeOfferRequest message) {
        this.serviceProvider = serviceProvider;
        this.model = model;
        this.message = message;
    }

    @Override
    public void handle() {
        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        BisqEasyOffer bisqEasyOffer = bisqEasyContract.getOffer();

        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNodeId(bisqEasyOffer.getMakerNetworkId().getNodeId()).orElseThrow();
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());

        try {
            ProtocolParty taker = new ProtocolParty(bisqEasyContract.getTaker().getNetworkId());
            taker.setContractSignatureData(takersContractSignatureData);

            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, myIdentity.getKeyPair());
            ProtocolParty maker = new ProtocolParty(bisqEasyOffer.getMakerNetworkId());
            maker.setContractSignatureData(contractSignatureData);


            model.setBisqEasyContract(bisqEasyContract);
            model.setTaker(taker);
            model.setMaker(maker);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}