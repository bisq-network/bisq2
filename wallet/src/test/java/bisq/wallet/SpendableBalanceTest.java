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

package bisq.wallet;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.wallet.vo.BalanceBreakdown;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpendableBalanceTest {
    private final Observable<BalanceBreakdown> breakdown = new Observable<>();
    private final SpendableBalance balance = new SpendableBalance(breakdown);

    @Test
    void thereIsNoBalanceBeforeTheFirstBreakdown() {
        assertNull(balance.get());
    }

    @Test
    void theBalanceIsTheSpendablePartOfTheBreakdown() {
        breakdown.set(new BalanceBreakdown(2000, 1500, 500, 7, 3));

        assertEquals(Coin.asBtcFromValue(2000), balance.get());
    }

    @Test
    void anObserverGetsTheCurrentBalanceAndEveryLaterOne() {
        breakdown.set(new BalanceBreakdown(2000, 1500, 500, 0, 0));
        List<Coin> seen = new ArrayList<>();

        balance.addObserver(seen::add);
        breakdown.set(new BalanceBreakdown(5000, 4500, 500, 0, 0));

        assertEquals(List.of(Coin.asBtcFromValue(2000), Coin.asBtcFromValue(5000)), seen);
    }

    @Test
    void anUnboundObserverIsNotCalledAgain() {
        breakdown.set(new BalanceBreakdown(2000, 1500, 500, 0, 0));
        List<Coin> seen = new ArrayList<>();
        Pin pin = balance.addObserver(seen::add);

        pin.unbind();
        breakdown.set(new BalanceBreakdown(5000, 4500, 500, 0, 0));

        assertEquals(List.of(Coin.asBtcFromValue(2000)), seen);
    }

    @Test
    void aRemovedObserverIsNotCalledAgain() {
        breakdown.set(new BalanceBreakdown(2000, 1500, 500, 0, 0));
        List<Coin> seen = new ArrayList<>();
        Consumer<Coin> observer = seen::add;
        balance.addObserver(observer);

        balance.removeObserver(observer);
        breakdown.set(new BalanceBreakdown(5000, 4500, 500, 0, 0));

        assertEquals(List.of(Coin.asBtcFromValue(2000)), seen);
    }

    @Test
    void anObserverAddedTwiceIsCalledOncePerBreakdown() {
        List<Coin> seen = new ArrayList<>();
        Consumer<Coin> observer = seen::add;
        balance.addObserver(observer);
        balance.addObserver(observer);
        seen.clear();

        breakdown.set(new BalanceBreakdown(2000, 1500, 500, 0, 0));

        assertEquals(List.of(Coin.asBtcFromValue(2000)), seen);
    }
}
