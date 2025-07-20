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

import bisq.common.platform.PlatformUtils;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class ExecutorFactory {
    public static final int LOW_PRIORITY = 2;
    public static final int DEFAULT_PRIORITY = 3;
    private static final Map<String, ThreadFactory> THREAD_FACTORY_BY_NAME = new HashMap<>();
    public static final ExecutorService WORKER_POOL = newFixedThreadPool("Worker-pool");

    public static boolean shutdownAndAwaitTermination(ExecutorService executor) {
        return shutdownAndAwaitTermination(executor, 100);
    }

    public static boolean shutdownAndAwaitTermination(ExecutorService executor, long timeoutMs) {
        return shutdownAndAwaitTermination(executor, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public static boolean shutdownAndAwaitTermination(ExecutorService executor, long timeout, TimeUnit unit) {
        //noinspection UnstableApiUsage
        return MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit);
    }

    public static ExecutorService newSingleThreadExecutor(String name) {
        ThreadFactory threadFactory = getThreadFactory(getNameWithThreadNum(name));
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
        ThreadFactory threadFactory = getThreadFactory(getNameWithThreadNum(name));
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    /**
     * Uses a SynchronousQueue, so each submitted task requires a new thread as no queuing functionality is provided.
     * To be used when we want to avoid overhead for new thread creation/destruction and no queuing functionality.
     */
    public static ExecutorService newCachedThreadPool(String name) {
        return newCachedThreadPool(name, 1, 100, 5);
    }

    public static ExecutorService newCachedThreadPool(String name, int poolSize, long keepAliveInSeconds) {
        return newCachedThreadPool(name, poolSize, poolSize, keepAliveInSeconds);
    }

    public static ExecutorService newCachedThreadPool(String name,
                                                      int corePoolSize,
                                                      int maxPoolSize,
                                                      long keepAliveInSeconds) {
        return newCachedThreadPool(name, corePoolSize, corePoolSize, keepAliveInSeconds, DEFAULT_PRIORITY);
    }

    public static ExecutorService newCachedThreadPool(String name,
                                                      int corePoolSize,
                                                      int maxPoolSize,
                                                      long keepAliveInSeconds,
                                                      int priority) {
        ThreadFactory threadFactory = getThreadFactory(getNameWithThreadNum(name), priority);
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
        executorService.setKeepAliveTime(keepAliveInSeconds, TimeUnit.SECONDS);
        executorService.setCorePoolSize(corePoolSize);
        executorService.setMaximumPoolSize(maxPoolSize);
        return executorService;
    }

    /**
     * Used when queuing is desired.
     */
    public static ExecutorService newFixedThreadPool(String name) {
        return newFixedThreadPool(name, PlatformUtils.availableProcessors());
    }

    public static ExecutorService newFixedThreadPool(String name, int numThreads) {
        ThreadFactory threadFactory = getThreadFactory(getNameWithThreadNum(name));
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
        ThreadFactory threadFactory = getThreadFactory(getNameWithThreadNum(name));
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeInSec,
                TimeUnit.SECONDS, workQueue, threadFactory);
    }

    public static ThreadFactory getThreadFactory(String name, int priority) {
        synchronized (THREAD_FACTORY_BY_NAME) {
            return THREAD_FACTORY_BY_NAME.computeIfAbsent(
                    name,
                    key ->
                            new ThreadFactoryBuilder()
                                    .setNameFormat(name)
                                    .setDaemon(true)
                                    .setPriority(priority)
                                    .build());
        }
    }

    public static ThreadFactory getThreadFactory(String name) {
        return getThreadFactory(name, DEFAULT_PRIORITY);
    }

    private static String getNameWithThreadNum(String name) {
        return name + "-%d";
    }
}
