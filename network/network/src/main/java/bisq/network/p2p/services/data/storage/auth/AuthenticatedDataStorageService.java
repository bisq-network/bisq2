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
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
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
    private final Scheduler scheduler;

    public AuthenticatedDataStorageService(PersistenceService persistenceService, String storeName, String storeKey) {
        super(persistenceService, storeName, storeKey);
        scheduler = Scheduler.run(this::pruneExpired).periodically(60, TimeUnit.SECONDS);
    }

    @Override
    public void onPersistedApplied(DataStore<AuthenticatedDataRequest> persisted) {
        maybeLogMapState("onPersistedApplied", persisted);
        pruneInvalidAuthorizedData();
    }

    @Override
    public void shutdown() {
        maybeLogMapState("shutdown", persistableStore);
        super.shutdown();
        scheduler.stop();
    }

    public DataStorageResult add(AddAuthenticatedDataRequest request) {
        maybeLogMapState("add", persistableStore);
        AuthenticatedSequentialData authenticatedSequentialData = request.getAuthenticatedSequentialData();
        AuthenticatedData authenticatedData = authenticatedSequentialData.getAuthenticatedData();
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
                log.info("Data is expired at add. request object={}",
                        request.getAuthenticatedSequentialData().getAuthenticatedData().distributedData.getClass().getSimpleName());
                log.debug("Data is expired at add. request={}", request);
                return new DataStorageResult(false).expired();
            }

            if (authenticatedData.isDataInvalid(authenticatedSequentialData.getPubKeyHash())) {
                log.warn("AuthenticatedData is invalid at add. request={}", request);
                return new DataStorageResult(false).dataInvalid();
            }

            if (authenticatedData instanceof AuthorizedData) {
                AuthorizedData authorizedData = (AuthorizedData) authenticatedData;
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
            map.put(byteArray, request);
        }

        persist();

        // If we had already the data (only updated seq nr) we return false as well and do not notify listeners.
       /* if (requestFromMap != null) {
            log.warn("requestFromMap != null. request={}", request);
            return new Result(false).payloadAlreadyStored();
        }*/

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
                return new DataStorageResult(false).noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                // log.debug("Already removed. request={}, map={}", request, map);
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the map with the new request with the fresh sequence number.
                    map.put(byteArray, request);
                    persist();
                }
                return new DataStorageResult(false).alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddProtectedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;

            // We skip that check for a while because we plan updates of the map size values
            if (new Date().after(IGNORE_MAX_MAP_SIZE_UNTIL)) {
                // The metaData provided in the RemoveAuthenticatedDataRequest must be the same as we had in the AddAuthenticatedDataRequest
                // The AddAuthenticatedDataRequest does use the metaData from the code base, not one provided by the message, thus it is trusted.
                if (!request.getMetaData().equals(addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData())) {
                    log.warn("MetaData of remove request not matching the one from the addRequest from the map. {} vs. {}",
                            request.getMetaData(),
                            addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData());
                }
            }

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
            AuthenticatedSequentialData dataFromMap = addRequestFromMap.getAuthenticatedSequentialData();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr is invalid at refresh. request={}", request);
                // Sequence number has not increased
                return new DataStorageResult(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyInvalid(dataFromMap)) {
                log.warn("PublicKey is invalid at refresh. request={}", request);
                // Hash of pubKey of data does not match provided one
                return new DataStorageResult(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at refresh. request={}", request);
                return new DataStorageResult(false).signatureInvalid();
            }

            AuthenticatedSequentialData updatedData = AuthenticatedSequentialData.from(dataFromMap, request.getSequenceNumber());
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
                .filter(entry -> entry.getValue().isExpired())
                .collect(Collectors.toSet());
        if (!expiredEntries.isEmpty()) {
            log.info("We remove {} expired entries from our map", expiredEntries.size());
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
                    if (request instanceof AddAuthenticatedDataRequest) {
                        AddAuthenticatedDataRequest addAuthenticatedDataRequest = (AddAuthenticatedDataRequest) request;
                        AuthenticatedData authenticatedData = addAuthenticatedDataRequest.getAuthenticatedSequentialData().getAuthenticatedData();
                        if (authenticatedData instanceof AuthorizedData) {
                            AuthorizedData authorizedData = (AuthorizedData) authenticatedData;
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

    // Useful for debugging state of the store
    private void maybeLogMapState(String methodName, DataStore<AuthenticatedDataRequest> persisted) {
        if (DevMode.isDevMode() || methodName.equals("onPersistedApplied")) {
            var added = persisted.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof AddAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (AddAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(e -> e.getAuthenticatedSequentialData().getAuthenticatedData().getDistributedData().getClass().getSimpleName())
                    .collect(Collectors.toList());
            var removed = persisted.getMap().values().stream()
                    .filter(authenticatedDataRequest -> authenticatedDataRequest instanceof RemoveAuthenticatedDataRequest)
                    .map(authenticatedDataRequest -> (RemoveAuthenticatedDataRequest) authenticatedDataRequest)
                    .map(RemoveAuthenticatedDataRequest::getClassName)
                    .collect(Collectors.toList());
            var className = Stream.concat(added.stream(), removed.stream())
                    .findAny().orElse(persistence.getFileName().replace("Store", "")); // Remove trailing Store postfix
            log.info("Method: {}; map entry: {}; num AddRequests: {}; num RemoveRequests={}; map size:{}",
                    methodName, className, added.size(), removed.size(), persisted.getMap().size());
        }
    }
}
