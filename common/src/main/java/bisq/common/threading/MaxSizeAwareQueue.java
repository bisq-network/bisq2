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

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The executor.getPoolSize() call might return a stale value which does not represent the internal value in the
 * ThreadPoolExecutor. This could lead to shouldInsert returning false which triggers the ThreadPoolExecutor to spin
 * up a new thread, but in case it had already reached the maximumPoolSize the rejection handler is triggered.
 * To avoid that edge case, we use the executor.getActiveCount() as well, but as that value could also be stale, we
 * additionally allow to use a smaller insertThreshold for the comparison. If that insertThreshold is for instance
 * maximumPoolSize - 2, we have some good buffer to not run into those edge cases.
 */
@Slf4j
public class MaxSizeAwareQueue extends LinkedBlockingQueue<Runnable> {
    private ThreadPoolExecutor executor;
    // Can be lower as executor.getMaximumPoolSize() to add some tolerance in case the
    // executor.getPoolSize() returns a stale value.
    private int insertThreshold;

    public MaxSizeAwareQueue(int capacity) {
        super(capacity);
    }

    public void applyExecutor(ThreadPoolExecutor executor) {
        applyExecutor(executor, executor.getMaximumPoolSize());
    }

    public void applyExecutor(ThreadPoolExecutor executor, int insertThreshold) {
        this.executor = executor;
        int maximumPoolSize = executor.getMaximumPoolSize();
        checkArgument(insertThreshold <= maximumPoolSize, "insertThreshold must not be larger as executor.getMaximumPoolSize()");
        if (insertThreshold <= 0) {
            log.warn("insertThreshold is <= 0. We use the maximumPoolSize instead. insertThreshold={}", insertThreshold);
            insertThreshold = maximumPoolSize;
        }
        this.insertThreshold = insertThreshold;
    }

    protected boolean shouldInsert() {
        if (executor == null || executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
            return false;
        }
        if (super.remainingCapacity() == 0) {
            //log.debug("No capacity left.");
            return false;
        }
        if (executor.getPoolSize() >= insertThreshold) {
            // log.debug("poolSize exceeds threshold. {} / {}", executor.getPoolSize(), threshold);
            return true;
        }
        if (executor.getActiveCount() >= insertThreshold) {
            // log.debug("activeCount exceeds threshold. {} / {}", executor.getActiveCount(), threshold);
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(Runnable runnable) {
        return shouldInsert() && super.offer(runnable);
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return shouldInsert() && super.offer(runnable, timeout, unit);
    }

    @Override
    public boolean add(Runnable runnable) {
        return shouldInsert() && super.add(runnable);
    }


    @Override
    public boolean addAll(Collection<? extends Runnable> c) {
        if (!shouldInsert()) {
            throw new IllegalStateException("addAll() not allowed before reaching max pool size");
        }
        return super.addAll(c);
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        if (shouldInsert()) {
            super.put(runnable);
        } else {
            throw new IllegalStateException("put() not allowed before reaching max pool size");
        }
    }
}
