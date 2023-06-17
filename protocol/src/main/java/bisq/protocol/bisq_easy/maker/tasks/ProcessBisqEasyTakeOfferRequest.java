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

package bisq.protocol.bisq_easy.maker.tasks;

import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.protocol.bisq_easy.tasks.ProcessBisqEasyMessageTask;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
public class ProcessBisqEasyTakeOfferRequest extends ProcessBisqEasyMessageTask<BisqEasyTakeOfferRequest> {
    private final BisqEasyTakeOfferRequest message;

    public ProcessBisqEasyTakeOfferRequest(ServiceProvider serviceProvider,
                                           BisqEasyProtocolModel model,
                                           BisqEasyTakeOfferRequest message) {
        super(serviceProvider, model);
        this.message = message;
        log.error("message={}", message);
    }

    @Override
    public void run() {
        verifyMessage(message);

        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        BisqEasyOffer bisqEasyOffer = bisqEasyContract.getOffer();

        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNodeId(bisqEasyOffer.getMakerNetworkId().getNodeId()).orElseThrow();
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());

        try {
            model.getTaker().setContractSignatureData(takersContractSignatureData);

            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, myIdentity.getKeyPair());
            model.getMaker().setContractSignatureData(contractSignatureData);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferRequest message) {

    }
}