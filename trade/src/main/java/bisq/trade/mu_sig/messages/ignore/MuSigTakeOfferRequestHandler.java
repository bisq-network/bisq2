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

package bisq.trade.mu_sig.messages.ignore;

import bisq.common.fsm.Event;
import bisq.contract.ContractSignatureData;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigTakeOfferRequestHandler extends TradeMessageHandler<MuSigTrade, MuSigTakeOfferRequest> implements TradeMessageSender<MuSigTrade> {
    public MuSigTakeOfferRequestHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigTakeOfferRequest message = (MuSigTakeOfferRequest) event;
        verifyMessage(message);
    }

    @Override
    protected void verifyMessage(MuSigTakeOfferRequest message) {
        super.verifyMessage(message);
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData,
                               ContractSignatureData makersContractSignatureData) {
        trade.getTaker().getContractSignatureData().set(takersContractSignatureData);
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }
}
