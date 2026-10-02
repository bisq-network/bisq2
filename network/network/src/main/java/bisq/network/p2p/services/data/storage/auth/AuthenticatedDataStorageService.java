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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.application.DevMode;
import bisq.common.data.ByteArray;
import bisq.common.formatter.DataSizeFormatter;
import bisq.common.timer.Clock;
import bisq.common.timer.SystemClock;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AuthenticatedDataStorageService extends DataStorageService<AuthenticatedDataRequest> {
    // TODO rename with Handler as only used by StorageService (see https://github.com/bisq-network/bisq2/issues/3691)
    // publisherPubKeyHash is the hash of the key which published the data: the pubKeyHash of its add request. It
    // identifies the entry together with the data. The store accepts only the owner of the data as publisher, except
    // by the transition rule for authorized data (see docs/specifications/network/storage-ownership.md).
    public interface Listener {
        void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash);

        void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash);

        default void onRefreshed(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
        }
    }

    // TODO Use a field for a single handler as only one listener is used by StorageService
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object mapAccessLock = new Object();

    public AuthenticatedDataStorageService(PersistenceService persistenceService,
                                           PruneExpiredEntriesService pruneExpiredEntriesService,
                                           String storeName,
                                           String storeKey) {
        this(persistenceService, pruneExpiredEntriesService, storeName, storeKey, new SystemClock());
    }

    @VisibleForTesting
    public AuthenticatedDataStorageService(PersistenceService persistenceService,
                                           PruneExpiredEntriesService pruneExpiredEntriesService,
                                           String storeName,
                                           String storeKey,
                                           Clock clock) {
        super(persistenceService, storeName, storeKey, clock);
        pruneExpiredEntriesService.addTask(this::pruneExpired);
    }

    @Override
    public void onPersistedApplied(DataStore<AuthenticatedDataRequest> persisted) {
        maybeLogMapState("onPersistedApplied", persisted);
        handlePersistedPublishDateAware(persisted);
    }

    @Override
    public void shutdown() {
        maybeLogMapState("shutdown", persistableStore);
        super.shutdown();
        listeners.clear();
    }

    public DataStorageResult add(AddAuthenticatedDataRequest request) {
        maybeLogMapState("add", persistableStore);

        if (isClassNameInvalid(request.getDistributedData().getClass().getSimpleName())) {
            return DataStorageResult.failed().classNameInvalid();
        }

        long now = clock.now();
        if (request.isRequiredSignatureV2Missing(now)) {
            log.debug("Signature V2 is missing at add. storageKey={}", request.getStorageKey());
            return DataStorageResult.failed().signatureV2Missing();
        }

        // Most received add requests are copies of stored requests (a broadcast arrives from several peers).
        // Detect them before the signature verification, which is the most expensive check. A copy without signature
        // V2, which a node of an earlier version relayed, is not equal to the stored request, but its sequence number is
        // not higher. The checks are repeated inside the lock because the map can change in between.
        ByteArray storageKey = request.getStorageKey();
        AuthenticatedDataRequest storedRequest = persistableStore.getMap().get(storageKey);
        if (request.equals(storedRequest)) {
            return DataStorageResult.failed().requestAlreadyReceived();
        }
        if (isSequenceNumberSigned(storedRequest) && request.isSequenceNrInvalid(storedRequest.getSequenceNumber())) {
            return DataStorageResult.failed().sequenceNrInvalid();
        }

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        AuthenticatedSequentialData authenticatedSequentialData = request.getAuthenticatedSequentialData();
        AuthenticatedData authenticatedData = authenticatedSequentialData.getAuthenticatedData();
        if (authenticatedData instanceof AuthorizedData authorizedData && authorizedData.isNotAuthorized()) {
            log.warn("AuthorizedData is not authorized. request={}", StringUtils.truncate(request.toString(), 1500));
            return DataStorageResult.failed().isNotAuthorized();
        }
        if (request.isSignatureInvalid(now)) {
            log.warn("Signature is invalid at add. request={}", request);
            return DataStorageResult.failed().signatureInvalid();
        }

        AuthenticatedDataRequest requestFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            requestFromMap = map.get(storageKey);

            if (requestFromMap == null && isExceedingMapSize()) {
                return DataStorageResult.failed().maxMapSizeReached();
            }

            if (request.equals(requestFromMap)) {
                return DataStorageResult.failed().requestAlreadyReceived();
            }

            if (isSequenceNumberSigned(requestFromMap) && request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (authenticatedData.getDistributedData() instanceof PublishDateAware publishDateAware) {
                publishDateAware.setPublishDate(request.getCreated());
            }

            map.put(storageKey, request);

            // In case we only updated the seq number we still want to broadcast and update the listeners.
            // It is a valid use case that we have both an add and remove data request, and we get repeated sequences
            // of add/remove events. In that case only the sequence number changes, but we still want to notify our
            // listeners.
        }

        persist();

        listeners.forEach(listener -> {
            try {
                listener.onAdded(authenticatedData, authenticatedSequentialData.getPubKeyHash());
            } catch (Exception e) {
                log.error("Calling onAdded at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("add success", persistableStore);
        return DataStorageResult.success();
    }

    public DataStorageResult remove(RemoveAuthenticatedDataRequest request) {
        maybeLogMapState("remove ", persistableStore);

        if (isClassNameInvalid(request.getClassName())) {
            return DataStorageResult.failed().classNameInvalid();
        }

        long now = clock.now();
        if (request.isRequiredSignatureV2Missing(now)) {
            log.debug("Signature V2 is missing at remove. storageKey={}", request.getStorageKey());
            return DataStorageResult.failed().signatureV2Missing();
        }

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        if (request.isSignatureInvalid(now)) {
            log.warn("Signature is invalid at remove. request={}", request);
            return DataStorageResult.failed().signatureInvalid();
        }

        ByteArray storageKey = request.getStorageKey();
        AuthenticatedData authenticatedDataFromMap;
        byte[] publisherPubKeyHash;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(storageKey);
            if (requestFromMap == null) {
                log.debug("No entry at remove. hash={}", storageKey);

                if (isExceedingMapSize()) {
                    return DataStorageResult.failed().maxMapSizeReached();
                }
                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(storageKey, request);
                persist();
                return DataStorageResult.success().noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                // A verified V2 removal supersedes an untrusted V1 sequence. Once upgraded, keep only
                // higher signed sequences so a V1 request cannot discard the trusted ordering information.
                if (request.isSignatureV2Present() &&
                        (!requestFromMap.isSignatureV2Present() || !request.isSequenceNrInvalid(requestFromMap.getSequenceNumber()))) {
                    // We update the map with the new request with the fresh sequence number.
                    map.put(storageKey, request);
                    persist();
                    return DataStorageResult.success().alreadyRemoved();
                }
                return DataStorageResult.failed().alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddProtectedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;

            // We have an entry, lets validate if we can remove it
            AuthenticatedSequentialData dataFromMap = addRequestFromMap.getAuthenticatedSequentialData();
            authenticatedDataFromMap = dataFromMap.getAuthenticatedData();
            publisherPubKeyHash = dataFromMap.getPubKeyHash();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr has not increased at remove. request={}", request);
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (request.isSignaturePubKeyHashInvalid(dataFromMap.getPubKeyHash())) {
                log.warn("PublicKey hash is invalid at remove. request={}", request);
                return DataStorageResult.failed().publicKeyHashInvalid();
            }

            map.put(storageKey, request);
        }

        persist();

        listeners.forEach(listener -> {
            try {
                listener.onRemoved(authenticatedDataFromMap, publisherPubKeyHash);
            } catch (Exception e) {
                log.error("Calling onRemoved at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("remove success", persistableStore);
        return DataStorageResult.success();
    }

    public DataStorageResult refresh(RefreshAuthenticatedDataRequest refreshRequest) {
        maybeLogMapState("refresh ", persistableStore);

        if (isClassNameInvalid(refreshRequest.getClassName())) {
            return DataStorageResult.failed().classNameInvalid();
        }

        // A refresh request needs signature V2, also before the cutoff date of signature V1. Nodes of earlier versions
        // sign only the hash of the data, which covers neither the sequence number nor the creation date (see
        // docs/specifications/network/storage-request-signatures.md).
        if (!refreshRequest.isSignatureV2Present()) {
            log.debug("Signature V2 is missing at refresh. storageKey={}", refreshRequest.getStorageKey());
            return DataStorageResult.failed().signatureV2Missing();
        }

        if (refreshRequest.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        ByteArray storageKey = refreshRequest.getStorageKey();
        AddAuthenticatedDataRequest updatedAddRequest;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(storageKey);

            if (requestFromMap == null) {
                return DataStorageResult.failed().noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                return DataStorageResult.failed().alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddAuthenticatedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;
            AuthenticatedSequentialData sequentialDataFromMap = addRequestFromMap.getAuthenticatedSequentialData();
            if (refreshRequest.isSequenceNrInvalid(sequentialDataFromMap.getSequenceNumber())) {
                // The sequence number has not increased. This is the normal case for the copies of a refresh request
                // which arrive from other peers after the node applied it.
                log.debug("SequenceNr is invalid at refresh. storageKey={}", storageKey);
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (refreshRequest.isSignaturePubKeyHashInvalid(sequentialDataFromMap.getPubKeyHash())) {
                log.warn("PublicKey is invalid at refresh. request={}", refreshRequest);
                // Hash of pubKey of data does not match provided one
                return DataStorageResult.failed().publicKeyHashInvalid();
            }

            // The refresh changes the creation date, on which the transition rule for the owner of authorized data
            // depends (see docs/specifications/network/storage-ownership.md). The updated request could not be created
            // for another owner.
            if (AuthenticatedSequentialData.isOwnerInvalid(sequentialDataFromMap.getAuthenticatedData(),
                    sequentialDataFromMap.getPubKeyHash(),
                    refreshRequest.getCreated())) {
                log.warn("The publisher is not the owner of the data at refresh. request={}", refreshRequest);
                return DataStorageResult.failed().ownerInvalid();
            }

            // We use the creation date, sequence number and signatures from our RefreshAuthenticatedDataRequest
            // and the rest from our existing AddAuthenticatedDataRequest to create an updated AddAuthenticatedDataRequest.
            // The signature of the RefreshAuthenticatedDataRequest is created with the full data of an
            // AddAuthenticatedDataRequest at the owner's side, thus the signature will match the new object.
            updatedAddRequest = AddAuthenticatedDataRequest.from(addRequestFromMap, refreshRequest);
            if (updatedAddRequest.isSignatureV2Invalid()) {
                log.warn("Signature is invalid at refresh. request={}", refreshRequest);
                return DataStorageResult.failed().signatureInvalid();
            }

            if (sequentialDataFromMap.getDistributedData() instanceof PublishDateAware publishDateAware) {
                publishDateAware.setPublishDate(updatedAddRequest.getCreated());
            }

            map.put(storageKey, updatedAddRequest);
        }

        persist();

        AuthenticatedData authenticatedData = updatedAddRequest.getAuthenticatedSequentialData().getAuthenticatedData();
        listeners.forEach(listener -> {
            try {
                listener.onRefreshed(authenticatedData, updatedAddRequest.getAuthenticatedSequentialData().getPubKeyHash());
            } catch (Exception e) {
                log.error("Calling onRefreshed at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("refresh success", persistableStore);
        return DataStorageResult.success();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // The sequence number for the next request of the owner. A relay can change the sequence number of a removal without
    // signature V2, so it is not used. Such a removal does not reject a request of any sequence number (see
    // docs/specifications/network/storage-request-ordering.md).
    public int getNextSequenceNumber(ByteArray storageKey) {
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(storageKey);
            if (!isSequenceNumberSigned(requestFromMap)) {
                return 1;
            }
            return Math.incrementExact(requestFromMap.getSequenceNumber());
        }
    }

    // Both signature versions of an add request cover its sequence number. Only signature V2 of a removal covers it.
    private static boolean isSequenceNumberSigned(AuthenticatedDataRequest requestFromMap) {
        boolean isAdd = requestFromMap instanceof AddAuthenticatedDataRequest;
        boolean isRemoveWithSignatureV2 = requestFromMap instanceof RemoveAuthenticatedDataRequest &&
                requestFromMap.isSignatureV2Present();
        return isAdd || isRemoveWithSignatureV2;
    }

    // Removes the entries which have expired, and those without signature V2 when the node requires it (see isToPrune).
    // A request can replace an entry after it was selected, so an entry is removed only if the map still holds it.
    private void pruneExpired() {
        Set<Map.Entry<ByteArray, AuthenticatedDataRequest>> expiredEntries = persistableStore.getMap().entrySet().stream()
                .filter(entry -> isToPrune(entry.getValue()))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
        if (!expiredEntries.isEmpty()) {
            log.info("We remove {} entries from our {} map which have expired or have no signature V2",
                    expiredEntries.size(), getStoreKey());
            expiredEntries.forEach(entry -> {
                if (!persistableStore.getMap().remove(entry.getKey(), entry.getValue())) {
                    return;
                }
                prunedAndExpiredDataRequests.add(entry.getValue());
                if (entry.getValue() instanceof AddAuthenticatedDataRequest addRequest) {
                    AuthenticatedSequentialData sequentialData = addRequest.getAuthenticatedSequentialData();
                    AuthenticatedData data = sequentialData.getAuthenticatedData();
                    listeners.forEach(listener -> {
                        try {
                            listener.onRemoved(data, sequentialData.getPubKeyHash());
                        } catch (Exception e) {
                            log.error("Calling onRemoved at listener {} failed", listener, e);
                        }
                    });
                }
            });
        }
    }

    private void handlePersistedPublishDateAware(DataStore<AuthenticatedDataRequest> persisted) {
        persisted.getMap().values().forEach(authenticatedDataRequest -> {
            // The map holds no RefreshAuthenticatedDataRequest: a refresh replaces the stored
            // AddAuthenticatedDataRequest.
            if (authenticatedDataRequest instanceof AddAuthenticatedDataRequest request) {
                DistributedData distributedData = request.getDistributedData();
                if (distributedData instanceof PublishDateAware publishDateAware) {
                    publishDateAware.setPublishDate(authenticatedDataRequest.getCreated());
                }
            }
        });
    }

    // Useful for debugging state of the store
    private void maybeLogMapState(String methodName, DataStore<AuthenticatedDataRequest> dataStore) {
        if (DevMode.isDevMode() || methodName.equals("onPersistedApplied")) {
            var dataSize = dataStore.getMap().values().stream()
                    .mapToLong(authenticatedDataRequest -> authenticatedDataRequest.serializeForHash().length)
                    .sum();
            var added = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof AddAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (AddAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(e -> e.getDistributedData().getClass().getSimpleName())
                    .collect(Collectors.toList());
            var removed = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof RemoveAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (RemoveAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(RemoveAuthenticatedDataRequest::getClassName)
                    .collect(Collectors.toList());
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
                        .filter(AddAuthenticatedDataRequest.class::isInstance)
                        .map(AddAuthenticatedDataRequest.class::cast)
                        .collect(Collectors.groupingBy(
                                e -> TimeUnit.MILLISECONDS.toDays(now - e.getCreated()),
                                TreeMap::new, // sorted by age in days
                                Collectors.counting()
                        ))
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + " days old: " + entry.getValue())
                        .collect(Collectors.joining("\n"));

                String summaryRemoved = dataStore.getMap().values().stream()
                        .filter(RemoveAuthenticatedDataRequest.class::isInstance)
                        .map(RemoveAuthenticatedDataRequest.class::cast)
                        .collect(Collectors.groupingBy(
                                e -> TimeUnit.MILLISECONDS.toDays(now - e.getCreated()),
                                TreeMap::new, // sorted by age in days
                                Collectors.counting()
                        ))
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + " days old: " + entry.getValue())
                        .collect(Collectors.joining("\n"));


                log.info("AddAuthenticatedDataRequests summaryAdded: {}\n{}", storeKey, summaryAdded);
                log.info("RemoveAuthenticatedDataRequests summaryRemoved: {}\n{}", storeKey, summaryRemoved);
            }
        }
    }
}
