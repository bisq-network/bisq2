package bisq.common.threading;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ThreadPoolExecutorTest {
   // @Test
    public void testThreadPoolExecutor() throws InterruptedException {
        AtomicInteger numCompleted = new AtomicInteger();
        AtomicInteger numRejected = new AtomicInteger();
        AtomicInteger numQueued = new AtomicInteger();

        int corePoolSize = 1;
        int maxPoolSize = 8;
        int threshold = 8;
        int queueCapacity = 100;
        MockMaxSizeAwareQueue queue = new MockMaxSizeAwareQueue(queueCapacity, numQueued);
        String name = "ThreadPoolExecutorTest";
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                30,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactoryWithCounter(name),
                new AbortPolicyWithLogging(name, queueCapacity, maxPoolSize));
        queue.applyExecutor(executor, threshold);

        int numTasks = 100;
        int delay = 1;
        CountDownLatch latch = new CountDownLatch(numTasks);
        for (int i = 0; i < numTasks; i++) {
            int counter = i;
            try {
                executor.submit(() -> {
                    // log.info("Start task {}", counter);
                    sleep(delay);
                    // log.info("Completed task {}", counter);
                    numCompleted.getAndIncrement();
                    latch.countDown();
                });
            } catch (RejectedExecutionException e) {
                log.error("executor.submit was rejected");
                numRejected.getAndIncrement();
                latch.countDown();
            } catch (Exception e) {
                log.error("executor.submit failed", e);
                latch.countDown();
            }
        }
        latch.await();

        assertEquals(numTasks, numCompleted.get());
        assertEquals(0, numRejected.get());
        assertEquals(numTasks - threshold, numQueued.get());
    }


   // @Test
    public void testThreadPoolExecutor2() throws InterruptedException {
        AtomicInteger numCompleted = new AtomicInteger();
        AtomicInteger numRejected = new AtomicInteger();
        AtomicInteger numQueued = new AtomicInteger();

        int corePoolSize = 1;
        int maxPoolSize = 2;
        int threshold = 2;
        int queueCapacity = 2;
        MockMaxSizeAwareQueue queue = new MockMaxSizeAwareQueue(queueCapacity, numQueued);
        String name = "ThreadPoolExecutorTest";
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                30,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactoryWithCounter(name),
                new AbortPolicyWithLogging(name, queueCapacity, maxPoolSize));
        queue.applyExecutor(executor, threshold);

        int numTasks = 10;
        int delay = 10;
        CountDownLatch latch = new CountDownLatch(numTasks);
        for (int i = 0; i < numTasks; i++) {
            int counter = i;
            try {
                executor.submit(() -> {
                    // log.info("Start task {}", counter);
                    sleep(delay);
                    // log.info("Completed task {}", counter);
                    numCompleted.getAndIncrement();
                    latch.countDown();
                });
            } catch (RejectedExecutionException e) {
                log.error("executor.submit was rejected");
                numRejected.getAndIncrement();
                latch.countDown();
            } catch (Exception e) {
                log.error("executor.submit failed", e);
                latch.countDown();
            }
        }
        latch.await();

        assertEquals(4, numCompleted.get());
        assertEquals(6, numRejected.get());
        assertEquals(2, numQueued.get());
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MockMaxSizeAwareQueue extends MaxSizeAwareQueue {
        private final AtomicInteger numQueued;

        public MockMaxSizeAwareQueue(int capacity, AtomicInteger numQueued) {
            super(capacity);
            this.numQueued = numQueued;
        }

        @Override
        public boolean offer(Runnable runnable) {
            boolean shouldInsert = shouldInsert();
            if (!shouldInsert) {
                return false;
            }
            boolean wasOffered = super.offer(runnable);
            if (wasOffered) {
                //log.info("added to queue");
                numQueued.incrementAndGet();
            } else {
                log.warn("super.offer(runnable) returned false");
            }
            return wasOffered;
        }
    }
}