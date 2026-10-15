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

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.StorageService;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// See rules 9 and 10 of docs/specifications/network/inventory-hash-set-filter.md
class HashSetFilterServiceTest {
    @Test
    void limitsTheFilterOfALargeStoreToTheEntriesWhichReceiversAccept() {
        var storageService = mock(StorageService.class);
        var dataRequest = mock(DataRequest.class);
        List<Map.Entry<ByteArray, DataRequest>> storedRequests = IntStream.range(0, HashSetFilter.MAX_ENTRIES + 1)
                .mapToObj(i -> Map.entry(new ByteArray(hash(i)), dataRequest))
                .toList();
        when(storageService.getAllDataRequestMapEntries()).thenAnswer(invocation -> storedRequests.stream());
        var filterService = new HashSetFilterService(storageService, 2_000_000, InventoryFilterType.HASH_SET_V2);

        HashSetFilter filter = filterService.getFilter();

        assertThat(filter.getFilterEntries()).hasSize(HashSetFilter.MAX_ENTRIES);
    }

    @Test
    void acceptsAFilterWithTheMaximumNumberOfEntries() {
        assertThat(new HashSetFilter(InventoryFilterType.HASH_SET_V2, entries(HashSetFilter.MAX_ENTRIES))
                .getFilterEntries())
                .hasSize(HashSetFilter.MAX_ENTRIES);
        assertThatThrownBy(() -> new HashSetFilter(InventoryFilterType.HASH_SET_V2,
                entries(HashSetFilter.MAX_ENTRIES + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<HashSetFilterEntry> entries(int numEntries) {
        return IntStream.range(0, numEntries)
                .mapToObj(i -> new HashSetFilterEntry(hash(i), 1))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static byte[] hash(int i) {
        return ByteBuffer.allocate(20).putInt(i).array();
    }
}
