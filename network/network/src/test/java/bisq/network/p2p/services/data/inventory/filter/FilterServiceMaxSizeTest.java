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

package bisq.network.p2p.services.data.inventory.filter;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// See rule 13 of docs/specifications/network/inventory-hash-set-filter.md
class FilterServiceMaxSizeTest {
    private static final int MAX_SIZE = 1000;
    private static final int HIGH_PRIORITY = 10;
    private static final int LOW_PRIORITY = 1;

    @Test
    void requestLargerThanTheSizeLimitDoesNotEndTheResponse() {
        // The large request has the highest priority, so it is the first request of the response
        AddAuthenticatedDataRequest largeAdd = request(AddAuthenticatedDataRequest.class, 1, MAX_SIZE + 1, HIGH_PRIORITY);
        AddAuthenticatedDataRequest smallAdd = request(AddAuthenticatedDataRequest.class, 2, 100, LOW_PRIORITY);
        RemoveAuthenticatedDataRequest removal = request(RemoveAuthenticatedDataRequest.class, 3, 100, LOW_PRIORITY);
        AddMailboxRequest mailboxAdd = request(AddMailboxRequest.class, 4, 100, LOW_PRIORITY);
        RemoveMailboxRequest mailboxRemoval = request(RemoveMailboxRequest.class, 5, 100, LOW_PRIORITY);
        AddAppendOnlyDataRequest appendOnly = request(AddAppendOnlyDataRequest.class, 6, 100, LOW_PRIORITY);
        StorageService storageService = storageService(List.of(largeAdd, smallAdd, removal),
                List.of(mailboxAdd, mailboxRemoval),
                List.of(appendOnly));

        Inventory inventory = new AllMissingFilterService(storageService).createInventory(mock(InventoryFilter.class));

        assertThat(entries(inventory))
                .containsExactlyInAnyOrder(smallAdd, removal, mailboxAdd, mailboxRemoval, appendOnly);
        assertThat(inventory.isMaxSizeReached()).isFalse();
    }

    @Test
    void requestWhichDoesNotFitAnymoreEndsTheResponse() {
        AddAuthenticatedDataRequest firstAdd = request(AddAuthenticatedDataRequest.class, 1, 600, HIGH_PRIORITY);
        AddAuthenticatedDataRequest secondAdd = request(AddAuthenticatedDataRequest.class, 2, 600, LOW_PRIORITY);
        AddMailboxRequest mailboxAdd = request(AddMailboxRequest.class, 3, 100, LOW_PRIORITY);
        StorageService storageService = storageService(List.of(firstAdd, secondAdd), List.of(mailboxAdd), List.of());

        Inventory inventory = new AllMissingFilterService(storageService).createInventory(mock(InventoryFilter.class));

        assertThat(entries(inventory)).containsExactly(firstAdd);
        assertThat(inventory.isMaxSizeReached()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = InventoryFilterType.class, names = {"HASH_SET", "HASH_SET_V2"})
    void hashSetResponderSendsTheRequestsAfterARequestLargerThanTheSizeLimit(InventoryFilterType filterType) {
        AddAuthenticatedDataRequest largeAdd = request(AddAuthenticatedDataRequest.class, 1, MAX_SIZE + 1, HIGH_PRIORITY);
        AddAuthenticatedDataRequest smallAdd = request(AddAuthenticatedDataRequest.class, 2, 100, LOW_PRIORITY);
        RemoveAuthenticatedDataRequest removal = request(RemoveAuthenticatedDataRequest.class, 3, 100, LOW_PRIORITY);
        AddMailboxRequest mailboxAdd = request(AddMailboxRequest.class, 4, 100, LOW_PRIORITY);
        StorageService storageService = storageService(List.of(largeAdd, smallAdd, removal),
                List.of(mailboxAdd),
                List.of());
        var filterService = new HashSetFilterService(storageService, MAX_SIZE, filterType);

        Inventory inventory = filterService.createInventory(new HashSetFilter(filterType, new ArrayList<>()));

        assertThat(entries(inventory)).containsExactlyInAnyOrder(smallAdd, removal, mailboxAdd);
        assertThat(inventory.isMaxSizeReached()).isFalse();
    }

    private static List<DataRequest> entries(Inventory inventory) {
        return new ArrayList<>(inventory.getEntries());
    }

    private static <R extends DataRequest> R request(Class<R> type, int id, int serializedSize, int priority) {
        R request = mock(type);
        when(request.getSerializedSize()).thenReturn(serializedSize);
        when(request.getPriority()).thenReturn(priority);
        when(request.serializeForHash()).thenReturn(new byte[]{(byte) id});
        when(request.getStorageKeyV1()).thenReturn(new ByteArray(hash(id)));
        return request;
    }

    private static StorageService storageService(List<AuthenticatedDataRequest> authenticatedRequests,
                                                 List<MailboxRequest> mailboxRequests,
                                                 List<AddAppendOnlyDataRequest> appendOnlyRequests) {
        StorageService storageService = mock(StorageService.class);
        when(storageService.getAuthenticatedDataStoreMaps())
                .thenAnswer(invocation -> Stream.of(toMap(authenticatedRequests)));
        when(storageService.getMailboxStoreMaps()).thenAnswer(invocation -> Stream.of(toMap(mailboxRequests)));
        when(storageService.getAddAppendOnlyDataStoreMaps())
                .thenAnswer(invocation -> Stream.of(toMap(appendOnlyRequests)));
        return storageService;
    }

    // The map key stands for storage key V2. It must differ from storage key V1, which the requests return.
    private static <R extends DataRequest> Map<ByteArray, R> toMap(List<R> requests) {
        Map<ByteArray, R> map = new HashMap<>();
        requests.forEach(request -> map.put(new ByteArray(hash(100 + request.serializeForHash()[0])), request));
        return map;
    }

    private static byte[] hash(int i) {
        return ByteBuffer.allocate(20).putInt(i).array();
    }

    private static final class AllMissingFilterService extends FilterService<InventoryFilter> {
        AllMissingFilterService(StorageService storageService) {
            super(storageService, MAX_SIZE);
        }

        @Override
        public InventoryFilter getFilter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAnyRequestInFilter(Collection<? extends DataRequest> dataRequests) {
            return false;
        }

        @Override
        protected boolean isAuthenticatedDataRequestMissing(InventoryFilter filter,
                                                            Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry) {
            return true;
        }

        @Override
        protected boolean isMailboxRequestMissing(InventoryFilter filter, Map.Entry<ByteArray, MailboxRequest> mapEntry) {
            return true;
        }

        @Override
        protected boolean isAddAppendOnlyDataRequestMissing(InventoryFilter filter,
                                                            Map.Entry<ByteArray, AddAppendOnlyDataRequest> entry) {
            return true;
        }

        @Override
        protected InventoryFilter safeCast(InventoryFilter inventoryFilter) {
            return inventoryFilter;
        }
    }
}
