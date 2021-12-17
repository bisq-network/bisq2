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

package network.misq.network.p2p.services.peergroup;

import network.misq.network.p2p.node.Address;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Quarantine {
    public enum Reason {
        ADDRESS_VALIDATION_FAILED,
        ADDRESS_VALIDATION_REQUEST_ON_OUTBOUND_CON
    }

    public record Entry(Reason reason, long eventDate) {
    }

    private final Map<Address, Entry> entryMap = new ConcurrentHashMap<>();

    public Quarantine() {
    }

    public void add(Address address, Reason reason) {
        entryMap.put(address, new Entry(reason, System.currentTimeMillis()));
    }

    public boolean isInQuarantine(Address address) {
        return entryMap.containsKey(address);
    }

    public boolean isNotInQuarantine(Address address) {
        return !isInQuarantine(address);
    }
}