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

package bisq.common.io_watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class DirectoryEventSubscription implements Flow.Subscription {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Flow.Subscriber<? super Path> subscriber;
    private final WatchService watchService;
    private final Path directoryPath;
    private final Set<WatchEvent.Kind<?>> watchEventKinds;

    public DirectoryEventSubscription(Flow.Subscriber<? super Path> subscriber,
                                      WatchService watchService,
                                      Path directoryPath,
                                      Set<WatchEvent.Kind<?>> watchEventKinds) {
        this.subscriber = subscriber;
        this.watchService = watchService;
        this.directoryPath = directoryPath;
        this.watchEventKinds = watchEventKinds;
    }

    @Override
    public void request(long l) {
        executorService.submit(this::watchDirectoryForChanges);
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException();
    }

    private void watchDirectoryForChanges() {
        try {
            WatchKey key = watchService.poll(1, TimeUnit.MINUTES);
            if (key == null) {
                var error = new NoDirectoryChangesTimeoutException("No changes in directory for the last minute.");
                subscriber.onError(error);
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (watchEventKinds.contains(event.kind())) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> castedWatchEvent = (WatchEvent<Path>) event;
                    Path filename = castedWatchEvent.context();

                    Path filePath = directoryPath.resolve(filename);
                    subscriber.onNext(filePath);
                }
            }
        } catch (InterruptedException e) {
            subscriber.onError(e);
            return;
        }
        var error = new IllegalStateException();
        subscriber.onError(error);
    }
}
