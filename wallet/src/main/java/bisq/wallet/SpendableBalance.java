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
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.wallet.vo.BalanceBreakdown;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// The spendable balance as a view on the published breakdown. It holds no value of its own, so while the breakdown is
// published one answer at a time, an observer which reads the balance and the breakdown in one notification sees one
// answer of the daemon. An observer is called with every new breakdown, also when only another part changed.
final class SpendableBalance implements ReadOnlyObservable<Coin> {
    private final ReadOnlyObservable<BalanceBreakdown> breakdown;
    private final Map<Consumer<Coin>, Consumer<BalanceBreakdown>> breakdownObservers = new ConcurrentHashMap<>();

    SpendableBalance(ReadOnlyObservable<BalanceBreakdown> breakdown) {
        this.breakdown = breakdown;
    }

    @Nullable
    @Override
    public Coin get() {
        return toCoin(breakdown.get());
    }

    @Override
    public Pin addObserver(Consumer<Coin> observer) {
        breakdown.addObserver(breakdownObservers.computeIfAbsent(observer,
                key -> value -> key.accept(toCoin(value))));
        return () -> removeObserver(observer);
    }

    @Override
    public void removeObserver(Consumer<Coin> observer) {
        Consumer<BalanceBreakdown> breakdownObserver = breakdownObservers.remove(observer);
        if (breakdownObserver != null) {
            breakdown.removeObserver(breakdownObserver);
        }
    }

    @Nullable
    private static Coin toCoin(@Nullable BalanceBreakdown breakdown) {
        return breakdown == null ? null : Coin.asBtcFromValue(breakdown.getSpendable());
    }
}
