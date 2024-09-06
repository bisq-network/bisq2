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

package bisq.network.p2p.services.data.storage;

import bisq.common.application.Service;
import bisq.common.timer.Scheduler;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

// Single instance for running the pruneExpired methods on all store services.
public class PruneExpiredEntriesService implements Service {
    private final Set<Runnable> tasks = new CopyOnWriteArraySet<>();
    private Optional<Scheduler> scheduler = Optional.empty();

    public PruneExpiredEntriesService() {
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        scheduler = Optional.of(Scheduler.run(this::pruneExpired)
                .host(this)
                .runnableName("prune")
                .periodically(1, TimeUnit.SECONDS));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        scheduler.ifPresent(Scheduler::stop);
        return CompletableFuture.completedFuture(true);
    }

    private void pruneExpired() {
        tasks.forEach(Runnable::run);
    }

    public void addTask(Runnable task) {
        tasks.add(task);
    }
}
