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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {
    // ScheduledExecutorService suppresses the later runs of a repeated task after an exception. For example, the
    // migration of the publisher of authorized data runs three times at startup. A failure of the first run must not
    // stop the later runs.
    @Test
    void repeatedTaskRunsAgainAfterAnException() throws InterruptedException {
        AtomicInteger runs = new AtomicInteger();
        Scheduler scheduler = Scheduler.run(() -> {
                    runs.incrementAndGet();
                    throw new IllegalStateException("Simulated failure");
                })
                .repeated(0, 10, TimeUnit.MILLISECONDS, 3);

        try {
            // The scheduler counts a run after the task has thrown and the exception has been logged. A signal from
            // inside the task would arrive before the last run is counted.
            awaitCounter(scheduler, 3);
            assertEquals(3, scheduler.getCounter());
            assertEquals(3, runs.get());
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void periodicTaskRunsAgainAfterAnException() throws InterruptedException {
        CountDownLatch secondRunDone = new CountDownLatch(2);
        Scheduler scheduler = Scheduler.run(() -> {
                    secondRunDone.countDown();
                    throw new IllegalStateException("Simulated failure");
                })
                .periodically(0, 10, TimeUnit.MILLISECONDS);

        try {
            assertTrue(secondRunDone.await(10, TimeUnit.SECONDS));
        } finally {
            scheduler.stop();
        }
    }

    private static void awaitCounter(Scheduler scheduler, long expectedCounter) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (scheduler.getCounter() < expectedCounter && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
    }
}
