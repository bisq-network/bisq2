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

package bisq.trade_protocol.bisq_easy.events;

import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.trade_protocol.bisq_easy.BisqEasyTradeModel;
import bisq.trade_protocol.bisq_easy.ServiceProvider;
import bisq.trade_protocol.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.trade_protocol.fsm.Event;
import bisq.trade_protocol.tasks.SendTradeProtocolMessageHandler;

import java.security.GeneralSecurityException;

public class BisqEasyTakeOfferEventHandler extends SendTradeProtocolMessageHandler<BisqEasyTradeModel> {

    public BisqEasyTakeOfferEventHandler(ServiceProvider serviceProvider,
                                         BisqEasyTradeModel model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferEvent bisqEasyTakeOfferEvent = (BisqEasyTakeOfferEvent) event;
        BisqEasyContract bisqEasyContract = bisqEasyTakeOfferEvent.getBisqEasyContract();
        Identity takerIdentity = bisqEasyTakeOfferEvent.getTakerIdentity();
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