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

package bisq.offer.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// Offer sorts its options by hashCode, and the order is part of the proof of work payload. The value must therefore
// be the same on every node, so it must not depend on Enum.hashCode, which uses the object identity.
class FeeOptionHashCodeTest {
    private static final String TX_ID = "a".repeat(64);

    @Test
    void hashCodeDependsOnlyOnValues() {
        FeeOption option = new FeeOption(FeeOption.FeeType.BSQ, 800_000, TX_ID);

        int expected = ((59 + 800_000) * 59 + TX_ID.hashCode()) * 59 + "BSQ".hashCode();
        assertEquals(expected, option.hashCode());
    }

    @Test
    void equalityStillDependsOnTheFeeType() {
        assertEquals(new FeeOption(FeeOption.FeeType.BSQ, 1, TX_ID), new FeeOption(FeeOption.FeeType.BSQ, 1, TX_ID));
        assertNotEquals(new FeeOption(FeeOption.FeeType.BSQ, 1, TX_ID), new FeeOption(FeeOption.FeeType.BTC, 1, TX_ID));
    }
}
