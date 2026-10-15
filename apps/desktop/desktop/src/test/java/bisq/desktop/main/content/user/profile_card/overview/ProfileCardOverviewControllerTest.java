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

package bisq.desktop.main.content.user.profile_card.overview;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileCardOverviewControllerTest {
    @Test
    void sumsIndividuallyValidBaseAmounts() {
        List<Monetary> baseAmounts = List.of(Coin.asBtcFromValue(100), Coin.asBtcFromValue(200));
        assertEquals(Optional.of(300L), ProfileCardOverviewController.checkedBaseAmountSum(baseAmounts));
    }

    @Test
    void emptyListSumsToZero() {
        assertEquals(Optional.of(0L), ProfileCardOverviewController.checkedBaseAmountSum(List.of()));
    }

    @Test
    void aggregateOverflowIsNotRepresentable() {
        // Each amount is individually valid but the sum exceeds Long.MAX_VALUE.
        List<Monetary> baseAmounts = List.of(Coin.asBtcFromValue(Long.MAX_VALUE), Coin.asBtcFromValue(1));
        assertEquals(Optional.empty(), ProfileCardOverviewController.checkedBaseAmountSum(baseAmounts));
    }

    @Test
    void nonPositiveBaseAmountsAreDropped() {
        List<Monetary> baseAmounts = List.of(Coin.asBtcFromValue(100), Coin.asBtcFromValue(-50), Coin.asBtcFromValue(0));
        assertEquals(Optional.of(100L), ProfileCardOverviewController.checkedBaseAmountSum(baseAmounts));
    }
}
