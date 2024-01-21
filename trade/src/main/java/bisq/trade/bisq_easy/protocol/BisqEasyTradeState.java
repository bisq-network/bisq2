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

import bisq.common.fsm.State;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum BisqEasyTradeState implements State {
    INIT,

    // Take offer
    TAKER_SENT_TAKE_OFFER_REQUEST,
    MAKER_SENT_TAKE_OFFER_RESPONSE,
    TAKER_RECEIVED_TAKE_OFFER_RESPONSE,

    // Account details
    // Option 1: Peer starts sending
    BUYER_DID_NOT_SEND_BTC_ADDRESS_AND_RECEIVED_ACCOUNT_DATA,
    SELLER_DID_NOT_SEND_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,

    // Option 2: Self start sending
    BUYER_SENT_BTC_ADDRESS_AND_WAITING_FOR_ACCOUNT_DATA,
    SELLER_SENT_ACCOUNT_DATA_AND_WAITING_FOR_BTC_ADDRESS,

    // Branch completed and payment data and BTC address are received
    BUYER_SENT_BTC_ADDRESS_AND_RECEIVED_ACCOUNT_DATA,
    SELLER_SENT_ACCOUNT_DATA_AND_RECEIVED_BTC_ADDRESS,

    // Fiat settlement
    BUYER_SENT_FIAT_SENT_CONFIRMATION,
    SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
    SELLER_CONFIRMED_FIAT_RECEIPT,
    BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,

    // BTC transfer
    SELLER_SENT_BTC_SENT_CONFIRMATION,
    BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
    BTC_CONFIRMED(true),

    REJECTED(true),
    PEER_REJECTED(true),

    CANCELLED(true),
    PEER_CANCELLED(true),

    FAILED(true),
    FAILED_AT_PEER(true);

    private final boolean isFinalState;
    private final int ordinal;

    BisqEasyTradeState() {
        this(false);
    }

    BisqEasyTradeState(boolean isFinalState) {
        this.isFinalState = isFinalState;
        ordinal = ordinal();
    }
}
