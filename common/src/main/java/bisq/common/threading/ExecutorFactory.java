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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExecutorFactory {
    public static final int DEFAULT_PRIORITY = 5;

    /* --------------------------------------------------------------------- */
    // Common ThreadPool Executors
    /* --------------------------------------------------------------------- */

    public static ExecutorService commonForkJoinPool() {
        return ForkJoinPool.commonPool();
    }

    /* --------------------------------------------------------------------- */
    // Single Thread Executors
    /* --------------------------------------------------------------------- */

    public static ExecutorService newSingleThreadExecutor(String name) {
        return Executors.newSingleThreadExecutor(getThreadFactory(name));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
        return Executors.newSingleThreadScheduledExecutor(getThreadFactory(name));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }


    /* --------------------------------------------------------------------- */
    // ThreadPool with fixed thread size
    /* --------------------------------------------------------------------- */

    /**
     * Used when queuing is desired.
     */
    public static ExecutorService newFixedThreadPool(String name) {
        return newFixedThreadPool(name, PlatformUtils.availableProcessors());
    }

    public static ExecutorService newFixedThreadPool(String name, int numThreads) {
        ThreadFactory threadFactory = numThreads == 1 ? getThreadFactory(name) : getThreadFactoryWithCounter(name);
        return Executors.newFixedThreadPool(numThreads, threadFactory);
    }



    /* --------------------------------------------------------------------- */
    // Cached ThreadPool
    /* --------------------------------------------------------------------- */

    /**
     * Creates a cached thread pool similar to {@link Executors#newCachedThreadPool()}, but allows customization
     * of {@code corePoolSize}, {@code maxPoolSize}, and {@code keepAliveInSeconds}, which the standard
     * factory method does not support.
     * <p>
     * This thread pool uses a {@link SynchronousQueue}, meaning it does not queue tasks but instead hands them
     * directly off to available threads. If no threads are available and the pool has not yet reached
     * {@code maxPoolSize}, a new thread is created. If the pool has reached {@code maxPoolSize}, the task is
     * rejected and handled by the {@link ThreadPoolExecutor.AbortPolicy}.
     * <p>
     * Unlike {@link Executors#newCachedThreadPool()}, which sets {@code corePoolSize = 0} and
     * {@code maximumPoolSize = Integer.MAX_VALUE}, this method provides control over the thread pool bounds to
     * better manage system resources and apply safety limits.
     *
     * @param name               the thread name prefix
     * @param corePoolSize       the number of threads to keep alive even when idle
     * @param maxPoolSize        the maximum number of threads to allow in the pool
     * @param keepAliveInSeconds the time to keep excess idle threads alive
     * @return a configured {@link ExecutorService} instance
     */
    public static ExecutorService newCachedThreadPool(String name,
                                                      int corePoolSize,
                                                      int maxPoolSize,
                                                      long keepAliveInSeconds) {
        return new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                keepAliveInSeconds,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                getThreadFactoryWithCounter(name),
                new AbortPolicyWithLogging(name, 0, maxPoolSize));
    }

    public static ExecutorService newCachedThreadPool(String name) {
        return newCachedThreadPool(name, 1, 100, 5);
    }

    public static ExecutorService newCachedThreadPool(String name, int poolSize, long keepAliveInSeconds) {
        return newCachedThreadPool(name, poolSize, poolSize, keepAliveInSeconds);
    }


    /* --------------------------------------------------------------------- */
    // ThreadPool using a bounded queue and caching
    /* --------------------------------------------------------------------- */

    /**
     * Creates a thread pool that scales between a minimum and maximum number of threads, with a bounded task queue.
     * <p>
     * This pool starts with {@code corePoolSize} threads and can grow up to {@code maxPoolSize}
     * if the task load exceeds the capacity of the core threads. Idle threads beyond the core size
     * will be terminated after {@code keepAliveInSeconds}.
     * <p>
     * Tasks are submitted to a bounded {@link LinkedBlockingQueue}, and rejected tasks are handled by the provided
     * {@link RejectedExecutionHandler}.
     *
     * <p>This configuration is useful when you want:</p>
     * <ul>
     *   <li>Moderate parallelism with resource control</li>
     *   <li>Thread reuse for performance</li>
     *   <li>Backpressure through bounded queuing and potential CallerRunsPolicy</li>
     * </ul>
     *
     * @param name               the base name for thread naming (used by the thread factory)
     * @param corePoolSize       the number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
     * @param maxPoolSize        the maximum number of threads to allow in the pool
     * @param keepAliveInSeconds the time to keep extra threads alive when idle
     * @param queueCapacity      the maximum number of tasks to queue before applying the {@code handler}
     * @param handler            the handler to apply when the pool is saturated
     * @return the configured {@link ExecutorService}
     */

    public static ExecutorService boundedCachedPool(String name,
                                                    int corePoolSize,
                                                    int maxPoolSize,
                                                    long keepAliveInSeconds,
                                                    int queueCapacity,
                                                    RejectedExecutionHandler handler) {
        return new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                keepAliveInSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                getThreadFactoryWithCounter(name),
                handler);
    }

    public static ExecutorService boundedCachedPool(String name) {
        int queueCapacity = 100;
        int maxPoolSize = 5;
        return boundedCachedPool(name,
                1,
                maxPoolSize,
                30,
                queueCapacity,
                new AbortPolicyWithLogging(name, queueCapacity, maxPoolSize));
    }


    /* --------------------------------------------------------------------- */
    // ThreadFactory
    /* --------------------------------------------------------------------- */

    public static ThreadFactory getThreadFactoryWithCounter(String name) {
        return getThreadFactory(name + "-%d");
    }

    public static ThreadFactory getThreadFactory(String name) {
        return new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .setPriority(DEFAULT_PRIORITY)
                .build();
    }


    /* --------------------------------------------------------------------- */
    // ShutdownAndAwaitTermination utils
    /* --------------------------------------------------------------------- */

    public static boolean shutdownAndAwaitTermination(ExecutorService executor) {
        return shutdownAndAwaitTermination(executor, 100);
    }

    public static boolean shutdownAndAwaitTermination(ExecutorService executor, long timeoutMs) {
        return shutdownAndAwaitTermination(executor, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public static boolean shutdownAndAwaitTermination(ExecutorService executor, long timeout, TimeUnit unit) {
        // In case the caller would run in the executors thread it would lead to a deadlock,
        // thus we wrap it into a new thread.
        ExecutorService executorService = newSingleThreadExecutor("shutdownAndAwaitTermination");
        //noinspection UnstableApiUsage
        return CompletableFuture.supplyAsync(() -> MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit), executorService)
                .whenComplete((r, t) -> executorService.shutdown())
                .join();
    }
}
