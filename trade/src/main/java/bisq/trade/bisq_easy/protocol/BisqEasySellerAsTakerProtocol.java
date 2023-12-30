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

public class BisqEasySellerAsTakerProtocol extends BisqEasyProtocol {

    public BisqEasySellerAsTakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    public void configTransitions() {
        addTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .run(BisqEasyTakeOfferEventHandler.class)
                .to(TAKER_SENT_TAKE_OFFER_REQUEST);

        addTransition()
                .from(TAKER_SENT_TAKE_OFFER_REQUEST)
                .on(BisqEasyTakeOfferResponse.class)
                .run(BisqEasyTakeOfferResponseHandler.class)
                .to(TAKER_RECEIVED_TAKE_OFFER_RESPONSE);

        // Option 1: Seller sends first account data, then buyer sends btc address
        addTransition()
                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyAccountDataEvent.class)
                .run(BisqEasyAccountDataEventHandler.class)
                .to(SELLER_SENT_ACCOUNT_DATA);

        addTransition()
                .from(SELLER_SENT_ACCOUNT_DATA)
                .on(BisqEasyBtcAddressMessage.class)
                .run(BisqEasyBtcAddressMessageHandler.class)
                .to(SELLER_RECEIVED_BTC_ADDRESS);

        addTransition()
                .from(SELLER_RECEIVED_BTC_ADDRESS)
                .on(BisqEasyConfirmFiatSentMessage.class)
                .run(BisqEasyConfirmFiatSentMessageHandler.class)
                .to(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        // Option 2: Buyer sends first btc address, then seller sends account data
        addTransition()
                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyBtcAddressMessage.class)
                .run(BisqEasyBtcAddressMessageHandler.class)
                .to(SELLER_RECEIVED_BTC_ADDRESS);

        addTransition()
                .from(SELLER_RECEIVED_BTC_ADDRESS)
                .on(BisqEasyAccountDataEvent.class)
                .run(BisqEasyAccountDataEventHandler.class)
                .to(SELLER_SENT_ACCOUNT_DATA);

        addTransition()
                .from(SELLER_SENT_ACCOUNT_DATA)
                .on(BisqEasyConfirmFiatSentMessage.class)
                .run(BisqEasyConfirmFiatSentMessageHandler.class)
                .to(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        // from here it's the same
        addTransition()
                .from(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyConfirmFiatReceiptEvent.class)
                .run(BisqEasyConfirmFiatReceivedEventHandler.class)
                .to(SELLER_CONFIRMED_FIAT_RECEIPT);

        addTransition()
                .from(SELLER_CONFIRMED_FIAT_RECEIPT)
                .on(BisqEasyConfirmBtcSentEvent.class)
                .run(BisqEasyConfirmBtcSentEventHandler.class)
                .to(SELLER_SENT_BTC_SENT_CONFIRMATION);

        addTransition()
                .from(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyBtcConfirmedEvent.class)
                .run(BisqEasyBtcConfirmedEventHandler.class)
                .to(BTC_CONFIRMED);


        // Reject trade
        addTransition()
                .from(TAKER_SENT_TAKE_OFFER_REQUEST)
                .on(BisqEasyRejectTradeEvent.class)
                .run(BisqEasyRejectTradeEventHandler.class)
                .to(REJECTED);
        addTransition()
                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeEvent.class)
                .run(BisqEasyRejectTradeEventHandler.class)
                .to(REJECTED);

        // Peer rejected trade
        addTransition()
                .from(TAKER_SENT_TAKE_OFFER_REQUEST)
                .on(BisqEasyRejectTradeMessage.class)
                .run(BisqEasyRejectTradeMessageHandler.class)
                .to(REJECTED);
        addTransition()
                .from(TAKER_RECEIVED_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeMessage.class)
                .run(BisqEasyRejectTradeMessageHandler.class)
                .to(REJECTED);


        // Cancel trade
        addTransition()
                .from(SELLER_SENT_ACCOUNT_DATA)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_RECEIVED_BTC_ADDRESS)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_CONFIRMED_FIAT_RECEIPT)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);

        // Peer cancelled trade
        addTransition()
                .from(SELLER_SENT_ACCOUNT_DATA)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_RECEIVED_BTC_ADDRESS)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_CONFIRMED_FIAT_RECEIPT)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(CANCELLED);
        addTransition()
                .from(SELLER_SENT_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(CANCELLED);
    }
}
