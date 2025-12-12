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

package bisq.network;

import bisq.common.threading.DiscardOldestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.MaxSizeAwareQueue;
import lombok.Getter;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class NetworkExecutors {
    @Getter
    private static ThreadPoolExecutor notifyExecutor;
    private static volatile boolean isInitialized;

    public static void initialize(int notifyExecutorMaxPoolSize) {
        checkArgument(!isInitialized, "initialize must not be called twice");
        notifyExecutor = createNotifyExecutor(notifyExecutorMaxPoolSize);

        isInitialized = true;
    }

    public static void shutdown() {
        if (isInitialized) {
            ExecutorFactory.shutdownAndAwaitTermination(notifyExecutor);

            notifyExecutor = null;
            isInitialized = false;
        }
    }

    private static ThreadPoolExecutor createNotifyExecutor(int maxPoolSize) {
        int queueCapacity = 100000;
        MaxSizeAwareQueue queue = new MaxSizeAwareQueue(queueCapacity);
        String name = "Network.notify";
        // We use DiscardOldestPolicy instead of AbortPolicyWithLogging as we do not want to handle in the notification
        // loops the exception, which otherwise would break the loop and escalates up.
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                maxPoolSize,
                30,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactoryWithCounter(name),
                new DiscardOldestPolicy(name, queueCapacity, maxPoolSize));
        queue.applyExecutor(executor, maxPoolSize - 2);
        return executor;
    }
}
