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

import bisq.common.threading.DiscardNewestPolicy;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.MaxSizeAwareDeque;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NetworkExecutors {
    public static ThreadPoolExecutor CONNECTION_READ;

    public static void initialize() {
        MaxSizeAwareDeque deque = new MaxSizeAwareDeque(20);
        CONNECTION_READ = new ThreadPoolExecutor(
                12,
                40,
                30,
                TimeUnit.SECONDS,
                deque,
                ExecutorFactory.getThreadFactory("Connection.read"),
                new DiscardNewestPolicy());
        deque.setExecutor(CONNECTION_READ);
    }

    public static void shutdown() {
        ExecutorFactory.shutdownAndAwaitTermination(CONNECTION_READ);
    }
}
