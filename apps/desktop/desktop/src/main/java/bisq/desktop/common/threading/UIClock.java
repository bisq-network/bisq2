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

package bisq.desktop.common.threading;

import bisq.common.observable.Pin;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class UIClock {
    private static final List<Runnable> onSecondTickObservers = new CopyOnWriteArrayList<>();
    private static final List<Runnable> onMinuteTickObservers = new CopyOnWriteArrayList<>();
    private static UIScheduler scheduler;
    private static int secondTicks;

    public static void initialize() {
        if (scheduler != null) {
            return;
        }
        scheduler = UIScheduler.run(() -> {
            secondTicks++;
            if (secondTicks == 60) {
                secondTicks = 0;
                onMinuteTickObservers.forEach(Runnable::run);
            }
            onSecondTickObservers.forEach(Runnable::run);
        }).periodically(1000);
    }

    public static void shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        onSecondTickObservers.clear();
        onMinuteTickObservers.clear();
    }

    public static Pin observeSecondTick(Runnable observer) {
        observer.run();
        onSecondTickObservers.add(observer);
        return () -> onSecondTickObservers.remove(observer);
    }

    public static Pin observeMinuteTick(Runnable observer) {
        observer.run();
        onMinuteTickObservers.add(observer);
        return () -> onMinuteTickObservers.remove(observer);
    }
}
