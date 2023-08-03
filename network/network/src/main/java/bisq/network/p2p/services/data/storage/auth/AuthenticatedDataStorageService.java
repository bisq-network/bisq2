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

import bisq.common.data.ByteArray;
import bisq.common.timer.Scheduler;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public void shutdown() {
        super.shutdown();
        scheduler.stop();
    }

    public Result add(AddAuthenticatedDataRequest request) {
        AuthenticatedSequentialData authenticatedSequentialData = request.getAuthenticatedSequentialData();
        AuthenticatedData authenticatedData = authenticatedSequentialData.getAuthenticatedData();
        byte[] hash = DigestUtil.hash(authenticatedData.serialize());
        ByteArray byteArray = new ByteArray(hash);
        AuthenticatedDataRequest requestFromMap;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (map.size() > getMaxMapSize()) {
                return new Result(false).maxMapSizeReached();
            }
            requestFromMap = map.get(byteArray);
            if (request.equals(requestFromMap)) {
                return new Result(false).requestAlreadyReceived();
            }

            if (requestFromMap != null && authenticatedSequentialData.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                return new Result(false).sequenceNrInvalid();
            }

            if (authenticatedSequentialData.isExpired()) {
                log.info("Data is expired at add. request object={}",
                        request.getAuthenticatedSequentialData().getAuthenticatedData().distributedData.getClass().getSimpleName());
                log.debug("Data is expired at add. request={}", request);
                return new Result(false).expired();
            }

            if (authenticatedData.isDataInvalid(authenticatedSequentialData.getPubKeyHash())) {
                log.warn("AuthenticatedData is invalid at add. request={}", request);
                return new Result(false).dataInvalid();
            }

            if (authenticatedData instanceof AuthorizedData) {
                AuthorizedData authorizedData = (AuthorizedData) authenticatedData;
                if (authorizedData.isNotAuthorized()) {
                    log.warn("AuthorizedData is not authorized. request={}", request);
                    return new Result(false).isNotAuthorized();
                }
            }

            if (request.isPublicKeyInvalid()) {
                log.warn("PublicKey is invalid at add. request={}", request);
                return new Result(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at add. request={}", request);
                return new Result(false).signatureInvalid();
            }
            map.put(byteArray, request);
        }

        persist();

        // If we had already the data (only updated seq nr) we return false as well and do not notify listeners.
       /* if (requestFromMap != null) {
            log.warn("requestFromMap != null. request={}", request);
            return new Result(false).payloadAlreadyStored();
        }*/

        listeners.forEach(listener -> listener.onAdded(authenticatedData));
        return new Result(true);
    }

    public Result remove(RemoveAuthenticatedDataRequest request) {
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
                return new Result(false).noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                // log.debug("Already removed. request={}, map={}", request, map);
                // We have had the entry already removed.
                if (!request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                    // We update the map with the new request with the fresh sequence number.
                    map.put(byteArray, request);
                    persist();
                }
                return new Result(false).alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddProtectedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;

            // The metaData provided in the RemoveAuthenticatedDataRequest must be the same as we had in the AddAuthenticatedDataRequest
            // The AddAuthenticatedDataRequest does use the metaData from the code base, not one provided by the message, thus it is trusted.
            if (!request.getMetaData().equals(addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData())) {
                log.warn("MetaData of remove request not matching the one from the addRequest from the map. {} vs. {}",
                        request.getMetaData(),
                        addRequestFromMap.getAuthenticatedSequentialData().getAuthenticatedData().getMetaData());
                return new Result(false).metaDataInvalid();
            }

            // We have an entry, lets validate if we can remove it
            AuthenticatedSequentialData dataFromMap = addRequestFromMap.getAuthenticatedSequentialData();
            authenticatedDataFromMap = dataFromMap.getAuthenticatedData();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr has not increased at remove. request={}", request);
                return new Result(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyHashInvalid(dataFromMap)) {
                log.warn("PublicKey hash is invalid at remove. request={}", request);
                return new Result(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at remove. request={}", request);
                return new Result(false).signatureInvalid();
            }
            map.put(byteArray, request);
        }
        persist();
        listeners.forEach(listener -> listener.onRemoved(authenticatedDataFromMap));
        return new Result(true).removedData(authenticatedDataFromMap);
    }

    public Result refresh(RefreshAuthenticatedDataRequest request) {
        ByteArray byteArray = new ByteArray(request.getHash());
        AddAuthenticatedDataRequest updatedRequest;
        Map<ByteArray, AuthenticatedDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            AuthenticatedDataRequest requestFromMap = map.get(byteArray);

            if (requestFromMap == null) {
                return new Result(false).noEntry();
            }

            if (requestFromMap instanceof RemoveAuthenticatedDataRequest) {
                return new Result(false).alreadyRemoved();
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
                return new Result(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyInvalid(dataFromMap)) {
                log.warn("PublicKey is invalid at refresh. request={}", request);
                // Hash of pubKey of data does not match provided one
                return new Result(false).publicKeyHashInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at refresh. request={}", request);
                return new Result(false).signatureInvalid();
            }

            AuthenticatedSequentialData updatedData = AuthenticatedSequentialData.from(dataFromMap, request.getSequenceNumber());
            updatedRequest = new AddAuthenticatedDataRequest(updatedData,
                    addRequestFromMap.getSignature(),
                    addRequestFromMap.getOwnerPublicKey());

            map.put(byteArray, updatedRequest);
        }
        persist();
        listeners.forEach(listener -> listener.onRefreshed(updatedRequest.getAuthenticatedSequentialData().getAuthenticatedData()));
        return new Result(true);
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
                    listeners.forEach(listener -> listener.onRemoved(data));
                }
            });
        }
    }
}
