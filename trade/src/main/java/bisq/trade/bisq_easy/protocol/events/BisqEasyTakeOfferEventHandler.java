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

package bisq.trade.bisq_easy.protocol.events;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.protocol.events.SendTradeMessageHandler;

import java.security.GeneralSecurityException;

public class BisqEasyTakeOfferEventHandler extends SendTradeMessageHandler<BisqEasyTrade> {

    public BisqEasyTakeOfferEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyContract bisqEasyContract = trade.getContract();
        try {
            ContractSignatureData contractSignatureData = serviceProvider.getContractService().signContract(bisqEasyContract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(contractSignatureData);
            sendMessage(new BisqEasyTakeOfferRequest(StringUtils.createUid(),
                    trade.getId(),
                    trade.getProtocolVersion(),
                    trade.getMyIdentity().getNetworkId(),
                    trade.getPeer().getNetworkId(),
                    bisqEasyContract,
                    contractSignatureData));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitToModel(ContractSignatureData contractSignatureData) {
        trade.getTaker().getContractSignatureData().set(contractSignatureData);
    }
}