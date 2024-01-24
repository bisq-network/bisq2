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

import bisq.common.fsm.FsmErrorEvent;
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
    protected void configErrorHandling() {
        fromAny()
                .on(FsmErrorEvent.class)
                .run(BisqEasyFsmErrorEventHandler.class)
                .to(FAILED);

        fromAny()
                .on(BisqEasyReportErrorMessage.class)
                .run(BisqEasyReportErrorMessageHandler.class)
                .to(FAILED_AT_PEER);
    }

    @Override
    public void configTransitions() {
        from(INIT)
                .on(BisqEasyTakeOfferRequest.class)
                .run(BisqEasyTakeOfferRequestHandler.class)
                .to(MAKER_SENT_TAKE_OFFER_RESPONSE)
                .then()
                .branch(
                        path("Option 1: Buyer sends btc address first, then seller sends account data")
                                .from(MAKER_SENT_TAKE_OFFER_RESPONSE)
                                .on(BisqEasySendBtcAddressEvent.class)
                                .run(BisqEasySendBtcAddressEventHandler.class)
                                .to(BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA)
                                .then()
                                .from(BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA)
                                .on(BisqEasyAccountDataMessage.class)
                                .run(BisqEasyAccountDataMessageHandler.class)
                                .to(BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA),
                        path("Option 2: Seller sends account data first, then buyer sends btc address")
                                .from(MAKER_SENT_TAKE_OFFER_RESPONSE)
                                .on(BisqEasyAccountDataMessage.class)
                                .run(BisqEasyAccountDataMessageHandler.class)
                                .to(BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA)
                                .then()
                                .from(BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA)
                                .on(BisqEasySendBtcAddressEvent.class)
                                .run(BisqEasySendBtcAddressEventHandler.class)
                                .to(BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA)
                )
                .then()
                .from(BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA)
                .on(BisqEasyConfirmFiatSentEvent.class)
                .run(BisqEasyConfirmFiatSentEventHandler.class)
                .to(BUYER_SENT_FIAT_SENT_CONFIRMATION)
                .then()
                .from(BUYER_SENT_FIAT_SENT_CONFIRMATION)
                .on(BisqEasyConfirmFiatReceiptMessage.class)
                .run(BisqEasyConfirmFiatReceiptMessageHandler.class)
                .to(BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION)
                .then()
                .from(BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION)
                .on(BisqEasyConfirmBtcSentMessage.class)
                .run(BisqEasyConfirmBtcSentMessageHandler.class)
                .to(BUYER_RECEIVED_BTC_SENT_CONFIRMATION)
                .then()
                .from(BUYER_RECEIVED_BTC_SENT_CONFIRMATION)
                .on(BisqEasyBtcConfirmedEvent.class)
                .run(BisqEasyBtcConfirmedEventHandler.class)
                .to(BTC_CONFIRMED);

        // Reject trade
        from(MAKER_SENT_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeEvent.class)
                .run(BisqEasyRejectTradeEventHandler.class)
                .to(REJECTED);

        // Peer rejected trade
        from(MAKER_SENT_TAKE_OFFER_RESPONSE)
                .on(BisqEasyRejectTradeMessage.class)
                .run(BisqEasyRejectTradeMessageHandler.class)
                .to(PEER_REJECTED);

        // Cancel trade
        fromStates(BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BUYER_SENT_FIAT_SENT_CONFIRMATION,
                BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
                BUYER_RECEIVED_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeMessage.class)
                .run(BisqEasyCancelTradeMessageHandler.class)
                .to(CANCELLED);

        // Peer cancelled trade
        fromStates(BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BUYER_SENT_FIAT_SENT_CONFIRMATION,
                BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
                BUYER_RECEIVED_BTC_SENT_CONFIRMATION)
                .on(BisqEasyCancelTradeEvent.class)
                .run(BisqEasyCancelTradeEventHandler.class)
                .to(PEER_CANCELLED);
    }
}
