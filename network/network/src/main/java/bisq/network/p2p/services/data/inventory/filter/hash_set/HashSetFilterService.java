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
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.Sequential;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class HashSetFilterService extends FilterService<HashSetFilter> {
    // The sequence number in the entry of a request without a signed sequence number. The other stored requests have
    // positive sequence numbers.
    static final int NO_SIGNED_SEQUENCE_NUMBER = 0;

    // Fixed per instance, so that requests of both types can be served concurrently
    private final InventoryFilterType inventoryFilterType;

    public HashSetFilterService(StorageService storageService, int maxSize, InventoryFilterType inventoryFilterType) {
        super(storageService, maxSize);
        checkArgument(inventoryFilterType.isHashSet(), "Invalid inventoryFilterType %s", inventoryFilterType);
        this.inventoryFilterType = inventoryFilterType;
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
        return new HashSetFilter(inventoryFilterType, filterEntries);
    }

    @Override
    protected HashSetFilter safeCast(InventoryFilter inventoryFilter) {
        if (inventoryFilter instanceof HashSetFilter
                && inventoryFilter.getInventoryFilterType() == inventoryFilterType) {
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

    // Used for the filter which we send and for the comparison with a received filter, so both use the same key
    private HashSetFilterEntry toFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        byte[] hash = getStorageKey(mapEntry).getBytes();
        return new HashSetFilterEntry(hash, getSequenceNumber(mapEntry.getValue()));
    }

    // AddAppendOnlyDataRequest does not use a seq nr., therefore it is not Sequential. In HASH_SET_V2, a removal
    // without signature V2 has no signed sequence number either, because signature V1 does not cover it. A relay of an
    // earlier version removes signature V2 and keeps the number. The entry of that copy must differ from the entry of
    // the removal with signature V2, which replaces it (see docs/specifications/network/inventory-hash-set-filter.md).
    private int getSequenceNumber(DataRequest dataRequest) {
        boolean isRemovalWithoutSignatureV2 = dataRequest instanceof RemoveDataRequest &&
                dataRequest instanceof SignedRequest signedRequest &&
                !signedRequest.isSignatureV2Present();
        if (inventoryFilterType == InventoryFilterType.HASH_SET_V2 && isRemovalWithoutSignatureV2) {
            return NO_SIGNED_SEQUENCE_NUMBER;
        }
        return dataRequest instanceof Sequential sequential ? sequential.getSequenceNumber() : NO_SIGNED_SEQUENCE_NUMBER;
    }

    // The stores hold every request under storage key V2 (DataStorageService.preProcessPersisted re-keys persisted
    // entries), so the map key is storage key V2. Storage key V1 is derived from the request.
    private ByteArray getStorageKey(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        return switch (inventoryFilterType) {
            case HASH_SET -> mapEntry.getValue().getStorageKeyV1();
            case HASH_SET_V2 -> mapEntry.getKey();
            case MINI_SKETCH -> throw new IllegalStateException("Not a hash set filter type " + inventoryFilterType);
        };
    }
}
