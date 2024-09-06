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
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.*;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AuthenticatedDataStorageService extends DataStorageService<AuthenticatedDataRequest> {
    public interface Listener {
        void onAdded(AuthenticatedData authenticatedData);

        void onRemoved(AuthenticatedData authenticatedData);

        default void onRefreshed(AuthenticatedData authenticatedData) {
        }
    }

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
    }

    public DataStorageResult add(AddAuthenticatedDataRequest request) {
        maybeLogMapState("add", persistableStore);
        AuthenticatedSequentialData authenticatedSequentialData = request.getAuthenticatedSequentialData();
        AuthenticatedData authenticatedData = authenticatedSequentialData.getAuthenticatedData();
        DistributedData distributedData = authenticatedData.distributedData;
        byte[] hash = DigestUtil.hash(authenticatedData.serializeForHash());
        ByteArray byteArray = new ByteArray(hash);
        AuthenticatedDataRequest requestFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (isExceedingMapSize()) {
                return new DataStorageResult(false).maxMapSizeReached();
            }

            requestFromMap = map.get(byteArray);
            if (request.equals(requestFromMap)) {
                return new DataStorageResult(false).requestAlreadyReceived();
            }

            if (requestFromMap != null && authenticatedSequentialData.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (authenticatedSequentialData.isExpired()) {
                log.info("AddAuthenticatedDataRequest with {} is expired on {}",
                        distributedData.getClass().getSimpleName(),
                        new Date(authenticatedSequentialData.getCreated() + distributedData.getMetaData().getTtl())
                );
                log.debug("Data is expired at add. request={}", request);
                return new DataStorageResult(false).expired();
            }

            if (authenticatedData.isDataInvalid(authenticatedSequentialData.getPubKeyHash())) {
                log.warn("AuthenticatedData is invalid at add. request={}", request);
                return new DataStorageResult(false).dataInvalid();
            }

            if (authenticatedData instanceof AuthorizedData authorizedData) {
                if (authorizedData.isNotAuthorized()) {
                    log.warn("AuthorizedData is not authorized. request={}", StringUtils.truncate(request.toString(), 500));
                    return new DataStorageResult(false).isNotAuthorized();
                }
            }

            if (request.isPublicKeyInvalid()) {
                log.warn("PublicKey is invalid at add. request={}", request);
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at add. request={}", request);
                return new DataStorageResult(false).signatureInvalid();
            }

            if (distributedData instanceof PublishDateAware publishDateAware) {
                publishDateAware.setPublishDate(authenticatedSequentialData.getCreated());
            }

            map.put(byteArray, request);

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
        return new DataStorageResult(true);
    }

    public DataStorageResult remove(RemoveAuthenticatedDataRequest request) {
        maybeLogMapState("remove ", persistableStore);
        ByteArray byteArray = new ByteArray(request.getHash());
        AuthenticatedData authenticatedDataFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(byteArray);
            if (requestFromMap == null) {
                log.debug("No entry at remove. hash={}", byteArray);
                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(byteArray, request);
                persist();
                return new DataStorageResult(true).noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                // log.debug("Already removed. request={}, map={}", request, map);
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the map with the new request with the fresh sequence number.
                    map.put(byteArray, request);
                    persist();
                }
                return new DataStorageResult(true).alreadyRemoved();
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
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyHashInvalid(dataFromMap)) {
                log.warn("PublicKey hash is invalid at remove. request={}", request);
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at remove. request={}", request);
                return new DataStorageResult(false).signatureInvalid();
            }

            // As metaData from distributedData is taken from the users current code base but the one from RemoveAuthenticatedDataRequest
            // is from the senders version (taken from senders distributedData) it could be different if both users had
            // different versions and metaData has changed between those versions.
            // If we detect such a difference we use our metaData version. This also protects against malicious manipulation.
            MetaData metaDataFromDistributedData = addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData();
            if (!request.getMetaDataFromProto().equals(metaDataFromDistributedData)) {
                request.setMetaDataFromDistributedData(Optional.of(metaDataFromDistributedData));
                log.warn("MetaData of remove request not matching the one from the addRequest from the map. We override " +
                                "metadata with the one we have from the associated distributed data." +
                                "{} vs. {}",
                        request.getMetaDataFromProto(),
                        metaDataFromDistributedData);
            }

            map.put(byteArray, request);
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
        return new DataStorageResult(true).removedData(authenticatedDataFromMap);
    }

    public DataStorageResult refresh(RefreshAuthenticatedDataRequest request) {
        maybeLogMapState("refresh ", persistableStore);
        ByteArray byteArray = new ByteArray(request.getHash());
        AddAuthenticatedDataRequest updatedRequest;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(byteArray);

            if (requestFromMap == null) {
                return new DataStorageResult(false).noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                return new DataStorageResult(false).alreadyRemoved();
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
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyInvalid(sequentialData)) {
                log.warn("PublicKey is invalid at refresh. request={}", request);
                // Hash of pubKey of data does not match provided one
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at refresh. request={}", request);
                return new DataStorageResult(false).signatureInvalid();
            }

            long refreshDate = request.getCreated();
            if (sequentialData.getDistributedData() instanceof PublishDateAware publishDateAware) {
                publishDateAware.setPublishDate(refreshDate);
            }
            AuthenticatedSequentialData updatedData = AuthenticatedSequentialData.from(sequentialData, request.getSequenceNumber(), refreshDate);
            updatedRequest = new AddAuthenticatedDataRequest(updatedData,
                    addRequestFromMap.getSignature(),
                    addRequestFromMap.getOwnerPublicKey());

            map.put(byteArray, updatedRequest);
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
        return new DataStorageResult(true);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @VisibleForTesting
    public int getSequenceNumber(byte[] hash) {
        ByteArray byteArray = new ByteArray(hash);
        int sequenceNumber = 0;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (map.containsKey(byteArray)) {
                sequenceNumber = map.get(byteArray).getSequenceNumber();
            }
        }
        return sequenceNumber;
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
            var added = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof AddAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (AddAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(e -> e.getDistributedData().getClass().getSimpleName())
                    .toList();
            var removed = dataStore.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof RemoveAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (RemoveAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(RemoveAuthenticatedDataRequest::getClassName)
                    .toList();
            var className = Stream.concat(added.stream(), removed.stream())
                    .findAny().orElse(persistence.getFileName().replace("Store", "")); // Remove trailing Store postfix
            log.info("Method: {}; map entry: {}; num AddRequests: {}; num RemoveRequests={}; map size:{}",
                    methodName, className, added.size(), removed.size(), dataStore.getMap().size());
        }
    }
}
