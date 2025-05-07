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

package bisq.trade.mu_sig.protocol;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigBuyersCooperativeCloseTimeoutEvent;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigBuyersCooperativeCloseTimeoutEventHandler;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigPaymentInitiatedEvent;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigPaymentInitiatedEventHandler;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigTakeOfferEvent;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigTakeOfferEventHandler;
import bisq.trade.mu_sig.messages.network.MuSigPaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.handler.buyer_as_taker.MuSigPaymentReceivedMessage_F_Handler;
import bisq.trade.mu_sig.messages.network.handler.buyer_as_taker.MuSigSetupTradeMessage_B_Handler;
import bisq.trade.mu_sig.messages.network.handler.buyer_as_taker.MuSigSetupTradeMessage_D_Handler;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_FORCE_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_INITIALIZED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_INITIATED_PAYMENT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_AS_TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.INIT;


public class MuSigBuyerAsTakerProtocol extends MuSigProtocol {

    public MuSigBuyerAsTakerProtocol(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }


    @Override
    public void configTransitions() {
        // Setup trade
        from(INIT)
                .on(MuSigTakeOfferEvent.class)
                .run(MuSigTakeOfferEventHandler.class)
                .to(BUYER_AS_TAKER_INITIALIZED_TRADE)
                .then()
                .from(BUYER_AS_TAKER_INITIALIZED_TRADE)
                .on(MuSigSetupTradeMessage_B.class)
                .run(MuSigSetupTradeMessage_B_Handler.class)
                .to(BUYER_AS_TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)
                .then()
                .from(BUYER_AS_TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)
                .on(MuSigSetupTradeMessage_D.class)
                .run(MuSigSetupTradeMessage_D_Handler.class)
                .to(BUYER_AS_TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)

                // Settlement
                .then()
                .from(BUYER_AS_TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)
                .on(MuSigPaymentInitiatedEvent.class)
                .run(MuSigPaymentInitiatedEventHandler.class)
                .to(BUYER_AS_TAKER_INITIATED_PAYMENT)

                // Close trade
                .then()
                .branch(
                        path("Cooperative closure")
                                .from(BUYER_AS_TAKER_INITIATED_PAYMENT)
                                .on(MuSigPaymentReceivedMessage_F.class)
                                .run(MuSigPaymentReceivedMessage_F_Handler.class)
                                .to(BUYER_AS_TAKER_CLOSED_TRADE),

                        path("Uncooperative closure")
                                .from(BUYER_AS_TAKER_INITIATED_PAYMENT)
                                .on(MuSigBuyersCooperativeCloseTimeoutEvent.class)
                                .run(MuSigBuyersCooperativeCloseTimeoutEventHandler.class)
                                .to(BUYER_AS_TAKER_FORCE_CLOSED_TRADE)
                );
    }
}