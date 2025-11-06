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
     * Creates a cached-style {@link ExecutorService} backed by a {@link ThreadPoolExecutor}
     * that dynamically creates new threads as needed and reuses idle threads when possible.
     * This configuration uses a {@link SynchronousQueue}, meaning tasks are never queued:
     * each submitted task must be immediately handed off to an available thread.
     *
     * <p><b>Behavior summary when submitting tasks:</b></p>
     * <ol>
     *   <li>If fewer than {@code corePoolSize} threads are running, a new thread is created
     *       immediately to execute the task.</li>
     *   <li>If {@code corePoolSize} threads are already running but a previously created thread
     *       has become idle (within {@code keepAliveInSeconds}), that thread will be reused.</li>
     *   <li>If all threads are busy and the current pool size is less than {@code maxPoolSize},
     *       a new thread is created to handle the incoming task.</li>
     *   <li>If all threads are busy and the pool has already reached {@code maxPoolSize},
     *       the task is <b>rejected</b> and passed to the configured
     *       {@link RejectedExecutionHandler} (by default {@link AbortPolicyWithLogging}).</li>
     * </ol>
     *
     * <p><b>Role of the {@link SynchronousQueue}:</b>
     * A {@code SynchronousQueue} has <em>no capacity</em> — it acts as a direct handoff between
     * producer and worker thread. Tasks are never stored or buffered. Consequently:</p>
     * <ul>
     *   <li>The pool size can grow rapidly under heavy load, up to {@code maxPoolSize}.</li>
     *   <li>There is no task queuing delay, but rejection occurs as soon as all threads are busy
     *       and the pool is full.</li>
     *   <li>This makes it suitable for highly concurrent workloads with short-lived tasks.</li>
     * </ul>
     *
     * <p>Threads above the core size that remain idle for longer than
     * {@code keepAliveInSeconds} are terminated, allowing the pool to shrink back toward the
     * core size when demand decreases.</p>
     *
     * @param name              the name prefix for threads created by this executor
     * @param corePoolSize      the minimum number of threads to keep in the pool
     * @param maxPoolSize       the maximum number of threads allowed in the pool
     * @param keepAliveInSeconds the time for which idle non-core threads are kept alive
     * @return a new {@link ExecutorService} that behaves like a bounded cached thread pool
     *
     * @see ThreadPoolExecutor
     * @see SynchronousQueue
     * @see RejectedExecutionHandler
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
     * Creates a bounded, cached-style {@link ExecutorService} backed by a {@link ThreadPoolExecutor}
     * that maintains a configurable core and maximum pool size, an optional bounded queue,
     * and a custom thread factory and rejection policy.
     *
     * <p><b>Behavior summary when submitting tasks:</b></p>
     * <ol>
     *   <li>If fewer than {@code corePoolSize} threads are running, a new thread is created immediately
     *       to execute the task (bypassing the queue).</li>
     *   <li>Once the number of running threads reaches {@code corePoolSize}, new tasks are
     *       <b>enqueued</b> in the {@link LinkedBlockingQueue} (up to {@code queueCapacity}).</li>
     *   <li>If the queue is full and fewer than {@code maxPoolSize} threads are active, a new
     *       thread is created to handle the task.</li>
     *   <li>If both the queue is full and the pool has already reached {@code maxPoolSize},
     *       the task is <b>rejected</b> and passed to the provided
     *       {@link RejectedExecutionHandler}.</li>
     * </ol>
     *
     * <p><b>Important:</b> The {@link LinkedBlockingQueue} used here is <em>bounded</em> by
     * {@code queueCapacity}. If an unbounded queue were used instead (e.g. new
     * {@code LinkedBlockingQueue<>()}), the executor would never create more than
     * {@code corePoolSize} threads, because the queue would never appear "full". Tasks would
     * simply accumulate in the queue, potentially leading to unbounded memory usage.</p>
     *
     * <p>Threads above the core size that remain idle for longer than
     * {@code keepAliveInSeconds} are terminated, allowing the pool to shrink back toward the
     * core size under low load.</p>
     *
     * @param name              the name prefix used for threads created by this executor
     * @param corePoolSize      the minimum number of threads to keep in the pool
     * @param maxPoolSize       the maximum number of threads to allow in the pool
     * @param keepAliveInSeconds time to keep extra threads alive when idle
     * @param queueCapacity     the maximum number of tasks that can be queued before new threads are created
     * @param handler           the handler invoked when execution is blocked and the queue is full
     * @return a new {@link ExecutorService} with bounded caching behavior
     *
     * @see ThreadPoolExecutor
     * @see LinkedBlockingQueue
     * @see RejectedExecutionHandler
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
