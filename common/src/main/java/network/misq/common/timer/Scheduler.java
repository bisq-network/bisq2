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
import java.util.concurrent.*;

/**
 * Global scheduler which is triggered by the TickEmitter.onTick calls every 10 ms.
 * Precise timing is not a priority of that scheduler but avoidance to create new Timer threads for each scheduled execution.
 * If no executor is defined we use the global work-stealing pool (ForkJoinPool using a pool size of the number of
 * available CPU cores).
 * The timing precision should be between 10 and 20 ms.
 */
@Slf4j
public class Scheduler implements TaskScheduler, TickEmitter.Listener {
    private Runnable task;
    private ExecutorService executor = ExecutorFactory.GLOBAL_WORK_STEALING_POOL;
    private long cycles;

    private volatile boolean stopped;
    private long startTs;
    private long interval;
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    @Getter
    private long counter;

    private Scheduler() {
    }

    public static Scheduler run(Runnable task) {
        Scheduler scheduler = new Scheduler();
        scheduler.task = task;
        return scheduler;
    }

    @Override
    public Scheduler withExecutor(ExecutorService executor) {
        this.executor = executor;
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
        this.cycles = cycles;

        stopped = false;
        startTs = System.currentTimeMillis();
        interval = timeUnit.toMillis(delay);
        TickEmitter.addListener(this);
        return this;
    }

    @Override
    public void stop() {
        stopped = true;
        future.ifPresent(f -> f.cancel(true));
        TickEmitter.removeListener(this);
    }

    @Override
    public void onTick(long now) {
        if (stopped) {
            return;
        }

        if (System.currentTimeMillis() - startTs >= interval) {
            if (future.isPresent()) {
                if (!future.get().isDone()) {
                    return;
                }
            }

            future = Optional.of(CompletableFuture.runAsync(() -> {
                if (stopped) {
                    return;
                }
                try {
                    task.run();
                } catch (CancellationException ignore) {
                } catch (CompletionException e) {
                    stop();
                } catch (Throwable t) {
                    log.error("Running scheduled task failed", t);
                    stop();
                } finally {
                    counter++;
                    if (counter >= cycles) {
                        stop();
                    } else {
                        // We use the current time to ensure that in case the task took longer we wait at least the 
                        // interval period before we execute again
                        startTs = System.currentTimeMillis();
                    }
                }
            }, executor));
        }
    }
}
