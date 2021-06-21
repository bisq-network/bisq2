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

package network.misq.common.util;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

public class ThreadingUtils {

    public static void shutdownAndAwaitTermination(ExecutorService executor) {
        MoreExecutors.shutdownAndAwaitTermination(executor, 10, TimeUnit.MILLISECONDS);
    }

    public static void shutdownAndAwaitTermination(ExecutorService executor, long timeout, TimeUnit unit) {
        MoreExecutors.shutdownAndAwaitTermination(executor, timeout, unit);
    }

    public static ThreadPoolExecutor getThreadPoolExecutor(String name,
                                                           int corePoolSize,
                                                           int maximumPoolSize,
                                                           long keepAliveTimeInSec) {
        return getThreadPoolExecutor(name, corePoolSize, maximumPoolSize, keepAliveTimeInSec,
                new ArrayBlockingQueue<>(maximumPoolSize));
    }

    public static ExecutorService getSingleThreadExecutor(String name) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .build();
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    private static ThreadPoolExecutor getThreadPoolExecutor(String name,
                                                            int corePoolSize,
                                                            int maximumPoolSize,
                                                            long keepAliveTimeInSec,
                                                            BlockingQueue<Runnable> workQueue) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name)
                .setDaemon(true)
                .build();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeInSec,
                TimeUnit.SECONDS, workQueue, threadFactory);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
