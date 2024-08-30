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

package bisq.tor.controller.events.listener;

import bisq.tor.controller.events.events.EventType;
import bisq.tor.controller.events.events.HsDescEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public abstract class FilteredHsDescEventListener extends HsDescEventListener {
    private final String serviceId;
    private final Set<HsDescEvent.Action> eventActions;

    public FilteredHsDescEventListener(EventType eventType, String onionAddress) {
        this(eventType, onionAddress, Set.of(HsDescEvent.Action.values()));
    }

    public FilteredHsDescEventListener(EventType eventType, String onionAddress, Set<HsDescEvent.Action> eventActions) {
        super(eventType);
        this.serviceId = onionAddress.replace(".onion", "");
        this.eventActions = eventActions;
    }

    @Override
    public void onHsDescEvent(HsDescEvent hsDescEvent) {
        if (hsDescEvent.getHsAddress().equals(serviceId) && eventActions.contains(hsDescEvent.getAction())) {
            onFilteredEvent(hsDescEvent);
        }
    }

    public abstract void onFilteredEvent(HsDescEvent hsDescEvent);
}
