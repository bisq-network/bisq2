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

package network.misq.network.p2p.services.data.storage.auth;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.ByteArray;
import network.misq.network.p2p.services.data.filter.ProtectedDataFilter;
import network.misq.network.p2p.services.data.inventory.Inventory;
import network.misq.network.p2p.services.data.inventory.InventoryUtil;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.Result;
import network.misq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import network.misq.network.p2p.services.data.storage.mailbox.DataStore;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxData;
import network.misq.persistence.Persistence;
import network.misq.security.DigestUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AuthenticatedDataStore extends DataStore<AuthenticatedDataRequest> {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    // Max size of serialized NetworkData or MailboxMessage. Used to limit response map.
    // Depends on data types max. expected size.
    // Does not contain metadata like signatures and keys as well not the overhead from encryption.
    // So this number has to be fine-tuned with real data later...
    private static final int MAX_INVENTORY_MAP_SIZE = 1_000_000;

    public interface Listener {
        void onAdded(AuthenticatedPayload authenticatedPayload);

        void onRemoved(AuthenticatedPayload authenticatedPayload);

        default void onRefreshed(AuthenticatedPayload authenticatedPayload) {
        }
    }

    private final int maxItems;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();


    public AuthenticatedDataStore(String appDirPath, MetaData metaData) {
        super(appDirPath, metaData);

        maxItems = MAX_INVENTORY_MAP_SIZE / metaData.getMaxSizeInBytes();
    }

    public CompletableFuture<Void> readPersisted() {
        if (!new File(storageFilePath).exists()) {
            return CompletableFuture.completedFuture(null);
        }
        return Persistence.readAsync(storageFilePath)
                .whenComplete((serializable, t) -> {
                    if (serializable instanceof ConcurrentHashMap) {
                        ConcurrentHashMap<ByteArray, AuthenticatedDataRequest> persisted = (ConcurrentHashMap<ByteArray, AuthenticatedDataRequest>) serializable;
                        maybePruneMap(persisted);
                    }
                }).thenApply(serializable -> null);
    }

    public Result add(AddAuthenticatedDataRequest request) {
        AuthenticatedData data = request.getAuthenticatedData();
        AuthenticatedPayload payload = data.getPayload();
        byte[] hash = DigestUtil.hash(payload.serialize());
        ByteArray byteArray = new ByteArray(hash);
        AuthenticatedDataRequest requestFromMap;
        synchronized (map) {
             requestFromMap = map.get(byteArray);
            if (request.equals(requestFromMap)) {
                return new Result(false).requestAlreadyReceived();
            }

            if (requestFromMap != null && data.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                return new Result(false).sequenceNrInvalid();
            }

            if (data.isExpired()) {
                log.warn("Data is expired at add. request={}", request);
                return new Result(false).expired();
            }

            if (payload.isDataInvalid()) {
                log.warn("Data is invalid at add. request={}", request);
                return new Result(false).dataInvalid();
            }

            if (request.isPublicKeyInvalid()) {
                log.warn("PublicKey is invalid at add. request={}", request);
                return new Result(false).publicKeyInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at add. request={}", request);
                return new Result(false).signatureInvalid();
            }
            map.put(byteArray, request);
           /* log.error("add byteArray={}", byteArray);
            if (requestFromMap != null)
                log.error("requestFromMap.getSequenceNumber() {}", requestFromMap.getSequenceNumber());
            log.error("data.sequenceNumber {}", data.sequenceNumber);
            log.error("request.equals(requestFromMap) {}", request.equals(requestFromMap));
            log.error("request {}", request);
            log.error("requestFromMap {}", requestFromMap);*/
        }
        
        persist();

        // If we had already the data (only updated seq nr) we return false as well and do not notify listeners.
        if (requestFromMap != null) {
            return new Result(false).payloadAlreadyStored();
        }
        
        listeners.forEach(listener -> listener.onAdded(payload));
        return new Result(true);
    }

    public Result remove(RemoveRequest request) {
        ByteArray byteArray = new ByteArray(request.getHash());
        synchronized (map) {
            AuthenticatedDataRequest requestFromMap = map.get(byteArray);
            if (requestFromMap == null) {
                log.warn("No entry at remove. request={}", request);
                // We don't have any entry, but it might be that we would receive later an add request, so we need to keep
                // track of the sequence number
                map.put(byteArray, request);
                persist();
                return new Result(false).noEntry();
            }

            if (requestFromMap instanceof RemoveRequest) {
                log.warn("Already removed at remove. request={}", request);
                // We have had the entry already removed.
                if (request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
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
            // We have an entry, lets validate if we can remove it
            AuthenticatedData dataFromMap = addRequestFromMap.getAuthenticatedData();
            AuthenticatedPayload payloadFromMap = dataFromMap.getPayload();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr is invalid at remove. request={}", request);
                // Sequence number has not increased
                return new Result(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyInvalid(dataFromMap)) {
                log.warn("PublicKey is invalid at remove. request={}", request);
                // Hash of pubKey of data does not match provided one
                return new Result(false).publicKeyInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at remove. request={}", request);
                return new Result(false).signatureInvalid();
            }

            map.put(byteArray, request);
            listeners.forEach(listener -> listener.onRemoved(payloadFromMap));
        }
        persist();
        return new Result(true);
    }

    public Result refresh(RefreshRequest request) {
        ByteArray byteArray = new ByteArray(request.getHash());
        synchronized (map) {
            AuthenticatedDataRequest requestFromMap = map.get(byteArray);

            if (requestFromMap == null) {
                return new Result(false).noEntry();
            }

            if (requestFromMap instanceof RemoveRequest) {
                return new Result(false).alreadyRemoved();
            }

            // At that point we know requestFromMap is an AddProtectedDataRequest
            checkArgument(requestFromMap instanceof AddAuthenticatedDataRequest,
                    "requestFromMap expected be type of AddAuthenticatedDataRequest");
            AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) requestFromMap;
            // We have an entry, lets validate if we can remove it
            AuthenticatedData dataFromMap = addRequestFromMap.getAuthenticatedData();
            if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
                log.warn("SequenceNr is invalid at refresh. request={}", request);
                // Sequence number has not increased
                return new Result(false).sequenceNrInvalid();
            }

            if (request.isPublicKeyInvalid(dataFromMap)) {
                log.warn("PublicKey is invalid at refresh. request={}", request);
                // Hash of pubKey of data does not match provided one
                return new Result(false).publicKeyInvalid();
            }

            if (request.isSignatureInvalid()) {
                log.warn("Signature is invalid at refresh. request={}", request);
                return new Result(false).signatureInvalid();
            }

            // Update request with new sequence number
            AddAuthenticatedDataRequest updatedRequest;
            if (addRequestFromMap instanceof AddMailboxRequest) {
                //todo why we get AddMailboxRequest here?
                checkArgument(dataFromMap instanceof MailboxData,
                        "dataFromMap expected be type of MailboxData");
                MailboxData mailboxDataFromMap = (MailboxData) dataFromMap;
                MailboxData updatedData = MailboxData.from(mailboxDataFromMap, request.getSequenceNumber());
                updatedRequest = new AddMailboxRequest(updatedData,
                        addRequestFromMap.getSignature(),
                        addRequestFromMap.getOwnerPublicKey());
            } else {
                AuthenticatedData updatedData = AuthenticatedData.from(dataFromMap, request.getSequenceNumber());
                updatedRequest = new AddAuthenticatedDataRequest(updatedData,
                        addRequestFromMap.getSignature(),
                        addRequestFromMap.getOwnerPublicKey());
            }

            map.put(byteArray, updatedRequest);
            listeners.forEach(listener -> listener.onRefreshed(updatedRequest.getAuthenticatedData().payload));
        }
        persist();
        return new Result(true);
    }

    public Inventory getInventory(ProtectedDataFilter dataFilter) {
        List<AuthenticatedDataRequest> inventoryList = getInventoryList(dataFilter.getFilterMap());
        int maxItems = getMaxItems();
        int size = inventoryList.size();
        if (size <= maxItems) {
            return new Inventory(inventoryList, 0);
        }

        List<? extends AuthenticatedDataRequest> result = InventoryUtil.getSubList(
                inventoryList,
                dataFilter.getOffset(),
                dataFilter.getRange(),
                maxItems
        );
        int numDropped = size - result.size();
        return new Inventory(result, numDropped);
    }

    @Override
    public void shutdown() {

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
        synchronized (map) {
            if (map.containsKey(byteArray)) {
                sequenceNumber = map.get(byteArray).getSequenceNumber();
            }
        }
        return sequenceNumber;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @VisibleForTesting
    int getMaxItems() {
        return maxItems;
    }

    @VisibleForTesting
    public Map<ByteArray, AuthenticatedDataRequest> getMap() {
        return new HashMap<>(map);
    }

    private List<AuthenticatedDataRequest> getInventoryList(Map<ByteArray, Integer> requesterMap) {
        return new HashSet<>(map.entrySet()).stream()
                .filter(entry -> {
                    // Any entry we have but is not included in filter gets added
                    if (!requesterMap.containsKey(entry.getKey())) {
                        return true;
                    }
                    // If there is a match we add entry if sequence number is higher
                    return entry.getValue().getSequenceNumber() > requesterMap.get(entry.getKey());
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // todo call by time interval
    private void maybePruneMap(Map<ByteArray, AuthenticatedDataRequest> current) {
        long now = System.currentTimeMillis();
        // Remove entries older than MAX_AGE
        // Remove expired ProtectedEntry in case value is of type AddProtectedDataRequest
        // Sort by created date
        // Limit to MAX_MAP_SIZE
        Map<ByteArray, AuthenticatedDataRequest> pruned = current.entrySet().stream()
                .filter(entry -> now - entry.getValue().getCreated() < MAX_AGE)
                .filter(entry -> entry.getValue() instanceof RemoveRequest ||
                        !((AddAuthenticatedDataRequest) entry.getValue()).getAuthenticatedData().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        synchronized (map) {
            map.clear();
            map.putAll(pruned);
        }
    }
}
