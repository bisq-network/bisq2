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

import bisq.common.fsm.FsmErrorEvent;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.events.MuSigFsmErrorEventHandler;
import bisq.trade.mu_sig.events.MuSigReportErrorMessageHandler;
import bisq.trade.mu_sig.events.blockchain.MuSigDepositTxConfirmedEvent;
import bisq.trade.mu_sig.events.blockchain.MuSigDepositTxConfirmedEventHandler;
import bisq.trade.mu_sig.events.seller.MuSigPaymentReceiptConfirmedEvent;
import bisq.trade.mu_sig.events.seller.MuSigPaymentReceiptConfirmedEventHandler;
import bisq.trade.mu_sig.events.seller.MuSigSellersCloseTradeTimeoutEvent;
import bisq.trade.mu_sig.events.seller.MuSigSellersCloseTradeTimeoutEventHandler;
import bisq.trade.mu_sig.events.taker.MuSigTakeOfferEvent;
import bisq.trade.mu_sig.events.taker.MuSigTakeOfferEventHandler;
import bisq.trade.mu_sig.messages.network.MuSigCooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.MuSigPaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.MuSigSetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.handler.seller.MuSigCooperativeClosureMessage_G_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller.MuSigPaymentInitiatedMessage_E_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller_as_taker.MuSigSetupTradeMessage_B_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller_as_taker.MuSigSetupTradeMessage_D_Handler;
import lombok.extern.slf4j.Slf4j;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.DEPOSIT_TX_CONFIRMED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED_AT_PEER;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.INIT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_CONFIRMED_PAYMENT_RECEIPT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_FORCE_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_INITIALIZED_TRADE;

@Slf4j
public final class MuSigSellerAsTakerProtocol extends MuSigProtocol {

    public MuSigSellerAsTakerProtocol(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
        log.error("MuSigSellerAsTakerProtocol not implemented yet");
    }

    @Override
    protected void configErrorHandling() {
        fromAny()
                .on(FsmErrorEvent.class)
                .run(MuSigFsmErrorEventHandler.class)
                .to(FAILED);

        fromAny()
                .on(MuSigReportErrorMessage.class)
                .run(MuSigReportErrorMessageHandler.class)
                .to(FAILED_AT_PEER);
    }

    public void configTransitions() {
        // Setup trade
        from(INIT)
                .on(MuSigTakeOfferEvent.class)
                .run(MuSigTakeOfferEventHandler.class)
                .to(TAKER_INITIALIZED_TRADE)
                .then()

                .from(TAKER_INITIALIZED_TRADE)
                .on(MuSigSetupTradeMessage_B.class)
                .run(MuSigSetupTradeMessage_B_Handler.class)
                .to(TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)
                .then()

                .from(TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)
                .on(MuSigSetupTradeMessage_D.class)
                .run(MuSigSetupTradeMessage_D_Handler.class)
                .to(TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)

                // Deposit confirmation phase
                .then()
                .from(TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)
                .on(MuSigDepositTxConfirmedEvent.class)
                .run(MuSigDepositTxConfirmedEventHandler.class)
                .to(DEPOSIT_TX_CONFIRMED)

                // Wait for buyers payment...

                // Settlement
                .then()
                .from(DEPOSIT_TX_CONFIRMED)
                .on(MuSigPaymentInitiatedMessage_E.class)
                .run(MuSigPaymentInitiatedMessage_E_Handler.class)
                .to(SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE)

                .then()
                .from(SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE)
                .on(MuSigPaymentReceiptConfirmedEvent.class)
                .run(MuSigPaymentReceiptConfirmedEventHandler.class)
                .to(SELLER_CONFIRMED_PAYMENT_RECEIPT)

                // Close trade
                .then()
                .branch(
                        path("Cooperative closure")
                                .from(SELLER_CONFIRMED_PAYMENT_RECEIPT)
                                .on(MuSigCooperativeClosureMessage_G.class)
                                .run(MuSigCooperativeClosureMessage_G_Handler.class)
                                .to(SELLER_CLOSED_TRADE),

                        path("Uncooperative closure")
                                .from(SELLER_CONFIRMED_PAYMENT_RECEIPT)
                                .on(MuSigSellersCloseTradeTimeoutEvent.class)
                                .run(MuSigSellersCloseTradeTimeoutEventHandler.class)
                                .to(SELLER_FORCE_CLOSED_TRADE)
                );
    }
}