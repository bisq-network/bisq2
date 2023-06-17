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

package bisq.protocol.bisq_easy.taker.tasks;

import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.protocol.bisq_easy.tasks.SendBisqEasyMessageTask;

import java.security.GeneralSecurityException;

public class SendBisqEasyTakeOfferRequest extends SendBisqEasyMessageTask {
    private final Identity takerIdentity;
    private final BisqEasyContract bisqEasyContract;

    public SendBisqEasyTakeOfferRequest(ServiceProvider serviceProvider,
                                        BisqEasyProtocolModel model,
                                        Identity takerIdentity,
                                        BisqEasyContract bisqEasyContract) {
        super(serviceProvider, model);
        this.takerIdentity = takerIdentity;
        this.bisqEasyContract = bisqEasyContract;
    }

    @Override
    public void run() {
        try {
            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract, takerIdentity.getKeyPair());
            model.getTaker().setContractSignatureData(contractSignatureData);

            BisqEasyTakeOfferRequest message = new BisqEasyTakeOfferRequest(takerIdentity.getNetworkId(), bisqEasyContract, contractSignatureData);
            sendMessage(message, model.getMaker().getNetworkId(), takerIdentity.getNodeIdAndKeyPair());

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}