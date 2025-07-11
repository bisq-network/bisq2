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

import java.util.ArrayList;
import java.util.List;

public class UIClock {
    private static final List<Runnable> onSecondTickListeners = new ArrayList<>();
    private static final List<Runnable> onMinuteTickListeners = new ArrayList<>();
    private static UIScheduler scheduler;
    private static int secondTicks;

    public static void initialize() {
        scheduler = UIScheduler.run(() -> {
            secondTicks++;
            if (secondTicks == 60) {
                secondTicks = 0;
                onMinuteTickListeners.forEach(Runnable::run);
            }
            onSecondTickListeners.forEach(Runnable::run);
        }).periodically(1000);
    }

    public static void shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        onSecondTickListeners.clear();
    }

    public static void addOnSecondTickListener(Runnable listener) {
        onSecondTickListeners.add(listener);
    }

    public static void removeOnSecondTickListener(Runnable listener) {
        onSecondTickListeners.remove(listener);
    }

    public static void addOnMinuteTickListener(Runnable listener) {
        onMinuteTickListeners.add(listener);
    }

    public static void removeOnMinuteTickListener(Runnable listener) {
        onMinuteTickListeners.remove(listener);
    }
}
