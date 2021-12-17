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

package network.misq.common.timer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class TickEmitter {
    public interface Listener {
        void onTick(long now);
    }

    private static final long INTERVAL = 10;
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    static {
        executorService.scheduleAtFixedRate(() -> {
            listeners.forEach(task -> task.onTick(System.currentTimeMillis()));
        }, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
    }

    static void addListener(Listener listener) {
        listeners.add(listener);
    }

    static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

}
