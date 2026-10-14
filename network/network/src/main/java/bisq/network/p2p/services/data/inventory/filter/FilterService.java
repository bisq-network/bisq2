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

package bisq.network.p2p.services.data.inventory.filter;

import bisq.common.data.ByteArray;
import bisq.common.data.ByteUnit;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class FilterService<T extends InventoryFilter> {
    protected final StorageService storageService;
    protected final int maxSize;

    public FilterService(StorageService storageService, int maxSize) {
        this.storageService = storageService;
        this.maxSize = maxSize;
    }

    abstract public T getFilter();

    // Whether the filter which this node would create now lists at least one of the requests. A responder does not
    // send a request which the filter of the requester lists.
    abstract public boolean isAnyRequestInFilter(Collection<? extends DataRequest> dataRequests);

    abstract protected boolean isAuthenticatedDataRequestMissing(T filter,
                                                                 Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry);

    abstract protected boolean isMailboxRequestMissing(T filter, Map.Entry<ByteArray, MailboxRequest> mapEntry);

    abstract protected boolean isAddAppendOnlyDataRequestMissing(T filter,
                                                                 Map.Entry<ByteArray, AddAppendOnlyDataRequest> entry);

    // The removals of authenticated data which the requester misses and which it can store. A filter type can exclude
    // removals which the requester cannot store, because its filter never lists them and every response would contain
    // them again.
    protected List<RemoveAuthenticatedDataRequest> getStorableRemovals(
            List<RemoveAuthenticatedDataRequest> missingRemovals) {
        return missingRemovals;
    }

    // The removals of mailbox data which the requester misses and which it can store, as getStorableRemovals for
    // authenticated data.
    protected List<RemoveMailboxRequest> getStorableMailboxRemovals(List<RemoveMailboxRequest> missingRemovals) {
        return missingRemovals;
    }

    public Inventory createInventory(InventoryFilter inventoryFilter) {
        final AtomicInteger accumulatedSize = new AtomicInteger();
        final AtomicBoolean maxSizeReached = new AtomicBoolean();
        // The type is not defined at compile time, thus we do a safe cast
        T filter = safeCast(inventoryFilter);
        List<DataRequest> dataRequests = getAuthenticatedDataRequests(filter, accumulatedSize, maxSizeReached);

        if (!maxSizeReached.get()) {
            dataRequests.addAll(getMailboxRequests(filter, accumulatedSize, maxSizeReached));
        }

        if (!maxSizeReached.get()) {
            dataRequests.addAll(getAppendOnlyDataRequests(filter, accumulatedSize, maxSizeReached));
        }

        log.info("Inventory with {} items and accumulatedSize of {} kb. maxSizeReached={}",
                dataRequests.size(), ByteUnit.BYTE.toKB(accumulatedSize.get()), maxSizeReached.get());
        return new Inventory(dataRequests, maxSizeReached.get());
    }

    abstract protected T safeCast(InventoryFilter inventoryFilter);

    private List<DataRequest> getAuthenticatedDataRequests(T filter,
                                                           AtomicInteger accumulatedSize,
                                                           AtomicBoolean maxSizeReached) {
        List<AddAuthenticatedDataRequest> addRequests = new ArrayList<>();
        List<RemoveAuthenticatedDataRequest> removeRequests = new ArrayList<>();
        storageService.getAuthenticatedDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (isAuthenticatedDataRequestMissing(filter, mapEntry)) {
                        AuthenticatedDataRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
                            DistributedData distributedData = addAuthenticatedDataRequest.getDistributedData();
                            addRequests.add(addAuthenticatedDataRequest);
                        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeAuthenticatedDataRequest) {
                            removeRequests.add(removeAuthenticatedDataRequest);
                        }
                        // Refresh is ignored
                    }
                });

        List<DataRequest> sortedAndFilteredRequests = selectWithinMaxSize(addRequests, accumulatedSize, maxSizeReached);
        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(selectWithinMaxSize(getStorableRemovals(removeRequests),
                    accumulatedSize,
                    maxSizeReached));
        }
        return sortedAndFilteredRequests;
    }


    private List<DataRequest> getMailboxRequests(T filter,
                                                 AtomicInteger accumulatedSize,
                                                 AtomicBoolean maxSizeReached) {
        List<AddMailboxRequest> addRequests = new ArrayList<>();
        List<RemoveMailboxRequest> removeRequests = new ArrayList<>();
        storageService.getMailboxStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (isMailboxRequestMissing(filter, mapEntry)) {
                        MailboxRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddMailboxRequest addMailboxRequest) {
                            addRequests.add(addMailboxRequest);
                        } else if (dataRequest instanceof RemoveMailboxRequest removeMailboxRequest) {
                            removeRequests.add(removeMailboxRequest);
                        }
                    }
                });
        List<DataRequest> sortedAndFilteredRequests = selectWithinMaxSize(addRequests, accumulatedSize, maxSizeReached);
        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(selectWithinMaxSize(getStorableMailboxRemovals(removeRequests),
                    accumulatedSize,
                    maxSizeReached));
        }
        return sortedAndFilteredRequests;
    }

    private List<DataRequest> getAppendOnlyDataRequests(T filter,
                                                        AtomicInteger accumulatedSize,
                                                        AtomicBoolean maxSizeReached) {
        List<AddAppendOnlyDataRequest> missingRequests = storageService.getAddAppendOnlyDataStoreMaps()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> isAddAppendOnlyDataRequestMissing(filter, entry))
                .map(Map.Entry::getValue)
                .toList();
        return selectWithinMaxSize(missingRequests, accumulatedSize, maxSizeReached);
    }

    // Selects the requests with the highest priority first, as long as the response stays within maxSize. At the first
    // request which does not fit, maxSizeReached is set, and no later request of any selection is added. A request which
    // is larger than maxSize fits into no response. It is skipped, so that it does not end every response of this node
    // before the requests after it (rule 13 of docs/specifications/network/inventory-hash-set-filter.md).
    private List<DataRequest> selectWithinMaxSize(Collection<? extends DataRequest> requests,
                                                  AtomicInteger accumulatedSize,
                                                  AtomicBoolean maxSizeReached) {
        List<? extends DataRequest> sortedRequests = requests.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()))
                .toList();
        List<DataRequest> selectedRequests = new ArrayList<>();
        int numRequestsLargerThanMaxSize = 0;
        for (DataRequest request : sortedRequests) {
            if (maxSizeReached.get()) {
                break;
            }
            int serializedSize = request.getSerializedSize();
            if (serializedSize > maxSize) {
                numRequestsLargerThanMaxSize++;
                continue;
            }
            maxSizeReached.set(accumulatedSize.addAndGet(serializedSize) > maxSize);
            if (!maxSizeReached.get()) {
                selectedRequests.add(request);
            }
        }
        if (numRequestsLargerThanMaxSize > 0) {
            log.warn("We skipped {} requests which are larger than the size limit of an inventory response ({} bytes)",
                    numRequestsLargerThanMaxSize, maxSize);
        }
        return selectedRequests;
    }
}
