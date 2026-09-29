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
        CountDownLatch allRunsDone = new CountDownLatch(3);
        Scheduler scheduler = Scheduler.run(() -> {
                    runs.incrementAndGet();
                    allRunsDone.countDown();
                    throw new IllegalStateException("Simulated failure");
                })
                .repeated(0, 10, TimeUnit.MILLISECONDS, 3);

        try {
            assertTrue(allRunsDone.await(10, TimeUnit.SECONDS));
            assertEquals(3, runs.get());
            assertEquals(3, scheduler.getCounter());
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
}
