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

package bisq.network.p2p.services.data.inventory.filter.hash_set;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.Sequential;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class HashSetFilterService extends FilterService<HashSetFilter> {
    // The sequence number in the entry of a request without a signed sequence number. The other stored requests have
    // positive sequence numbers, and in HASH_SET_V2 an add request without signature V2 has its negated number.
    static final int NO_SIGNED_SEQUENCE_NUMBER = 0;

    // Fixed per instance, so that requests of both types can be served concurrently
    private final InventoryFilterType inventoryFilterType;

    public HashSetFilterService(StorageService storageService, int maxSize, InventoryFilterType inventoryFilterType) {
        super(storageService, maxSize);
        checkArgument(inventoryFilterType.isHashSet(), "Invalid inventoryFilterType %s", inventoryFilterType);
        this.inventoryFilterType = inventoryFilterType;
    }

    public HashSetFilter getFilter() {
        List<HashSetFilterEntry> filterEntries = storageService.getAllDataRequestMapEntries()
                .map(this::toFilterEntry)
                .collect(Collectors.toList());
        if (filterEntries.size() > HashSetFilter.MAX_ENTRIES) {
            Collections.shuffle(filterEntries);
            filterEntries = filterEntries.stream().limit(HashSetFilter.MAX_ENTRIES).collect(Collectors.toList());
            log.warn("We limited the number of filter entries we send in our inventory request to {}",
                    HashSetFilter.MAX_ENTRIES);
        }
        return new HashSetFilter(inventoryFilterType, filterEntries);
    }

    // Uses all requests which we hold, not the sample of getFilter(). If we hold more requests than a filter can list,
    // each filter is a new random sample, so a request which we hold can be listed in the next filter. The next response
    // of the peer then differs (see docs/specifications/network/inventory-hash-set-filter.md).
    @Override
    public boolean isAnyRequestInFilter(Collection<? extends DataRequest> dataRequests) {
        Set<HashSetFilterEntry> filterEntries = storageService.getAllDataRequestMapEntries()
                .map(this::toFilterEntry)
                .collect(Collectors.toSet());
        return dataRequests.stream().anyMatch(dataRequest -> isInFilter(filterEntries, dataRequest));
    }

    // The request comes from a peer and is not the map entry of a store, so the storage key is computed from it
    private boolean isInFilter(Set<HashSetFilterEntry> filterEntries, DataRequest dataRequest) {
        try {
            return isListed(filterEntries, toFilterEntry(Map.entry(dataRequest.getStorageKey(), dataRequest)));
        } catch (Exception e) {
            // No store holds a request whose storage key cannot be computed
            log.debug("Could not compute the filter entry of {}", dataRequest.getClass().getSimpleName(), e);
            return false;
        }
    }

    @Override
    protected HashSetFilter safeCast(InventoryFilter inventoryFilter) {
        if (inventoryFilter instanceof HashSetFilter
                && inventoryFilter.getInventoryFilterType() == inventoryFilterType) {
            return (HashSetFilter) inventoryFilter;
        }
        throw new IllegalArgumentException("InventoryFilter not of expected type. inventoryFilter=" + inventoryFilter);
    }

    @Override
    protected boolean isAuthenticatedDataRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry) {
        return isSequentialRequestMissing(filter, toFilterEntry(mapEntry));
    }

    // A requester of an earlier version (HASH_SET) holds one request per storage key V1 and accepts for it only a higher
    // sequence number. It cannot store a removal for which this node holds an add request with the same storage key V1
    // and a higher sequence number. If it holds that add request, isSequentialRequestMissing already excludes the
    // removal. Otherwise it receives the add request in the same response, because add requests are selected before
    // removals, and then rejects the removal. After the migration of the publisher of authorized data, this applies to
    // every removal of the migration. The requester also accepts a removal of an add request only from its owner, so
    // it cannot store a removal of another key than the owner of the data of such an add request either (rule 11 of
    // docs/specifications/network/inventory-hash-set-filter.md).
    @Override
    protected List<RemoveAuthenticatedDataRequest> getStorableRemovals(
            List<RemoveAuthenticatedDataRequest> missingRemovals) {
        if (inventoryFilterType != InventoryFilterType.HASH_SET || missingRemovals.isEmpty()) {
            return missingRemovals;
        }
        // The add requests with the same storage key V1 hold the same data, so they have the same owner
        Map<ByteArray, AddAuthenticatedDataRequest> addWithHighestSequenceNumberByStorageKeyV1 =
                storageService.getAuthenticatedDataStoreMaps()
                        .flatMap(map -> map.values().stream())
                        .filter(AddAuthenticatedDataRequest.class::isInstance)
                        .map(AddAuthenticatedDataRequest.class::cast)
                        .collect(Collectors.toMap(AddAuthenticatedDataRequest::getStorageKeyV1,
                                Function.identity(),
                                (first, second) ->
                                        first.getSequenceNumber() >= second.getSequenceNumber() ? first : second));
        return missingRemovals.stream()
                .filter(removal -> {
                    AddAuthenticatedDataRequest add =
                            addWithHighestSequenceNumberByStorageKeyV1.get(removal.getStorageKeyV1());
                    return add == null ||
                            (add.getSequenceNumber() <= removal.getSequenceNumber() && isSignedByOwner(removal, add));
                })
                .toList();
    }

    private static boolean isSignedByOwner(RemoveAuthenticatedDataRequest removal, AddAuthenticatedDataRequest add) {
        byte[] signerPubKeyHash = DigestUtil.hash(removal.getSignaturePubKeyEncoded());
        return add.getAuthenticatedSequentialData().getAuthenticatedData().isOwner(signerPubKeyHash);
    }

    // A requester of an earlier version (HASH_SET) holds one request per storage key V1 and accepts a removal of mailbox
    // data only from the receiver of the add request which it holds. It cannot store a removal of another key than the
    // receiver of an add request which this node holds with the same storage key V1: it holds that add request, or
    // receives it in the same response (rule 11 of docs/specifications/network/inventory-hash-set-filter.md).
    @Override
    protected List<RemoveMailboxRequest> getStorableMailboxRemovals(List<RemoveMailboxRequest> missingRemovals) {
        if (inventoryFilterType != InventoryFilterType.HASH_SET || missingRemovals.isEmpty()) {
            return missingRemovals;
        }
        // Add requests of the same mailbox data can name different receivers, so all of them count
        Map<ByteArray, List<AddMailboxRequest>> addsByStorageKeyV1 = storageService.getMailboxStoreMaps()
                .flatMap(map -> map.values().stream())
                .filter(AddMailboxRequest.class::isInstance)
                .map(AddMailboxRequest.class::cast)
                .collect(Collectors.groupingBy(AddMailboxRequest::getStorageKeyV1));
        return missingRemovals.stream()
                .filter(removal -> {
                    List<AddMailboxRequest> adds = addsByStorageKeyV1.get(removal.getStorageKeyV1());
                    return adds == null || adds.stream().anyMatch(add -> isSignedByReceiver(removal, add));
                })
                .toList();
    }

    private static boolean isSignedByReceiver(RemoveMailboxRequest removal, AddMailboxRequest add) {
        return Arrays.equals(add.getMailboxSequentialData().getReceiversPubKeyBytes(),
                removal.getSignaturePubKeyEncoded());
    }

    @Override
    protected boolean isMailboxRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, MailboxRequest> mapEntry) {
        return isSequentialRequestMissing(filter, toFilterEntry(mapEntry));
    }

    // A requester of an earlier version (HASH_SET) holds one request per storage key V1, and its stores accept for that
    // key only a request with a higher sequence number, of any owner. It rejects a request whose number is not higher
    // than the one which it lists for the key, so its filter does not change, and every response would contain the
    // request again. Such a request is therefore not missing. A requester of this version stops after a full response
    // which it cannot store (rule 6), and the entries of HASH_SET_V2 do not always show the number by which it orders
    // an entry, so the rule applies only to HASH_SET (rule 12 of
    // docs/specifications/network/inventory-hash-set-filter.md).
    private boolean isSequentialRequestMissing(HashSetFilter filter, HashSetFilterEntry entry) {
        if (isListed(filter.getFilterEntriesAsSet(), entry)) {
            return false;
        }
        if (inventoryFilterType != InventoryFilterType.HASH_SET) {
            return true;
        }
        Integer listedSequenceNumber = filter.getLowestSequenceNumberByHash().get(new ByteArray(entry.getHash()));
        return listedSequenceNumber == null || listedSequenceNumber < entry.getSequenceNumber();
    }

    @Override
    protected boolean isAddAppendOnlyDataRequestMissing(HashSetFilter filter, Map.Entry<ByteArray, AddAppendOnlyDataRequest> mapEntry) {
        return !filter.getFilterEntriesAsSet().contains(toFilterEntry(mapEntry));
    }

    // An add request without signature V2 has the entry of its negated sequence number in HASH_SET_V2. A filter which
    // lists the entry of its complete request, with the positive number, lists it too: the requester holds the
    // complete request, which replaces the copy in a store, so the copy is not missing. In the other direction the
    // complete request is missing for a requester which lists only the copy, so a responder sends it (see
    // docs/specifications/network/inventory-hash-set-filter.md).
    private static boolean isListed(Set<HashSetFilterEntry> filterEntries, HashSetFilterEntry entry) {
        if (filterEntries.contains(entry)) {
            return true;
        }
        return entry.getSequenceNumber() < 0 &&
                filterEntries.contains(new HashSetFilterEntry(entry.getHash(), -entry.getSequenceNumber()));
    }

    // Used for the filter which we send and for the comparison with a received filter, so both use the same key
    private HashSetFilterEntry toFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        byte[] hash = getStorageKey(mapEntry).getBytes();
        return new HashSetFilterEntry(hash, getSequenceNumber(mapEntry.getValue()));
    }

    // AddAppendOnlyDataRequest does not use a seq nr., therefore it is not Sequential. In HASH_SET_V2, a removal
    // without signature V2 has no signed sequence number either, because signature V1 does not cover it. A relay of an
    // earlier version removes signature V2 and keeps the number. The entry of that copy must differ from the entry of
    // the removal with signature V2, which replaces it (see docs/specifications/network/inventory-hash-set-filter.md).
    // An add request without signature V2 has a signed sequence number, but its complete request with the same number
    // replaces it in a store. So its entry has the negated number: it differs from the entry of the complete request,
    // and from the entries of the copies with other numbers.
    private int getSequenceNumber(DataRequest dataRequest) {
        boolean isWithoutSignatureV2 = dataRequest instanceof SignedRequest signedRequest &&
                !signedRequest.isSignatureV2Present();
        if (inventoryFilterType == InventoryFilterType.HASH_SET_V2 && isWithoutSignatureV2) {
            if (dataRequest instanceof RemoveDataRequest) {
                return NO_SIGNED_SEQUENCE_NUMBER;
            }
            if (dataRequest instanceof Sequential sequential) {
                return -sequential.getSequenceNumber();
            }
        }
        return dataRequest instanceof Sequential sequential ? sequential.getSequenceNumber() : NO_SIGNED_SEQUENCE_NUMBER;
    }

    // The stores hold every request under storage key V2 (DataStorageService.preProcessPersisted re-keys persisted
    // entries), so the map key is storage key V2. Storage key V1 is derived from the request.
    private ByteArray getStorageKey(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        return switch (inventoryFilterType) {
            case HASH_SET -> mapEntry.getValue().getStorageKeyV1();
            case HASH_SET_V2 -> mapEntry.getKey();
            case MINI_SKETCH -> throw new IllegalStateException("Not a hash set filter type " + inventoryFilterType);
        };
    }
}
