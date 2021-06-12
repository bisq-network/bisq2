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

import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.ThreadingUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TimerTest {
    @Test
    public void testTimerDefaults() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Thread caller = Thread.currentThread();
        UserThread.execute(() -> {
            assertEquals(caller, Thread.currentThread());
            latch.countDown();
        });

        long ts = System.currentTimeMillis();
        UserThread.runAfter(() -> {
            //  we get called from timer thread
            assertNotEquals(caller, Thread.currentThread());
            assertTrue(Math.abs(System.currentTimeMillis() - ts - 1000) < 100);
            latch.countDown();
        }, 1);
        latch.await(5, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(3);
        long ts2 = System.currentTimeMillis();
        UserThread.runPeriodically(latch2::countDown, 100, TimeUnit.MILLISECONDS);
        latch2.await(5, TimeUnit.SECONDS);
        assertTrue(Math.abs(System.currentTimeMillis() - ts2 - 300) < 60);
    }

    @Test
    public void testWithCustomExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = ThreadingUtils.getSingleThreadExecutor("testWithCustomExecutor");
        AtomicReference<Thread> caller = new AtomicReference<>();
        executor.execute(() -> {
            caller.set(Thread.currentThread());
        });
        UserThread.setExecutor(executor);
        UserThread.execute(() -> {
            assertEquals(caller.get(), Thread.currentThread());
            latch.countDown();
        });

        long ts = System.currentTimeMillis();
        UserThread.runAfter(() -> {
            //  we get called from timer thread
            assertEquals(caller.get(), Thread.currentThread());
            assertTrue(Math.abs(System.currentTimeMillis() - ts - 1000) < 100);
            latch.countDown();
        }, 1);
        latch.await(5, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(3);
        long ts2 = System.currentTimeMillis();
        UserThread.runPeriodically(() -> {
            assertEquals(caller.get(), Thread.currentThread());
            latch2.countDown();
        }, 100, TimeUnit.MILLISECONDS);
        latch2.await(5, TimeUnit.SECONDS);
        assertTrue(Math.abs(System.currentTimeMillis() - ts2 - 300) < 60);
    }
}
