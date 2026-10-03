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

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * At startup a service replays data which it received from other users before it was initialized: the confidential
 * messages which the node processed, for example from the mailbox, and the data of the network store. The live delivery
 * calls each listener on its own (ConfidentialMessageService, DataService), so the replay does the same: an item whose
 * processing fails does not stop the others nor the start of the service (docs/specifications/network/startup-replay.md).
 */
@Slf4j
public final class StartupReplay {
    private StartupReplay() {
    }

    public static <T> void forEach(Stream<T> items, Consumer<T> handler) {
        items.forEach(item -> {
            try {
                handler.accept(item);
            } catch (RuntimeException e) {
                log.error("Replaying a {} failed", item.getClass().getSimpleName(), e);
            }
        });
    }
}
