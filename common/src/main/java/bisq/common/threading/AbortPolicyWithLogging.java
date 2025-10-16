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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class AbortPolicyWithLogging implements RejectedExecutionHandler {
    private final String name;
    private final int queueCapacity;
    private final int maxPoolSize;

    public AbortPolicyWithLogging(String name, int queueCapacity, int maxPoolSize) {
        this.name = name;
        this.queueCapacity = queueCapacity;
        this.maxPoolSize = maxPoolSize;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        log.warn("Task rejected from {} with capacity {} and maxPoolSize {}. We throw a RejectedExecutionException", name, queueCapacity, maxPoolSize);
        throw new RejectedExecutionException("Task " + runnable.getClass().getSimpleName() +
                " rejected from " +
                executor.toString());
    }
}