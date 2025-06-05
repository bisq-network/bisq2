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
import bisq.trade.mu_sig.events.blockchain.DepositTxConfirmedEvent;
import bisq.trade.mu_sig.events.blockchain.DepositTxConfirmedEventHandler;
import bisq.trade.mu_sig.events.seller.PaymentReceiptConfirmedEvent;
import bisq.trade.mu_sig.events.seller.PaymentReceiptConfirmedEventHandler;
import bisq.trade.mu_sig.events.seller.SellersCloseTradeTimeoutEvent;
import bisq.trade.mu_sig.events.seller.SellersCloseTradeTimeoutEventHandler;
import bisq.trade.mu_sig.events.taker.MuSigTakeOfferEvent;
import bisq.trade.mu_sig.events.taker.MuSigTakeOfferEventHandler;
import bisq.trade.mu_sig.messages.network.CooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.PaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.handler.seller.CooperativeClosureMessage_G_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller.PaymentInitiatedMessage_E_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller_as_taker.SendAccountPayloadMessage_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller_as_taker.SetupTradeMessage_B_Handler;
import bisq.trade.mu_sig.messages.network.handler.seller_as_taker.SetupTradeMessage_D_Handler;
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
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_INITIALIZED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_RECEIVED_ACCOUNT_PAYLOAD;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX;
@Slf4j
public final class MuSigSellerAsTakerProtocol extends MuSigProtocol {

    public MuSigSellerAsTakerProtocol(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
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

    @Override
    public void configTransitions() {
        // Setup trade
        from(INIT)
                .on(MuSigTakeOfferEvent.class)
                .run(MuSigTakeOfferEventHandler.class)
                .to(TAKER_INITIALIZED_TRADE)

                .then()
                .from(TAKER_INITIALIZED_TRADE)
                .on(SetupTradeMessage_B.class)
                .run(SetupTradeMessage_B_Handler.class)
                .to(TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)

                .then()
                .from(TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES)
                .on(SetupTradeMessage_D.class)
                .run(SetupTradeMessage_D_Handler.class)
                .to(TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)

                .then()
                .from(TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX)
                .on(SendAccountPayloadMessage.class)
                .run(SendAccountPayloadMessage_Handler.class)
                .to(TAKER_RECEIVED_ACCOUNT_PAYLOAD)

                // Deposit confirmation phase
                .then()
                .from(TAKER_RECEIVED_ACCOUNT_PAYLOAD)
                .on(DepositTxConfirmedEvent.class)
                .run(DepositTxConfirmedEventHandler.class)
                .to(DEPOSIT_TX_CONFIRMED)

                // Wait for buyers payment...

                // Settlement
                .then()
                .from(DEPOSIT_TX_CONFIRMED)
                .on(PaymentInitiatedMessage_E.class)
                .run(PaymentInitiatedMessage_E_Handler.class)
                .to(SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE)

                .then()
                .from(SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE)
                .on(PaymentReceiptConfirmedEvent.class)
                .run(PaymentReceiptConfirmedEventHandler.class)
                .to(SELLER_CONFIRMED_PAYMENT_RECEIPT)

                // Close trade
                .then()
                .branch(
                        path("Cooperative closure")
                                .from(SELLER_CONFIRMED_PAYMENT_RECEIPT)
                                .on(CooperativeClosureMessage_G.class)
                                .run(CooperativeClosureMessage_G_Handler.class)
                                .to(SELLER_CLOSED_TRADE),

                        path("Uncooperative closure")
                                .from(SELLER_CONFIRMED_PAYMENT_RECEIPT)
                                .on(SellersCloseTradeTimeoutEvent.class)
                                .run(SellersCloseTradeTimeoutEventHandler.class)
                                .to(SELLER_FORCE_CLOSED_TRADE)
                );
    }
}