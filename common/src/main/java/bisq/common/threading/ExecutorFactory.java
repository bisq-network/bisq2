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

package bisq.common.threading;

import bisq.common.util.OsUtils;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class ExecutorFactory {
    public static final ExecutorService WORKER_POOL = newFixedThreadPool("Worker-pool");

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
                .setNameFormat(getNameWithThreadNum(name))
                .setDaemon(true)
                .build();
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getNameWithThreadNum(name))
                .setDaemon(true)
                .build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    /**
     * Uses a SynchronousQueue, so each submitted task requires a new thread as no queuing functionality is provided.
     * To be used when we want to avoid overhead for new thread creation/destruction and no queuing functionality.
     */
    public static ExecutorService newCachedThreadPool(String name) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getNameWithThreadNum(name))
                .setDaemon(true)
                .build();
        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);
        ((ThreadPoolExecutor) executorService).setKeepAliveTime(5, TimeUnit.SECONDS);
        ((ThreadPoolExecutor) executorService).setMaximumPoolSize(1000);
        return executorService;
    }

    /**
     * Used when queuing is desired.
     */
    public static ExecutorService newFixedThreadPool(String name) {
        return newFixedThreadPool(name, OsUtils.availableProcessors());
    }

    public static ExecutorService newFixedThreadPool(String name, int numThreads) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getNameWithThreadNum(name))
                .setDaemon(true)
                .build();
        return Executors.newFixedThreadPool(numThreads, threadFactory);
    }

    public static ThreadPoolExecutor getThreadPoolExecutor(String name,
                                                           int corePoolSize,
                                                           int maximumPoolSize,
                                                           long keepAliveTimeInSec) {
        return getThreadPoolExecutor(name, 1, 10000, 1, new SynchronousQueue<>());
    }

    public static ThreadPoolExecutor getThreadPoolExecutor(String name,
                                                           int corePoolSize,
                                                           int maximumPoolSize,
                                                           long keepAliveTimeInSec,
                                                           BlockingQueue<Runnable> workQueue) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getNameWithThreadNum(name))
                .setDaemon(true)
                .build();

        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeInSec,
                TimeUnit.MILLISECONDS, workQueue, threadFactory);
    }

    private static String getNameWithThreadNum(String name) {
        return name + "-%d";
    }
}
