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
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyTakeOfferResponseHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyTakeOfferResponse> implements TradeMessageSender<BisqEasyTrade> {

    public BisqEasyTakeOfferResponseHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferResponse message = (BisqEasyTakeOfferResponse) event;
        verifyMessage(message);
        commitToModel(message.getContractSignatureData());
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferResponse message) {
        super.verifyMessage(message);

        ContractSignatureData makersContractSignatureData = message.getContractSignatureData();
        ContractSignatureData takersContractSignatureData = trade.getTaker().getContractSignatureData().get();
        checkArgument(Arrays.equals(makersContractSignatureData.getContractHash(), takersContractSignatureData.getContractHash()));

        ContractService contractService = serviceProvider.getContractService();
        try {
            checkArgument(contractService.verifyContractSignature(trade.getContract(), makersContractSignatureData));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitToModel(ContractSignatureData makersContractSignatureData) {
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }
}