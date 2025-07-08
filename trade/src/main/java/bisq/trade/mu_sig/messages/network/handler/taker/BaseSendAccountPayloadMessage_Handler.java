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

package bisq.trade.mu_sig.messages.network.handler.taker;

import bisq.account.accounts.AccountPayload;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseSendAccountPayloadMessage_Handler extends MuSigTradeMessageHandler<MuSigTrade, SendAccountPayloadMessage> {
    private AccountPayload<?> accountPayload;

    public BaseSendAccountPayloadMessage_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(SendAccountPayloadMessage message) {
    }

    @Override
    protected void process(SendAccountPayloadMessage message) {
        accountPayload = message.getAccountPayload();

        // We observe the txConfirmationStatus to get informed once the deposit tx is confirmed.
        tradeService.observeDepositTxConfirmationStatus(trade);
    }

    @Override
    protected void commit() {
        trade.getPeer().setAccountPayload(accountPayload);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Taker received the peers account payload.\n" +
                "Taker starts listening for blockchain confirmation of deposit tx.");
    }
}
