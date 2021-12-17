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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface TaskScheduler {
    TaskScheduler withExecutor(ExecutorService executor);

    TaskScheduler after(long delayMs);

    TaskScheduler after(long delay, TimeUnit timeUnit);

    TaskScheduler periodically(long delayMs);

    TaskScheduler periodically(long delay, TimeUnit timeUnit);

    TaskScheduler repeated(long delayMs, long cycles);

    TaskScheduler repeated(long delay, TimeUnit timeUnit, long cycles);

    void stop();

    long getCounter();
}
