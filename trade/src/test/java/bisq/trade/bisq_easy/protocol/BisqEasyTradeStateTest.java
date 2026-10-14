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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BisqEasyTradeStateTest {

    private static final Set<BisqEasyTradeState> EXPECTED_FINAL_STATES = Set.of(
            BisqEasyTradeState.BTC_CONFIRMED,
            BisqEasyTradeState.REJECTED,
            BisqEasyTradeState.PEER_REJECTED,
            BisqEasyTradeState.CANCELLED,
            BisqEasyTradeState.PEER_CANCELLED,
            BisqEasyTradeState.FAILED,
            BisqEasyTradeState.FAILED_AT_PEER
    );

    @Test
    @DisplayName("expected final states are marked final")
    void expected_final_states_are_marked_final() {
        for (BisqEasyTradeState state : EXPECTED_FINAL_STATES) {
            assertTrue(state.isFinalState(), state.name() + " should be a final state");
        }
    }

    @Test
    @DisplayName("non final states are not marked final")
    void non_final_states_are_not_marked_final() {
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            if (!EXPECTED_FINAL_STATES.contains(state)) {
                assertFalse(state.isFinalState(), state.name() + " should NOT be a final state");
            }
        }
    }

    @Test
    @DisplayName("init has ordinal zero")
    void init_has_ordinal_zero() {
        assertEquals(0, BisqEasyTradeState.INIT.getOrdinal());
    }

    @Test
    @DisplayName("ordinals are monotonically increasing")
    void ordinals_are_monotonically_increasing() {
        BisqEasyTradeState[] values = BisqEasyTradeState.values();
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i].getOrdinal() > values[i - 1].getOrdinal(),
                    values[i].name() + " ordinal (" + values[i].getOrdinal() +
                            ") should be > " + values[i - 1].name() + " ordinal (" + values[i - 1].getOrdinal() + ")");
        }
    }

    @Test
    @DisplayName("total state count is non final plus final")
    void total_state_count_is_non_final_plus_final() {
        long finalCount = 0;
        long nonFinalCount = 0;
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            if (state.isFinalState()) finalCount++;
            else nonFinalCount++;
        }
        assertEquals(BisqEasyTradeState.values().length, finalCount + nonFinalCount);
        assertTrue(nonFinalCount > 0, "Should have non-final states");
        assertTrue(finalCount > 0, "Should have final states");
    }

    @Test
    @DisplayName("exactly seven final states")
    void exactly_seven_final_states() {
        long finalCount = 0;
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            if (state.isFinalState()) finalCount++;
        }
        assertEquals(EXPECTED_FINAL_STATES.size(), finalCount);
    }

    @Test
    @DisplayName("fiat payment states exist")
    void fiat_payment_states_exist() {
        assertNotNull(BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION);
        assertNotNull(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);
        assertNotNull(BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT);
        assertNotNull(BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION);
    }

    @Test
    @DisplayName("btc transfer states exist")
    void btc_transfer_states_exist() {
        assertNotNull(BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION);
        assertNotNull(BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION);
        assertNotNull(BisqEasyTradeState.BTC_CONFIRMED);
    }

    @Test
    @DisplayName("fiat states are not final")
    void fiat_states_are_not_final() {
        assertFalse(BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION.isFinalState());
        assertFalse(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION.isFinalState());
        assertFalse(BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT.isFinalState());
        assertFalse(BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION.isFinalState());
    }

    @Test
    @DisplayName("btc sent states are not final")
    void btc_sent_states_are_not_final() {
        assertFalse(BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION.isFinalState());
        assertFalse(BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION.isFinalState());
    }

    @Test
    @DisplayName("btc confirmed is final")
    void btc_confirmed_is_final() {
        assertTrue(BisqEasyTradeState.BTC_CONFIRMED.isFinalState());
    }
}
