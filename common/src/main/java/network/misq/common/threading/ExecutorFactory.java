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

package network.misq.common.threading;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExecutorFactory {
    public static final AtomicInteger COUNTER = new AtomicInteger(0);
    public static final ExecutorService WORK_STEALING_POOL = Executors.newWorkStealingPool();
    public static final ExecutorService IO_POOL = getThreadPoolExecutor("IO-pool",
            1,
            200,
            10,
            new SynchronousQueue<>());

    public static CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            ExecutorFactory.shutdownAndAwaitTermination(WORK_STEALING_POOL, 100);
            ExecutorFactory.shutdownAndAwaitTermination(IO_POOL, 100);
        });
    }

    public static void shutdownAndAwaitTermination(ExecutorService executor) {
        shutdownAndAwaitTermination(executor, 100);
    }

    public static void shutdownAndAwaitTermination(ExecutorService executor, long timeoutMs) {
        shutdownAndAwaitTermination(executor, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public static void shutdownAndAwaitTermination(ExecutorService executor, long timeout, TimeUnit unit) {
        //noinspection UnstableApiUsage
        MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit);
    }

    public static ExecutorService newSingleThreadExecutor(String name) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name + "-" + COUNTER.incrementAndGet())
                .setDaemon(true)
                .build();
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name + "-" + COUNTER.incrementAndGet())
                .setDaemon(true)
                .build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    private static ThreadPoolExecutor getThreadPoolExecutor(String name,
                                                            int corePoolSize,
                                                            int maximumPoolSize,
                                                            long keepAliveTimeInSec,
                                                            BlockingQueue<Runnable> workQueue) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .build();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeInSec,
                TimeUnit.SECONDS, workQueue, threadFactory);
        return executor;
    }
}
