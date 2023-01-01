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

package bisq.wallets.electrum;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FileCreationWatcher {
    private final Path directoryToWatch;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FileCreationWatcher(Path directoryToWatch) {
        this.directoryToWatch = directoryToWatch;
    }

    public Future<Path> waitUntilNewFileCreated() {
        return executor.submit(this::waitForNewFile);
    }

    private Path waitForNewFile() throws IOException, InterruptedException {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directoryToWatch.register(watchService,
                    new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE},
                    SensitivityWatchEventModifier.HIGH);
            WatchKey watchKey = watchService.poll(1, TimeUnit.MINUTES);
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> castedWatchEvent = (WatchEvent<Path>) event;
                Path filename = castedWatchEvent.context();
                return directoryToWatch.resolve(filename);
            }
        }

        throw new IllegalStateException("FileCreationWatcher terminated prematurely.");
    }
}
