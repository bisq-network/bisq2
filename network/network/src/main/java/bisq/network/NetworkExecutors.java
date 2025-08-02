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

import bisq.common.threading.CallerRunsPolicyWithLogging;
import bisq.common.threading.DiscardNewestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.MaxSizeAwareDeque;
import bisq.common.threading.MaxSizeAwareQueue;
import lombok.Getter;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

// TODO use config to set core pool sized
public class NetworkExecutors {
    @Getter
    private static ThreadPoolExecutor networkReadExecutor;
    @Getter
    private static ThreadPoolExecutor networkSendExecutor;
    private static volatile boolean isInitialized;

    public static void initialize() {
        checkArgument(!isInitialized, "initialize must not be called twice");
        networkReadExecutor = createNetworkReadExecutor();
        networkSendExecutor = createNetworkSendExecutor();
        isInitialized = true;
    }

    public static void shutdown() {
        if (isInitialized) {
            ExecutorFactory.shutdownAndAwaitTermination(networkReadExecutor);
            ExecutorFactory.shutdownAndAwaitTermination(networkSendExecutor);
            networkReadExecutor = null;
            networkSendExecutor = null;
            isInitialized = false;
        }
    }


    /**
     * We keep a core pool size of 12 which reflects the target peer group.
     * We allow up to 40 threads which a keepAlive time of 30 seconds for the threads outside the core pool size.
     * If all threads are busy, we add up to 20 tasks into a deque. Once that gets full we drop the newest added task.
     * We choose the newest instead of dropping the oldest as it might serve better for protecting against attacks.
     * This executor must only be used for direct network read operations, which happen in Connection and ConnectionHandshake.
     */
    private static ThreadPoolExecutor createNetworkReadExecutor() {
        MaxSizeAwareDeque deque = new MaxSizeAwareDeque(20);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                12,
                40,
                30,
                TimeUnit.SECONDS,
                deque,
                ExecutorFactory.getThreadFactory("Network.read"),
                new DiscardNewestPolicy());
        deque.setExecutor(executor);
        return executor;
    }

    /**
     * The core pool size is aligned to the broadcasters peer group size of 75% of the peer group (target 12),
     * thus resulting in 9 in case we broadcast our own message.
     * We use a higher queue capacity to allow network bursts to some extent.
     * We use the CallerRunsPolicy thus putting backpressure on the caller in case we exceed the queue capacity.
     * This pool must be used only for sending messages to the network, either in Connection or in ConnectionHandshake.
     * <p>
     * Sending a message start with creating the handshake which starts with creating the socket which is a blocking operation.
     * Then sending the message (blocking) and waiting for the response (blocking read). After successful handshake we create the connection.
     * Every send after that will only have the blocking send operation, but there is a throttle to avoid network burst with a Thread.sleep.
     * Thus, the send operation can take  longer as the actual network IO operation.
     * This whole process is all covered by that executor.
     */
    private static ThreadPoolExecutor createNetworkSendExecutor() {
        MaxSizeAwareQueue queue = new MaxSizeAwareQueue(1000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                9,
                20,
                10,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactory("Network.send"),
                new CallerRunsPolicyWithLogging());
        queue.setExecutor(executor);
        return executor;
    }
}
