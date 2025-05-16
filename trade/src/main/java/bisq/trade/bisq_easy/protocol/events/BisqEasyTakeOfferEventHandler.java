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
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.protocol.handler.TradeEventHandlerAsMessageSender;

import java.security.GeneralSecurityException;

public class BisqEasyTakeOfferEventHandler extends TradeEventHandlerAsMessageSender<BisqEasyTrade> {

    private ContractSignatureData contractSignatureData;

    public BisqEasyTakeOfferEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void processEvent(Event event) {
        try {
            contractSignatureData = serviceProvider.getContractService().signContract(trade.getContract(),
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void commitToModel() {
        trade.getTaker().getContractSignatureData().set(contractSignatureData);
    }

    @Override
    protected void sendMessage() {
        send(new BisqEasyTakeOfferRequest(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                trade.getContract(),
                contractSignatureData));
    }
}