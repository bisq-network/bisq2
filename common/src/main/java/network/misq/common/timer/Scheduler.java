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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Scheduler implements TaskScheduler {
    private static final ScheduledExecutorService executor = ExecutorFactory.newScheduledThreadPool("Scheduler", 1);
    private Runnable task;
    private volatile boolean stopped;
    private Optional<ScheduledFuture<?>> future = Optional.empty();

    @Getter
    private long counter;
    private Optional<String> threadName = Optional.empty();

    private Scheduler() {
    }

    public static Scheduler run(Runnable task) {
        Scheduler scheduler = new Scheduler();
        scheduler.task = task;
        return scheduler;
    }

    public Scheduler name(String threadName) {
        this.threadName = Optional.of(threadName);
        return this;
    }

    @Override
    public Scheduler after(long delayMs) {
        return after(delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public Scheduler after(long delay, TimeUnit timeUnit) {
        return repeated(delay, timeUnit, 1);
    }

    @Override
    public Scheduler periodically(long delayMs) {
        return periodically(delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public Scheduler periodically(long delay, TimeUnit timeUnit) {
        return repeated(delay, timeUnit, Long.MAX_VALUE);
    }

    @Override
    public Scheduler repeated(long delayMs, long cycles) {
        return repeated(delayMs, TimeUnit.MILLISECONDS, cycles);
    }

    @Override
    public Scheduler repeated(long delay, TimeUnit timeUnit, long cycles) {
        if (stopped) {
            return this;
        }
        if (delay == 0) {
            log.warn("Delay must be > 0. We set it to 1 ms.");
            delay = 1;
        }
        future = Optional.of(executor.scheduleWithFixedDelay(() -> {
            if (stopped) {
                return;
            }
            threadName.ifPresent(name -> Thread.currentThread().setName(name));
            try {
                task.run();
            } finally {
                counter++;
                if (counter >= cycles) {
                    stop();
                }
            }
        }, delay, delay, timeUnit));
        return this;
    }

    @Override
    public void stop() {
        stopped = true;
        future.ifPresent(f -> f.cancel(true));
    }
}
