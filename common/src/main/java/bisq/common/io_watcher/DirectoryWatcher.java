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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.function.Consumer;

public class DirectoryWatcher implements AutoCloseable {
    private final Path directoryPath;
    private final Set<WatchEvent.Kind<?>> watchEventKinds;

    private WatchService watchService;

    public DirectoryWatcher(Path directoryPath, Set<WatchEvent.Kind<?>> watchEventKinds) {
        this.directoryPath = directoryPath;
        this.watchEventKinds = watchEventKinds;
    }

    public void initialize(Consumer<Path> eventConsumer) {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            WatchEvent.Kind<?>[] eventKinds = new WatchEvent.Kind[watchEventKinds.size()];
            watchEventKinds.toArray(eventKinds);
            directoryPath.register(watchService, eventKinds);

            subscribeToChangesAsync(eventConsumer);

        } catch (IOException e) {
            throw new CouldNotInitializeDirectoryWatcherException(e);
        }
    }

    private void subscribeToChangesAsync(Consumer<Path> consumer) {
        var directoryEventPublisher = new DirectoryEventPublisher(watchService, directoryPath, watchEventKinds);
        var directoryEventSubscriber = new DirectoryEventSubscriber(consumer);
        directoryEventPublisher.subscribe(directoryEventSubscriber);
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }
}
