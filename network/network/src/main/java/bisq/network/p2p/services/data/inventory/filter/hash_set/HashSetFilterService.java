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
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import lombok.extern.slf4j.Slf4j;

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
            // AddMailboxRequest extends AddAuthenticatedDataRequest so its covered here as well
            sequenceNumber = ((AddAuthenticatedDataRequest) dataRequest).getAuthenticatedSequentialData().getSequenceNumber();
        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest) {
            // RemoveMailboxRequest extends RemoveAuthenticatedDataRequest so its covered here as well
            sequenceNumber = ((RemoveAuthenticatedDataRequest) dataRequest).getSequenceNumber();
        }
        return new HashSetFilterEntry(hash, sequenceNumber);
    }
}
