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

package bisq.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeIdTest {
    // Persisted trades and the trade IDs of other nodes have these values, so the derivation must not change. The
    // expected values were computed outside of Bisq: UUID version 3 of RIPEMD-160(SHA-256(offerId + takerId + date)).
    @Test
    void tradeIdWithTakeOfferDate() {
        assertThat(TradeId.create("offer-1", "taker-1", 1_700_000_000_000L))
                .isEqualTo("8ed36fad-dbbf-306d-a913-7426e6203ccf");
    }

    @Test
    void tradeIdWithoutTakeOfferDate() {
        assertThat(TradeId.create("offer-1", "taker-1"))
                .isEqualTo("bcd1c077-d378-3b46-829e-d248420bd5dc");
    }
}
