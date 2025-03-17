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

package bisq.network.tor.controller.events.listener;

import bisq.network.tor.controller.events.events.TorBootstrapEvent;
import bisq.network.tor.controller.events.events.EventType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BootstrapEventListener {
    @Getter
    private final EventType eventType;

    public BootstrapEventListener(EventType eventType) {
        this.eventType = eventType;
    }

    abstract public void onBootstrapStatusEvent(TorBootstrapEvent torBootstrapEvent);
}
