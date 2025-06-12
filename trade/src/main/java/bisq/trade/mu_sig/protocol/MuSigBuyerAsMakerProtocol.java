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
import bisq.trade.mu_sig.events.buyer.BuyersCloseTradeTimeoutEvent;
import bisq.trade.mu_sig.events.buyer.BuyersCloseTradeTimeoutEventHandler;
import bisq.trade.mu_sig.events.buyer.PaymentInitiatedEvent;
import bisq.trade.mu_sig.events.buyer.PaymentInitiatedEventHandler;
import bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadAndDepositTxMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_C;
import bisq.trade.mu_sig.messages.network.handler.buyer.PaymentReceivedMessage_F_Handler;
import bisq.trade.mu_sig.messages.network.handler.buyer_as_maker.SendAccountPayloadAndDepositTxMessage_Handler;
import bisq.trade.mu_sig.messages.network.handler.buyer_as_maker.SetupTradeMessage_C_Handler;
import bisq.trade.mu_sig.messages.network.handler.maker.SetupTradeMessage_A_Handler;
import lombok.extern.slf4j.Slf4j;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_FORCE_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_INITIATED_PAYMENT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.DEPOSIT_TX_CONFIRMED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED_AT_PEER;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.INIT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX;

@Slf4j
public final class MuSigBuyerAsMakerProtocol extends MuSigProtocol {

    public MuSigBuyerAsMakerProtocol(ServiceProvider serviceProvider, MuSigTrade model) {
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
                .on(SetupTradeMessage_A.class)
                .run(SetupTradeMessage_A_Handler.class)
                .to(MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES)

                .then()
                .from(MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES)
                .on(SetupTradeMessage_C.class)
                .run(SetupTradeMessage_C_Handler.class)
                .to(MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX)

                .then()
                .from(MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX)
                .on(SendAccountPayloadAndDepositTxMessage.class)
                .run(SendAccountPayloadAndDepositTxMessage_Handler.class)
                .to(MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX)

                // Deposit confirmation phase
                .then()
                .from(MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX)
                .on(DepositTxConfirmedEvent.class)
                .run(DepositTxConfirmedEventHandler.class)
                .to(DEPOSIT_TX_CONFIRMED)

                // Settlement
                .then()
                .from(DEPOSIT_TX_CONFIRMED)
                .on(PaymentInitiatedEvent.class)
                .run(PaymentInitiatedEventHandler.class)
                .to(BUYER_INITIATED_PAYMENT)

                // Close trade
                .then()
                .branch(
                        path("Cooperative closure")
                                .from(BUYER_INITIATED_PAYMENT)
                                .on(PaymentReceivedMessage_F.class)
                                .run(PaymentReceivedMessage_F_Handler.class)
                                .to(BUYER_CLOSED_TRADE),

                        path("Uncooperative closure")
                                .from(BUYER_INITIATED_PAYMENT)
                                .on(BuyersCloseTradeTimeoutEvent.class)
                                .run(BuyersCloseTradeTimeoutEventHandler.class)
                                .to(BUYER_FORCE_CLOSED_TRADE)
                );
    }
}