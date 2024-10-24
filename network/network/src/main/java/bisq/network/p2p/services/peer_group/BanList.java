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

package bisq.network.p2p.services.peer_group;

import bisq.common.network.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//todo (Critical) check at connection handshake
public class BanList {
    public enum Reason {
        ADDRESS_VALIDATION_FAILED,
        ADDRESS_VALIDATION_REQUEST_ON_OUTBOUND_CON
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Entry {
        private final Reason reason;
        private final long eventDate;

        public Entry(Reason reason, long eventDate) {
            this.reason = reason;
            this.eventDate = eventDate;
        }
    }

    private final Map<Address, Entry> entryMap = new ConcurrentHashMap<>();

    public BanList() {
    }

    public void add(Address address, Reason reason) {
        entryMap.put(address, new Entry(reason, System.currentTimeMillis()));
    }

    public boolean isBanned(Address address) {
        return entryMap.containsKey(address);
    }

    public boolean isNotBanned(Address address) {
        return !isBanned(address);
    }
}