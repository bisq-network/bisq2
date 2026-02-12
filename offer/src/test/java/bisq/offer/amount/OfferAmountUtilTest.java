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

package bisq.offer.amount;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OfferAmountUtilTest {

    @Test
    void calculateSecurityDepositUsesPercentageAndCode() {
        Monetary input = Coin.asBtcFromValue(100_000_000L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDeposit(input, 0.25);
        assertEquals("BTC", deposit.getCode());
        assertEquals(25_000_000L, deposit.getValue());
    }

    @Test
    void calculateSecurityDepositRoundsHalfUp() {
        Monetary input = Coin.asBtcFromValue(3L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDeposit(input, 0.5);
        assertEquals(2L, deposit.getValue());
    }
}
