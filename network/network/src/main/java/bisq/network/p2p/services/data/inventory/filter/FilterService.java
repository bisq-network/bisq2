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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public abstract class FilterService<T extends InventoryFilter> {
    protected final StorageService storageService;
    protected final int maxSize;

    public FilterService(StorageService storageService, int maxSize) {
        this.storageService = storageService;
        this.maxSize = maxSize;
    }

    abstract public T getFilter();

    abstract protected boolean isAuthenticatedDataRequestMissing(T filter, Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry);

    abstract protected boolean isMailboxRequestMissing(T filter, Map.Entry<ByteArray, MailboxRequest> mapEntry);

    abstract protected boolean isAddAppendOnlyDataRequestMissing(T filter, Map.Entry<ByteArray, AddAppendOnlyDataRequest> entry);

    public Inventory createInventory(InventoryFilter inventoryFilter, Predicate<Integer> predicate) {
        final AtomicInteger accumulatedSize = new AtomicInteger();
        final AtomicBoolean maxSizeReached = new AtomicBoolean();
        // The type is not defined at compile time, thus we do a safe cast
        T filter = safeCast(inventoryFilter);
        List<DataRequest> dataRequests = getAuthenticatedDataRequests(filter, accumulatedSize, maxSizeReached, predicate);

        if (!maxSizeReached.get()) {
            dataRequests.addAll(getMailboxRequests(filter, accumulatedSize, maxSizeReached, predicate));
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
                                                           AtomicBoolean maxSizeReached,
                                                           Predicate<Integer> predicate) {
        List<AddAuthenticatedDataRequest> addRequests = new ArrayList<>();
        List<RemoveAuthenticatedDataRequest> removeRequests = new ArrayList<>();
        storageService.getAuthenticatedDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (isAuthenticatedDataRequestMissing(filter, mapEntry)) {
                        AuthenticatedDataRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
                            DistributedData distributedData = addAuthenticatedDataRequest.getDistributedData();
                            if (predicate.test(distributedData.getVersion())) {
                                addRequests.add(addAuthenticatedDataRequest);
                            }
                        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeAuthenticatedDataRequest) {
                            if (predicate.test(removeAuthenticatedDataRequest.getVersion())) {
                                removeRequests.add(removeAuthenticatedDataRequest);
                            }
                        }
                        // Refresh is ignored
                    }
                });

        List<DataRequest> sortedAndFilteredRequests = addRequests.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getDistributedData().getMetaData().getPriority(),
                        o1.getDistributedData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());


        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(removeRequests.stream()
                    .sorted((o1, o2) -> Integer.compare(o2.getMetaData().getPriority(), o1.getMetaData().getPriority()))
                    .filter(request -> {
                        if (!maxSizeReached.get()) {
                            maxSizeReached.set(accumulatedSize.addAndGet(request.getSerializedSize()) > maxSize);
                        }
                        return !maxSizeReached.get();
                    })
                    .toList());
        }
        return sortedAndFilteredRequests;
    }


    private List<DataRequest> getMailboxRequests(T filter,
                                                 AtomicInteger accumulatedSize,
                                                 AtomicBoolean maxSizeReached,
                                                 Predicate<Integer> predicate) {
        List<AddMailboxRequest> addRequests = new ArrayList<>();
        List<RemoveMailboxRequest> removeRequests = new ArrayList<>();
        storageService.getMailboxStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (isMailboxRequestMissing(filter, mapEntry)) {
                        MailboxRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddMailboxRequest addMailboxRequest) {
                            if (predicate.test(addMailboxRequest.getMailboxSequentialData().getMailboxData().getVersion())) {
                                addRequests.add(addMailboxRequest);
                            }
                        } else if (dataRequest instanceof RemoveMailboxRequest removeMailboxRequest) {
                            if (predicate.test(removeMailboxRequest.getVersion())) {
                                removeRequests.add(removeMailboxRequest);
                            }
                        }
                    }
                });
        List<DataRequest> sortedAndFilteredRequests = addRequests.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getMailboxSequentialData().getMailboxData().getMetaData().getPriority(),
                        o1.getMailboxSequentialData().getMailboxData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());

        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(removeRequests.stream()
                    .sorted((o1, o2) -> Integer.compare(o2.getMetaData().getPriority(), o1.getMetaData().getPriority()))
                    .filter(request -> {
                        if (!maxSizeReached.get()) {
                            maxSizeReached.set(accumulatedSize.addAndGet(request.getSerializedSize()) > maxSize);
                        }
                        return !maxSizeReached.get();
                    })
                    .toList());
        }
        return sortedAndFilteredRequests;
    }

    private List<DataRequest> getAppendOnlyDataRequests(T filter,
                                                        AtomicInteger accumulatedSize,
                                                        AtomicBoolean maxSizeReached) {
        return storageService.getAddAppendOnlyDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .filter(entry -> isAddAppendOnlyDataRequestMissing(filter, entry))
                //hashSetFilter.getFilterEntries().contains(toFilterEntry(mapEntry)))
                .map(Map.Entry::getValue)
                .sorted((o1, o2) -> Integer.compare(o2.getAppendOnlyData().getMetaData().getPriority(),
                        o1.getAppendOnlyData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());
    }
}
