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

import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class DiscardNewestPolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            log.warn("Executor was already shut down");
            return;
        }
        BlockingQueue<Runnable> queue = executor.getQueue();
        if (queue instanceof Deque<?> deque) {
            log.warn("Task rejected as queue is full. We remove the newest task from the deque and retry to execute the current task.");
            deque.pollLast(); // remove newest
            executor.execute(runnable); // retry execution with current task
        } else {
            log.warn("Task rejected as queue is full. Queue is not type of Deque. We remove the oldest task instead of the intended policy to remove the newest.");
            queue.poll();
            executor.execute(runnable); // retry execution with current task
            // Fallback to default discard if not a Deque
            // or just silently drop
        }
    }
}

