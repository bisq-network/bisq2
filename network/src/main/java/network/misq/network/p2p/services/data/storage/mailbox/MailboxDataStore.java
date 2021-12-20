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

package network.misq.network.p2p.services.data.storage.mailbox;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.services.data.filter.ProtectedDataFilter;
import network.misq.network.p2p.services.data.inventory.Inventory;
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.Util;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import network.misq.network.p2p.services.data.storage.auth.Result;
import network.misq.persistence.Persistence;
import network.misq.security.DigestUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MailboxDataStore extends DataStore<MailboxRequest> {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    // Max size of serialized NetworkData or MailboxMessage. Used to limit response map.
    // Depends on data types max. expected size.
    // Does not contain meta data like signatures and keys as well not the overhead from encryption.
    // So this number has to be fine tuned with real data later...
    private static final int MAX_INVENTORY_MAP_SIZE = 1_000_000;

    public interface Listener {
        void onAdded(MailboxPayload mailboxPayload);

        void onRemoved(MailboxPayload mailboxPayload);
    }

    private final int maxItems;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public MailboxDataStore(String appDirPath, MetaData metaData) throws IOException {
        super(appDirPath, metaData);

        maxItems = MAX_INVENTORY_MAP_SIZE / metaData.getMaxSizeInBytes();

        if (new File(storageFilePath).exists()) {
            Serializable serializable = Persistence.read(storageFilePath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, MailboxRequest> persisted = (ConcurrentHashMap<MapKey, MailboxRequest>) serializable;
                maybePruneMap(persisted);
            }
        }
    }

    public Result add(AddMailboxRequest request) {
        MailboxData data = request.getMailboxData();
        MailboxPayload payload = data.getMailboxPayload();
        byte[] hash = DigestUtil.hash(payload.serialize());
        MapKey mapKey = new MapKey(hash);
        MailboxRequest requestFromMap = map.get(mapKey);
        int sequenceNumberFromMap = requestFromMap != null ? requestFromMap.getSequenceNumber() : 0;

        if (requestFromMap != null && data.isSequenceNrInvalid(sequenceNumberFromMap)) {
            return new Result(false).sequenceNrInvalid();
        }

        if (data.isExpired()) {
            return new Result(false).expired();
        }

        if (payload.isDataInvalid()) {
            return new Result(false).dataInvalid();
        }

        if (request.isPublicKeyInvalid()) {
            return new Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new Result(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onAdded(payload));
        persist();
        return new Result(true);
    }

    public Result remove(RemoveMailboxRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        MailboxRequest requestFromMap = map.get(mapKey);

        if (requestFromMap == null) {
            // We don't have any entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, request);
            persist();
            return new Result(false).noEntry();
        }

        if (requestFromMap instanceof RemoveMailboxRequest) {
            // We have had the entry already removed.
            if (request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                // We update the request so we have latest sequence number.
                map.put(mapKey, request);
                persist();
            }
            return new Result(false).alreadyRemoved();
        }

        // At that point we know requestFromMap is an AddProtectedDataRequest
        AddMailboxRequest addRequest = (AddMailboxRequest) requestFromMap;
        // We have an entry, lets validate if we can remove it
        MailboxData dataFromMap = addRequest.getMailboxData();
        if (request.isSequenceNrInvalid(dataFromMap.getSequenceNumber())) {
            // Sequence number has not increased
            return new Result(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(dataFromMap)) {
            // Hash of pubKey of data does not match provided one
            return new Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new Result(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onRemoved(dataFromMap.getMailboxPayload()));
        persist();
        return new Result(true);
    }

    public Inventory getInventory(ProtectedDataFilter dataFilter) {
        List<MailboxRequest> inventoryMap = getInventoryMap(map, dataFilter.getFilterMap());
        int maxItems = getMaxItems();
        int size = inventoryMap.size();
        if (size <= maxItems) {
            return new Inventory(inventoryMap, 0);
        }

        List<? extends AuthenticatedDataRequest> result = Util.getSubSet(inventoryMap, dataFilter.getOffset(), dataFilter.getRange(), maxItems);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    @VisibleForTesting
    int getMaxItems() {
        return maxItems;
    }

    @VisibleForTesting
    ConcurrentHashMap<MapKey, MailboxRequest> getMap() {
        return map;
    }

    List<MailboxRequest> getInventoryMap(ConcurrentHashMap<MapKey, MailboxRequest> map,
                                         Map<MapKey, Integer> requesterMap) {
        return map.entrySet().stream()
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


    int getSequenceNumber(byte[] hash) {
        MapKey mapKey = new MapKey(hash);
        if (map.containsKey(mapKey)) {
            return map.get(mapKey).getSequenceNumber();
        }
        return 0;
    }

    boolean canAddMailboxMessage(MailboxPayload mailboxPayload) {
        byte[] hash = DigestUtil.hash(mailboxPayload.serialize());
        return getSequenceNumber(hash) < Integer.MAX_VALUE;
    }

    // todo call by time interval
    private void maybePruneMap(ConcurrentHashMap<MapKey, MailboxRequest> current) {
        long now = System.currentTimeMillis();
        // Remove entries older than MAX_AGE
        // Remove expired ProtectedEntry in case value is of type AddProtectedDataRequest
        // Sort by created date
        // Limit to MAX_MAP_SIZE
        Map<MapKey, MailboxRequest> pruned = current.entrySet().stream()
                .filter(entry -> now - entry.getValue().getCreated() < MAX_AGE)
                .filter(entry -> entry.getValue() instanceof RemoveMailboxRequest ||
                        !((AddMailboxRequest) entry.getValue()).getMailboxData().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.putAll(pruned);
    }
}
