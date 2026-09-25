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
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AuthenticatedDataStorageService extends DataStorageService<AuthenticatedDataRequest> {
    // TODO rename with Handler as only used by StorageService (see https://github.com/bisq-network/bisq2/issues/3691)
    public interface Listener {
        void onAdded(AuthenticatedData authenticatedData);

        void onRemoved(AuthenticatedData authenticatedData);

        default void onRefreshed(AuthenticatedData authenticatedData) {
        }
    }

    // TODO Use a field for a single handler as only one listener is used by StorageService
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object mapAccessLock = new Object();

    public AuthenticatedDataStorageService(PersistenceService persistenceService,
                                           PruneExpiredEntriesService pruneExpiredEntriesService,
                                           String storeName,
                                           String storeKey) {
        super(persistenceService, storeName, storeKey);
        pruneExpiredEntriesService.addTask(this::pruneExpired);
    }

    @Override
    public void onPersistedApplied(DataStore<AuthenticatedDataRequest> persisted) {
        maybeLogMapState("onPersistedApplied", persisted);
        pruneInvalidAuthorizedData();
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

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        AuthenticatedSequentialData authenticatedSequentialData = request.getAuthenticatedSequentialData();
        AuthenticatedData authenticatedData = authenticatedSequentialData.getAuthenticatedData();
        if (authenticatedData.isDataInvalid(authenticatedSequentialData.getPubKeyHash())) {
            log.warn("AuthenticatedData is invalid at add. request={}", request);
            return DataStorageResult.failed().dataInvalid();
        }
        if (authenticatedData instanceof AuthorizedData authorizedData && authorizedData.isNotAuthorized()) {
            log.warn("AuthorizedData is not authorized. request={}", StringUtils.truncate(request.toString(), 1500));
            return DataStorageResult.failed().isNotAuthorized();
        }
        if (request.isSignaturePubKeyHashInvalid(authenticatedSequentialData.getPubKeyHash())) {
            log.warn("PublicKey is invalid at add. request={}", request);
            return DataStorageResult.failed().publicKeyHashInvalid();
        }
        if (request.isSignatureInvalid()) {
            log.warn("Signature is invalid at add. request={}", request);
            return DataStorageResult.failed().signatureInvalid();
        }

        ByteArray storageKey = request.getStorageKey();
        AuthenticatedDataRequest requestFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (isExceedingMapSize()) {
                return DataStorageResult.failed().maxMapSizeReached();
            }

            requestFromMap = map.get(storageKey);
            if (request.equals(requestFromMap)) {
                return DataStorageResult.failed().requestAlreadyReceived();
            }

            if (requestFromMap != null && request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
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
                listener.onAdded(authenticatedData);
            } catch (Exception e) {
                log.error("Calling onAdded at listener {} failed", listener, e);
            }
        });

        maybeLogMapState("add success", persistableStore);
        return DataStorageResult.success();
    }

    public DataStorageResult remove(RemoveAuthenticatedDataRequest request) {
        maybeLogMapState("remove ", persistableStore);
        ByteArray storageKey = request.getStorageKey();
        AuthenticatedData authenticatedDataFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(storageKey);
            if (requestFromMap == null) {
                log.debug("No entry at remove. hash={}", storageKey);
                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(storageKey, request);
                persist();
                return DataStorageResult.success().noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                // log.debug("Already removed. request={}, map={}", request, map);
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the map with the new request with the fresh sequence number.
                    map.put(storageKey, request);
                    persist();
                }
                return DataStorageResult.success().alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddProtectedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;

            // We have an entry, lets validate if we can remove it
            AuthenticatedSequentialData dataFromMap = addRequestFromMap.getAuthenticatedSequentialData();
            authenticatedDataFromMap = dataFromMap.getAuthenticatedData();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr has not increased at remove. request={}", request);
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (request.isSignaturePubKeyHashInvalid(dataFromMap.getPubKeyHash())) {
                log.warn("PublicKey hash is invalid at remove. request={}", request);
                return DataStorageResult.failed().publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at remove. request={}", request);
                return DataStorageResult.failed().signatureInvalid();
            }

            // As metaData from distributedData is taken from the users current code base but the one from RemoveAuthenticatedDataRequest
            // is from the senders version (taken from senders distributedData) it could be different if both users had
            // different versions and metaData has changed between those versions.
            // If we detect such a difference we use our metaData version. This also protects against malicious manipulation.
            MetaData metaDataFromDistributedData = addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData();
            if (!request.getFallbackMetaData().equals(metaDataFromDistributedData)) {
                request.setMetaDataFromDistributedData(Optional.of(metaDataFromDistributedData));
                log.warn("MetaData of remove request not matching the one from the addRequest from the map. We override " +
                                "metadata with the one we have from the associated distributed data." +
                                "{} vs. {}",
                        request.getFallbackMetaData(),
                        metaDataFromDistributedData);
            }

            map.put(storageKey, request);
        }

        persist();

        listeners.forEach(listener -> {
            try {
                listener.onRemoved(authenticatedDataFromMap);
            } catch (Exception e) {
                log.error("Calling onRemoved at listener {} failed", listener, e);
            }
        });
        maybeLogMapState("remove success", persistableStore);
        return DataStorageResult.success().removedData(authenticatedDataFromMap);
    }

    public DataStorageResult refresh(RefreshAuthenticatedDataRequest request) {
        maybeLogMapState("refresh ", persistableStore);

        if (request.isExpired()) {
            return DataStorageResult.failed().expired();
        }

        ByteArray storageKey = request.getStorageKey();
        AddAuthenticatedDataRequest updatedRequest;
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
            // We have an entry, lets validate if we can remove it
            AuthenticatedSequentialData sequentialData = addRequestFromMap.getAuthenticatedSequentialData();
            if (request.isSequenceNrInvalid(sequentialData.getSequenceNumber())) {
                log.warn("SequenceNr is invalid at refresh. request={}", request);
                // Sequence number has not increased
                return DataStorageResult.failed().sequenceNrInvalid();
            }

            if (request.isSignaturePubKeyHashInvalid(sequentialData.getPubKeyHash())) {
                log.warn("PublicKey is invalid at refresh. request={}", request);
                // Hash of pubKey of data does not match provided one
                return DataStorageResult.failed().publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at refresh. request={}", request);
                return DataStorageResult.failed().signatureInvalid();
            }

            long refreshDate = request.getCreated();
            if (sequentialData.getDistributedData() instanceof PublishDateAware publishDateAware) {
                publishDateAware.setPublishDate(refreshDate);
            }
            AuthenticatedSequentialData updatedData = AuthenticatedSequentialData.from(sequentialData, request.getSequenceNumber(), refreshDate);
            updatedRequest = new AddAuthenticatedDataRequest(updatedData,
                    addRequestFromMap.getSignature(),
                    addRequestFromMap.getSignaturePubKeyEncoded(),
                    addRequestFromMap.getSignaturePublicKey());

            map.put(storageKey, updatedRequest);
        }

        persist();
        listeners.forEach(listener -> {
            try {
                listener.onRefreshed(updatedRequest.getAuthenticatedSequentialData().getAuthenticatedData());
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

    public int getNextSequenceNumber(ByteArray storageKey) {
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(storageKey);
            if (requestFromMap == null) {
                return 1;
            } else {
                return Math.incrementExact(map.get(storageKey).getSequenceNumber());
            }
        }
    }

    private void pruneExpired() {
        Set<Map.Entry<ByteArray, AuthenticatedDataRequest>> expiredEntries = persistableStore.getMap().entrySet().stream()
                .filter(entry -> {
                    AuthenticatedDataRequest dataRequest = entry.getValue();
                    boolean isExpired = dataRequest.isExpired();
                    if (isExpired) {
                        prunedAndExpiredDataRequests.add(dataRequest);
                    }
                    return isExpired;
                })
                .collect(Collectors.toSet());
        if (!expiredEntries.isEmpty()) {
            log.info("We remove {} expired entries from our {} map", expiredEntries.size(), getStoreKey());
            expiredEntries.forEach(entry -> {
                persistableStore.getMap().remove(entry.getKey());
                if (entry.getValue() instanceof AddAuthenticatedDataRequest) {
                    AuthenticatedData data = ((AddAuthenticatedDataRequest) entry.getValue()).getAuthenticatedSequentialData().getAuthenticatedData();
                    listeners.forEach(listener -> {
                        try {
                            listener.onRemoved(data);
                        } catch (Exception e) {
                            log.error("Calling onRemoved at listener {} failed", listener, e);
                        }
                    });
                }
            });
        }
    }

    private void pruneInvalidAuthorizedData() {
        Map<ByteArray, AuthenticatedDataRequest> invalidAuthorizedData = persistableStore.getMap().entrySet().stream()
                .filter(entry -> {
                    AuthenticatedDataRequest request = entry.getValue();
                    if (request instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
                        AuthenticatedData authenticatedData = addAuthenticatedDataRequest.getAuthenticatedSequentialData().getAuthenticatedData();
                        if (authenticatedData instanceof AuthorizedData authorizedData) {
                            return authorizedData.isNotAuthorized();
                        }
                    }
                    return false;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!invalidAuthorizedData.isEmpty()) {
            invalidAuthorizedData.forEach((key, value) -> {
                log.warn("We prune the AddAuthenticatedDataRequest with an invalid AuthorizedData. {}",
                        StringUtils.truncate(value.toString(), 3000));
                persistableStore.getMap().remove(key);
            });
            persist();
        }
    }

    private void handlePersistedPublishDateAware(DataStore<AuthenticatedDataRequest> persisted) {
        persisted.getMap().values().forEach(authenticatedDataRequest -> {
            // We do not handle RefreshAuthenticatedDataRequest as we would receive a new
            // AddAuthenticatedDataRequest from inventoryRequest anyway as RefreshAuthenticatedDataRequests are
            // not included in inventoryRequests.
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
                    getMaxMapSize());
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
