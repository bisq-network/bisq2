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
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HashSetFilterService extends FilterService<HashSetFilter> {
    public HashSetFilterService(StorageService storageService, int maxSize) {
        super(storageService, maxSize);
    }

    public HashSetFilter getFilter() {
        List<HashSetFilterEntry> filterEntries = storageService.getAllDataRequestMapEntries()
                .map(this::toFilterEntry)
                .collect(Collectors.toList());
        if (filterEntries.size() > HashSetFilter.MAX_ENTRIES) {
            Collections.shuffle(filterEntries);
            filterEntries = filterEntries.stream().limit(HashSetFilter.MAX_ENTRIES).collect(Collectors.toList());
            log.warn("We limited the number of filter entries we send in our inventory request to {}",
                    HashSetFilter.MAX_ENTRIES);
        }
        return new HashSetFilter(filterEntries);
    }

    @Override
    protected HashSetFilter safeCast(InventoryFilter inventoryFilter) {
        if (inventoryFilter instanceof HashSetFilter
                && inventoryFilter.getInventoryFilterType() == InventoryFilterType.HASH_SET) {
            return (HashSetFilter) inventoryFilter;
        }
        throw new IllegalArgumentException("InventoryFilter not of expected type. inventoryFilter=" + inventoryFilter);
    }

    @Override
    protected boolean isAuthenticatedDataRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry) {
        return !filter.getFilterEntriesAsSet().contains(toFilterEntry(mapEntry));
    }

    @Override
    protected boolean isMailboxRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, MailboxRequest> mapEntry) {
        return !filter.getFilterEntriesAsSet().contains(toFilterEntry(mapEntry));
    }

    @Override
    protected boolean isAddAppendOnlyDataRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, AddAppendOnlyDataRequest> mapEntry) {
        return !filter.getFilterEntriesAsSet().contains(toFilterEntry(mapEntry));
    }

    private HashSetFilterEntry toFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        DataRequest dataRequest = mapEntry.getValue();
        int sequenceNumber = 0;
        byte[] hash = mapEntry.getKey().getBytes();
        if (dataRequest instanceof AddAppendOnlyDataRequest) {
            // AddAppendOnlyDataRequest does not use a seq nr.
            return new HashSetFilterEntry(hash, 0);
        } else if (dataRequest instanceof AddAuthenticatedDataRequest) {
            sequenceNumber = ((AddAuthenticatedDataRequest) dataRequest).getAuthenticatedSequentialData().getSequenceNumber();
        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest) {
            sequenceNumber = ((RemoveAuthenticatedDataRequest) dataRequest).getSequenceNumber();
        } else if (dataRequest instanceof RefreshAuthenticatedDataRequest) {
            sequenceNumber = ((RefreshAuthenticatedDataRequest) dataRequest).getSequenceNumber();
        } else if (dataRequest instanceof AddMailboxRequest) {
            sequenceNumber = ((AddMailboxRequest) dataRequest).getSequenceNumber();
        } else if (dataRequest instanceof RemoveMailboxRequest) {
            sequenceNumber = ((RemoveMailboxRequest) dataRequest).getSequenceNumber();
        }
        return new HashSetFilterEntry(hash, sequenceNumber);
    }
}
