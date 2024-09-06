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
import bisq.network.p2p.services.data.storage.*;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MailboxDataStorageService extends DataStorageService<MailboxRequest> {
    public interface Listener {
        void onAdded(MailboxData mailboxData);

        void onRemoved(MailboxData mailboxData);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object mapAccessLock = new Object();

    public MailboxDataStorageService(PersistenceService persistenceService, PruneExpiredEntriesService pruneExpiredEntriesService, String storeName, String storeKey) {
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
    }

    public DataStorageResult add(AddMailboxRequest request) {
        maybeLogMapState("add", persistableStore);
        MailboxSequentialData mailboxSequentialData = request.getMailboxSequentialData();
        MailboxData mailboxData = mailboxSequentialData.getMailboxData();
        byte[] hash = DigestUtil.hash(mailboxData.serializeForHash());
        ByteArray byteArray = new ByteArray(hash);
        MailboxRequest requestFromMap;
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (isExceedingMapSize()) {
                return new DataStorageResult(false).maxMapSizeReached();
            }

            requestFromMap = map.get(byteArray);
            if (request.equals(requestFromMap)) {
                return new DataStorageResult(false).requestAlreadyReceived();
            }

            int sequenceNumberFromMap = requestFromMap != null ? requestFromMap.getSequenceNumber() : 0;
            if (requestFromMap != null && mailboxSequentialData.isSequenceNrInvalid(sequenceNumberFromMap)) {
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (mailboxSequentialData.isExpired()) {
                return new DataStorageResult(false).expired();
            }

            if (mailboxData.isDataInvalid(mailboxSequentialData.getSenderPublicKeyHash())) {
                return new DataStorageResult(false).dataInvalid();
            }

            if (request.isPublicKeyInvalid()) {
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                return new DataStorageResult(false).signatureInvalid();
            }
            map.put(byteArray, request);
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
        return new DataStorageResult(true);
    }


    public DataStorageResult remove(RemoveMailboxRequest request) {
        maybeLogMapState("remove ", persistableStore);
        ByteArray byteArray = new ByteArray(request.getHash());
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        MailboxRequest requestFromMap = map.get(byteArray);
        MailboxSequentialData sequentialDataFromMap;
        synchronized (mapAccessLock) {
            if (requestFromMap == null) {
                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(byteArray, request);
                persist();
                return new DataStorageResult(true).noEntry();
            }

            if (requestFromMap instanceof RemoveMailboxRequest) {
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the request, so we have the latest sequence number.
                    map.put(byteArray, request);
                    persist();
                }
                return new DataStorageResult(true).alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddMailboxRequest
            AddMailboxRequest addRequest = (AddMailboxRequest) requestFromMap;
            // We have an entry, lets validate if we can remove it
            sequentialDataFromMap = addRequest.getMailboxSequentialData();
            if (request.isSequenceNrInvalid(sequentialDataFromMap.getSequenceNumber())) {
                // Sequence number has not increased
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyHashInvalid(sequentialDataFromMap)) {
                // Hash of pubKey of data does not match provided one
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                return new DataStorageResult(false).signatureInvalid();
            }

            // As metaData from mailboxData is taken from the users current code base but the one from RemoveMailboxRequest
            // is from the senders version (taken from senders mailboxData) it could be different if both users had
            // different versions and metaData has changed between those versions.
            // If we detect such a difference we use our metaData version. This also protects against malicious manipulation.
            MetaData metaDataFromMailboxData = sequentialDataFromMap.getMailboxData().getMetaData();
            if (!request.getMetaDataFromProto().equals(metaDataFromMailboxData)) {
                request.setMetaDataFromDistributedData(Optional.of(metaDataFromMailboxData));
                log.warn("MetaData of remove request not matching the one from the addRequest from the map. We override " +
                                "metadata with the one we have from the associated mailbox data." +
                                "{} vs. {}",
                        request.getMetaDataFromProto(),
                        metaDataFromMailboxData);
            }

            map.put(byteArray, request);
            listeners.forEach(listener -> {
                try {
                    listener.onRemoved(sequentialDataFromMap.getMailboxData());
                } catch (Exception e) {
                    log.error("Calling onRemoved at listener {} failed", listener, e);
                }
            });
        }

        persist();
        maybeLogMapState("remove success", persistableStore);
        return new DataStorageResult(true).removedData(sequentialDataFromMap.getMailboxData());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    int getSequenceNumber(byte[] hash) {
        ByteArray byteArray = new ByteArray(hash);
        int sequenceNumber = 0;
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (map.containsKey(byteArray)) {
                sequenceNumber = map.get(byteArray).getSequenceNumber();
            }
        }
        return sequenceNumber;
    }

    boolean contains(byte[] hash) {
        Map<ByteArray, MailboxRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            return map.containsKey(new ByteArray(hash));
        }
    }

    boolean canAddMailboxMessage(MailboxData mailboxData) {
        byte[] hash = DigestUtil.hash(mailboxData.serializeForHash());
        return getSequenceNumber(hash) < Integer.MAX_VALUE;
    }

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
    private void maybeLogMapState(String methodName, DataStore<MailboxRequest> persisted) {
        if (DevMode.isDevMode() || methodName.equals("onPersistedApplied")) {
            var added = persisted.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof AddMailboxRequest)
                    .map(authenticatedDataRequest -> (AddMailboxRequest) authenticatedDataRequest)
                    .map(e -> e.getMailboxSequentialData().getMailboxData().getClassName())
                    .toList();
            var removed = persisted.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof RemoveMailboxRequest)
                    .map(authenticatedDataRequest -> (RemoveMailboxRequest) authenticatedDataRequest)
                    .map(RemoveMailboxRequest::getClassName)
                    .toList();
            var className = Stream.concat(added.stream(), removed.stream())
                    .findAny().orElse(persistence.getFileName().replace("Store", ""));
            log.info("Method: {}; map entry: {}; num AddRequests: {}; num RemoveRequests={}; map size:{}",
                    methodName, className, added.size(), removed.size(), persisted.getMap().size());
        }
    }
}
