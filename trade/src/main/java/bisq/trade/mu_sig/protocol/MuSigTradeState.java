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

import bisq.common.fsm.State;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum MuSigTradeState implements State {
    INIT,

    // In the first part the taker/maker role is the relevant aspect
    // Deposit tx setup phase
    TAKER_INITIALIZED_TRADE,
    MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES,
    TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES,

    // Deposit tx published
    MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX,
    TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX,
    MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
    TAKER_RECEIVED_ACCOUNT_PAYLOAD,

    // Deposit confirmation phase
    DEPOSIT_TX_CONFIRMED,

    // From here on the buyer/seller role is the relevant aspect
    // Settlement phase
    BUYER_INITIATED_PAYMENT,
    SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE,
    SELLER_CONFIRMED_PAYMENT_RECEIPT,

    // Cooperative path
    BUYER_CLOSED_TRADE(true),
    SELLER_CLOSED_TRADE(true),

    // Uncooperative path
    BUYER_FORCE_CLOSED_TRADE(true),
    SELLER_FORCE_CLOSED_TRADE(true),

    FAILED(true),
    FAILED_AT_PEER(true);

    private final boolean isFinalState;
    private final int ordinal;

    MuSigTradeState() {
        this(false);
    }

    MuSigTradeState(boolean isFinalState) {
        this.isFinalState = isFinalState;
        ordinal = ordinal();
    }
}