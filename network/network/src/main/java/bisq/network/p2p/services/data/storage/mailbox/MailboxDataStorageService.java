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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.common.application.DevMode;
import bisq.common.data.ByteArray;
import bisq.common.formatter.DataSizeFormatter;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.persistence.PersistenceService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MailboxDataStorageService extends DataStorageService<MailboxRequest> {
    // TODO rename with Handler as only used by StorageService (see https://github.com/bisq-network/bisq2/issues/3691)
    public interface Listener {
        void onAdded(MailboxData mailboxData);

        void onRemoved(MailboxData mailboxData);
    }

    // TODO Use a field for a single handler as only one listener is used by StorageService
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object mapAccessLock = new Object();

    public MailboxDataStorageService(PersistenceService persistenceService,
                                     PruneExpiredEntriesService pruneExpiredEntriesService,
                                     String storeName,
                                     String storeKey) {
        super(persistenceService, storeName, storeKey);
        pruneExpiredEntriesService.addTask(this::pruneExpired);
    }

    @Override
    public void onPersistedApplied(DataStore<MailboxRequest> persisted) {
        maybeLogMapState("onPersistedApplied", persisted);
    }

    @Override
    public void shutdown() {
        maybeLogMapState("shutdown", persistableStore);
        super.shutdown();
        listeners.clear();
    }

    public DataStorageResult add(AddMailboxRequest request) {
        maybeLogMapState("add", persistableStore);

        // The message is encrypted, so the class name comes from MailboxData
        if (isClassNameInvalid(request.getClassName())) {
            return DataStorageResult.failed().classNameInvalid();
        }

        // Most received add requests are copies of stored requests (a broadcast arrives from several peers).
        // Detect them before the signature verification, which is the most expensive check. The check is
        // repeated inside the lock because the map can change in between.
        ByteArray storageKey = request.getStorageKey();
        if (request.equals(persistableStore.getMap().get(storageKey))) {
            return DataStorageResult.failed().requestAlreadyReceived();
        }

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        MailboxSequentialData mailboxSequentialData = request.getMailboxSequentialData();
        MailboxData mailboxData = mailboxSequentialData.getMailboxData();

        if (mailboxSequentialData.isSenderInvalid()) {
            return DataStorageResult.failed().ownerInvalid();
        }
        if (mailboxData.isDataInvalid(mailboxSequentialData.getSenderPublicKeyHash())) {
            return DataStorageResult.failed().dataInvalid();
        }
        if (request.isSignaturePubKeyHashInvalid(mailboxSequentialData.getSenderPublicKeyHash())) {
            return DataStorageResult.failed().publicKeyHashInvalid();
        }
        if (request.isSignatureInvalid()) {
            return DataStorageResult.failed().signatureInvalid();
        }

        MailboxRequest requestFromMap;
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            requestFromMap = map.get(storageKey);
            if (requestFromMap == null && isExceedingMapSize()) {
                return DataStorageResult.failed().maxMapSizeReached();
            }

            if (request.equals(requestFromMap)) {
                return DataStorageResult.failed().requestAlreadyReceived();
            }

            if (requestFromMap != null && request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            map.put(storageKey, request);
        }

        persist();

        listeners.forEach(listener -> {
            try {
                listener.onAdded(mailboxData);
            } catch (Exception e) {
                log.error("Calling onAdded at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("add success", persistableStore);
        return DataStorageResult.success();
    }


    public DataStorageResult remove(RemoveMailboxRequest request) {
        maybeLogMapState("remove ", persistableStore);

        if (isClassNameInvalid(request.getClassName())) {
            return DataStorageResult.failed().classNameInvalid();
        }

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }
        if (request.isSignatureInvalid()) {
            return DataStorageResult.failed().signatureInvalid();
        }

        ByteArray storageKey = request.getStorageKey();
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        MailboxSequentialData sequentialDataFromMap;
        synchronized (mapAccessLock) {
            MailboxRequest requestFromMap = map.get(storageKey);
            if (requestFromMap == null) {
                if (isExceedingMapSize()) {
                    return DataStorageResult.failed().maxMapSizeReached();
                }

                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(storageKey, request);
                persist();
                return DataStorageResult.success().noEntry();
            }

            if (requestFromMap instanceof RemoveMailboxRequest) {
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the request, so we have the latest sequence number.
                    map.put(storageKey, request);
                    persist();
                }
                return DataStorageResult.success().alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddMailboxRequest
            AddMailboxRequest addRequest = (AddMailboxRequest) requestFromMap;
            // We have an entry, lets validate if we can remove it
            sequentialDataFromMap = addRequest.getMailboxSequentialData();
            if (request.isSequenceNrInvalid(sequentialDataFromMap.getSequenceNumber())) {
                // Sequence number has not increased
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (request.isSignaturePubKeyHashInvalid(sequentialDataFromMap.getReceiversPublicKeyHash())) {
                // Hash of pubKey of data does not match provided one
                return DataStorageResult.failed().publicKeyHashInvalid();
            }

            map.put(storageKey, request);
        }

        persist();

        listeners.forEach(listener -> {
            try {
                listener.onRemoved(sequentialDataFromMap.getMailboxData());
            } catch (Exception e) {
                log.error("Calling onRemoved at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("remove success", persistableStore);
        return DataStorageResult.success().removedData(sequentialDataFromMap.getMailboxData());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void pruneExpired() {
        Set<Map.Entry<ByteArray, MailboxRequest>> expiredEntries = persistableStore.getMap().entrySet().stream()
                .filter(entry -> {
                    MailboxRequest dataRequest = entry.getValue();
                    boolean isExpired = dataRequest.isExpired();
                    if (isExpired) {
                        prunedAndExpiredDataRequests.add(dataRequest);
                    }
                    return isExpired;
                })
                .collect(Collectors.toSet());
        if (!expiredEntries.isEmpty()) {
            log.info("We remove {} expired entries from our {} map", expiredEntries.size(), getStoreKey());
            expiredEntries.forEach(entry -> persistableStore.getMap().remove(entry.getKey()));
        }
    }

    // Useful for debugging state of the store
    private void maybeLogMapState(String methodName, DataStore<MailboxRequest> dataStore) {
        if (DevMode.isDevMode() || methodName.equals("onPersistedApplied")) {
            var dataSize = dataStore.getMap().values().stream()
                    .mapToLong(authenticatedDataRequest -> authenticatedDataRequest.serializeForHash().length)
                    .sum();
            var added = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof AddMailboxRequest)
                    .map(authenticatedDataRequest -> (AddMailboxRequest) authenticatedDataRequest)
                    .map(e -> e.getMailboxSequentialData().getMailboxData().getClassName())
                    .toList();
            var removed = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof RemoveMailboxRequest)
                    .map(authenticatedDataRequest -> (RemoveMailboxRequest) authenticatedDataRequest)
                    .map(RemoveMailboxRequest::getClassName)
                    .toList();
            log.info("Method: {}; map entry: {}; num AddRequests: {}; num RemoveRequests={}; map size:{}, data size: {}, Max size: {}",
                    methodName,
                    storeKey,
                    added.size(),
                    removed.size(),
                    dataStore.getMap().size(),
                    DataSizeFormatter.format(dataSize),
                    MAX_MAP_SIZE);

            boolean showDetails = false;
            if (showDetails) {
                long now = System.currentTimeMillis();
                String summaryAdded = dataStore.getMap().values().stream()
                        .filter(AddMailboxRequest.class::isInstance)
                        .map(AddMailboxRequest.class::cast)
                        .collect(Collectors.groupingBy(
                                e -> TimeUnit.MILLISECONDS.toDays(now - e.getCreated()),
                                TreeMap::new, // sorted by age in days
                                Collectors.counting()
                        ))
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + " days old: " + entry.getValue())
                        .collect(Collectors.joining("\n"));

                String summaryRemoved = dataStore.getMap().values().stream()
                        .filter(RemoveMailboxRequest.class::isInstance)
                        .map(RemoveMailboxRequest.class::cast)
                        .collect(Collectors.groupingBy(
                                e -> TimeUnit.MILLISECONDS.toDays(now - e.getCreated()),
                                TreeMap::new, // sorted by age in days
                                Collectors.counting()
                        ))
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + " days old: " + entry.getValue())
                        .collect(Collectors.joining("\n"));


                log.info("AddMailboxRequest: {}\n{}", storeKey, summaryAdded);
                log.info("RemoveMailboxRequest: {}\n{}", storeKey, summaryRemoved);
            }
        }
    }
}
