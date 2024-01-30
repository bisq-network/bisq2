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

import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyAccountDataMessageHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyAccountDataMessage> {

    public BisqEasyAccountDataMessageHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyAccountDataMessage message = (BisqEasyAccountDataMessage) event;
        verifyMessage(message);
        commitToModel(message.getPaymentAccountData());
    }

    @Override
    protected void verifyMessage(BisqEasyAccountDataMessage message) {
        super.verifyMessage(message);

        checkArgument(StringUtils.isNotEmpty(message.getPaymentAccountData()));
        checkArgument(message.getPaymentAccountData().length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH);
        checkNotNull(message.getBisqEasyOffer());
    }

    private void commitToModel(String paymentAccountData) {
        trade.getPaymentAccountData().set(paymentAccountData);
    }
}
