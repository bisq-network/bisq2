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

package bisq.common.timer;

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Scheduler implements TaskScheduler {
    // We do not use a ScheduledThreadPoolExecutor as the queue cannot be customized. It would cause undesired behaviour
    // in case we would use a static executor for all Scheduler instances and multiple schedule calls would get 
    // queued up instead of starting a new scheduler.
    private Optional<ScheduledExecutorService> executor = Optional.empty();
    private final Runnable task;
    private volatile boolean stopped;
    @Getter
    private long counter;
    private Optional<String> runnableName = Optional.empty();
    private Optional<String> hostClassName = Optional.empty();

    private Scheduler(Runnable task) {
        this.task = task;
    }

    public static Scheduler run(Runnable task) {
        return new Scheduler(task);
    }

    public Scheduler runnableName(String runnableName) {
        this.runnableName = Optional.of(StringUtils.truncate(runnableName, 50));
        return this;
    }

    public Scheduler host(Object host) {
        return host(host.getClass());
    }

    public Scheduler host(Class<?> hostClass) {
        return host(StringUtils.truncate(hostClass.getSimpleName(), 50));
    }

    public Scheduler host(String hostClassName) {
        this.hostClassName = Optional.of(hostClassName);
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
    public Scheduler periodically(long initialDelay, long delay, TimeUnit timeUnit) {
        return repeated(initialDelay, delay, timeUnit, Long.MAX_VALUE);
    }

    @Override
    public Scheduler repeated(long delayMs, long cycles) {
        return repeated(delayMs, TimeUnit.MILLISECONDS, cycles);
    }

    @Override
    public Scheduler repeated(long delay, TimeUnit timeUnit, long cycles) {
        return repeated(delay, delay, timeUnit, cycles);
    }

    @Override
    public Scheduler repeated(long initialDelay, long delay, TimeUnit timeUnit, long cycles) {
        if (stopped) {
            return this;
        }
        if (cycles == 1) {
            executor = Optional.of(ExecutorFactory.newSingleThreadScheduledExecutor(ExecutorFactory.getThreadFactory(getThreadName(false))));
            executor.get().schedule(() -> {
                if (stopped) {
                    return;
                }
                try {
                    task.run();
                } finally {
                    stop();
                }
            }, delay, timeUnit);
        } else {
            executor = Optional.of(ExecutorFactory.newSingleThreadScheduledExecutor(ExecutorFactory.getThreadFactory(getThreadName(true))));
            executor.get().scheduleWithFixedDelay(() -> {
                if (stopped) {
                    return;
                }
                try {
                    task.run();
                } finally {
                    counter++;
                    if (counter >= cycles) {
                        stop();
                    }
                }
            }, initialDelay, delay, timeUnit);
        }
        return this;
    }

    private String getThreadName(boolean isPeriodic) {
        String name = isPeriodic ? "PeriodicScheduler" : "Scheduler";
        String host = hostClassName.map(hostClassName -> "." + hostClassName).orElse("");
        String runnable = runnableName.map(runnableName -> "." + runnableName).orElse("");
        return name + host + runnable;
    }

    @Override
    public void stop() {
        stopped = true;
        executor.ifPresent(ExecutorFactory::shutdownAndAwaitTermination);
        executor = Optional.empty();
    }
}
