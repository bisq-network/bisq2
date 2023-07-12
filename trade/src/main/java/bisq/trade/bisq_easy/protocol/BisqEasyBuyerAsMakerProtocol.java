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

package bisq.trade.bisq_easy.protocol;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.*;

import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.*;

public class BisqEasyBuyerAsMakerProtocol extends BisqEasyProtocol {

    public BisqEasyBuyerAsMakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void configTransitions() {
        addTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferRequest.class)
                .run(BisqEasyTakeOfferRequestHandler.class)
                .to(MAKER_SENT_TAKE_OFFER_RESPONSE);

        addTransition()
                .from(MAKER_SENT_TAKE_OFFER_RESPONSE)
                .on(BisqEasyAccountDataMessage.class)
                .run(BisqEasyAccountDataMessageHandler.class)
                .to(BUYER_RECEIVED_ACCOUNT_DATA);

        addTransition()
                .from(BUYER_RECEIVED_ACCOUNT_DATA)
                .on(BisqEasyConfirmFiatSentEvent.class)
                .run(BisqEasyConfirmFiatSentEventHandler.class)
                .to(BUYER_SENT_FIAT_SENT_CONFIRMATION);

        addTransition()
                .from(BUYER_SENT_FIAT_SENT_CONFIRMATION)
                .on(BisqEasySendBtcAddressEvent.class)
                .run(BisqEasySendBtcAddressEventHandler.class)
                .to(BUYER_SENT_BTC_ADDRESS);

        addTransition()
                .from(BUYER_SENT_BTC_ADDRESS)
                .on(BisqEasyConfirmFiatReceiptMessage.class)
                .run(BisqEasyConfirmFiatReceiptMessageHandler.class)
                .to(BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION);

        addTransition()
                .from(BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION)
                .on(BisqEasyConfirmBtcSentMessage.class)
                .run(BisqEasyConfirmBtcSentMessageHandler.class)
                .to(BUYER_RECEIVED_BTC_SENT_CONFIRMATION);

        addTransition()
                .from(BUYER_RECEIVED_BTC_SENT_CONFIRMATION)
                .on(BisqEasyBtcConfirmedEvent.class)
                .run(BisqEasyBtcConfirmedEventHandler.class)
                .to(BTC_CONFIRMED);
    }
}