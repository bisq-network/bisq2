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

package bisq.network.p2p.services.data.inventory.filter.hash_set;


import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true)
public final class HashSetFilter extends InventoryFilter {
    // FilterEntry has about 24 bytes (hash of 20 bytes + integer). 200_000 items are about 4.8 MB)
    // We should aim to be much below that limit.
    public final static int MAX_ENTRIES = 200_000;

    private final List<HashSetFilterEntry> filterEntries;

    public HashSetFilter(List<HashSetFilterEntry> filterEntries) {
        this(InventoryFilterType.HASH_SET, filterEntries);
    }

    private HashSetFilter(InventoryFilterType inventoryFilterType, List<HashSetFilterEntry> filterEntries) {
        super(inventoryFilterType);

        this.filterEntries = filterEntries;
        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.filterEntries);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(inventoryFilterType == InventoryFilterType.HASH_SET);
        checkArgument(filterEntries.size() < MAX_ENTRIES);
    }

    @Override
    public bisq.network.protobuf.InventoryFilter toProto() {
        return getInventoryFilterBuilder().setHashSetFilter(
                        bisq.network.protobuf.HashSetFilter.newBuilder()
                                .addAllFilterEntries(filterEntries.stream()
                                        .map(HashSetFilterEntry::toProto)
                                        .collect(Collectors.toList())))
                .build();
    }

    public static HashSetFilter fromProto(bisq.network.protobuf.InventoryFilter proto) {
        List<HashSetFilterEntry> entries = proto.getHashSetFilter().getFilterEntriesList().stream()
                .map(HashSetFilterEntry::fromProto)
                .collect(Collectors.toList());
        return new HashSetFilter(InventoryFilterType.fromProto(proto.getInventoryFilterType()), entries);
    }

    @Override
    public String getDetails() {
        return "HashSetFilter with " + filterEntries.size() + " filterEntries";
    }

    public Set<HashSetFilterEntry> getFilterEntriesAsSet() {
        return new HashSet<>(filterEntries);
    }
}