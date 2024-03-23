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
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.Flow;

public class DirectoryEventPublisher implements Flow.Publisher<Path> {
    private final WatchService watchService;
    private final Path directoryPath;
    private final Set<WatchEvent.Kind<?>> watchEventKinds;

    public DirectoryEventPublisher(WatchService watchService, Path directoryPath, Set<WatchEvent.Kind<?>> watchEventKinds) {
        this.watchService = watchService;
        this.directoryPath = directoryPath;
        this.watchEventKinds = watchEventKinds;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Path> subscriber) {
        var subscription = new DirectoryEventSubscription(subscriber, watchService, directoryPath, watchEventKinds);
        subscriber.onSubscribe(subscription);
    }
}
