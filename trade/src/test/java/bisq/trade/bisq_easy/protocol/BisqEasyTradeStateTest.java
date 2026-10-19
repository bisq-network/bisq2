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

}
