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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MaxSizeAwareQueue extends LinkedBlockingQueue<Runnable> {
    @Setter
    private ThreadPoolExecutor executor;

    public MaxSizeAwareQueue(int capacity) {
        super(capacity);
    }

    private boolean shouldInsert() {
        return executor != null && executor.getPoolSize() >= executor.getMaximumPoolSize();
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
