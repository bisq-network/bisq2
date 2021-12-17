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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.ObjectSerializer;
import network.misq.common.util.OsUtils;
import network.misq.network.p2p.services.data.filter.FilterItem;
import network.misq.network.p2p.services.data.filter.ProtectedDataFilter;
import network.misq.network.p2p.services.data.inventory.Inventory;
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.network.p2p.services.data.storage.Util;
import network.misq.security.DigestUtil;
import network.misq.security.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AuthenticatedDataStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Getter
    private static class MockDataTransaction implements AuthenticatedDataRequest {
        private final int sequenceNumber;
        private final long created;

        public MockDataTransaction(int sequenceNumber, long created) {
            this.sequenceNumber = sequenceNumber;
            this.created = created;
        }
    }

    @Test
    public void testGetSubSet() {
        List<AuthenticatedDataRequest> map = new ArrayList<>();
        map.add(new MockDataTransaction(1, 0));
        int filterOffset = 0;
        int filterRange = 100;
        int maxItems = Integer.MAX_VALUE;
        List<? extends AuthenticatedDataRequest> result = Util.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(1, result.size());

        map = new ArrayList<>();
        filterOffset = 0;
        filterRange = 50;
        maxItems = Integer.MAX_VALUE;
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            map.add(new MockDataTransaction(i, iterations - i)); // created are inverse order so we can test sorting
        }
        result = Util.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(50, result.size());

        filterOffset = 25;
        result = Util.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(50, result.size());
        assertEquals(74, result.get(0).getSequenceNumber()); // sorted by date, so list is inverted -> 99-25
        assertEquals(26, result.get(0).getCreated());       // original item i=74 had 100-74=26
        assertEquals(25, result.get(49).getSequenceNumber());

        filterOffset = 85; // 85+50 > 100 -> throw
        try {
            Util.getSubSet(map, filterOffset, filterRange, maxItems);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        filterRange = 150; // > 100 -> throw
        filterOffset = 0;
        try {
            Util.getSubSet(map, filterOffset, filterRange, maxItems);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        filterOffset = 0;
        filterRange = 100;
        maxItems = 5;
        result = Util.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(5, result.size());

        filterOffset = 0;
        filterRange = 100;
        maxItems = 500;
        result = Util.getSubSet(map, filterOffset, filterRange, maxItems);
        assertEquals(100, result.size());
    }

    @Test
    public void testAddAndRemove() throws GeneralSecurityException, IOException {
        MockAuthenticatedPayload data = new MockAuthenticatedPayload("test" + UUID.randomUUID());
        AuthenticatedDataStore store = new AuthenticatedDataStore(appDirPath, data.getMetaData());
        KeyPair keyPair = KeyGeneration.generateKeyPair();

        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, data, keyPair);
        int initialMapSize = store.getMap().size();
        byte[] hash = DigestUtil.hash(data.serialize());
        int initialSeqNum = store.getSequenceNumber(hash);
        Result addRequestResult = store.add(addRequest);
        assertTrue(addRequestResult.isSuccess());

        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = store.getMap();
        MapKey mapKey = new MapKey(hash);
        AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        AuthenticatedData dataFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, dataFromMap.getSequenceNumber());
        AuthenticatedPayload payload = addRequest.getAuthenticatedData().getPayload();
        assertEquals(dataFromMap.getPayload(), payload);

        // request inventory with old seqNum
        String dataType = data.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        Inventory inventory = store.getInventory(filter);
        assertEquals(initialMapSize + 1, inventory.getEntries().size());

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 1));
        filter = new ProtectedDataFilter(dataType, filterItems);
        inventory = store.getInventory(filter);
        assertEquals(initialMapSize, inventory.getEntries().size());

        // refresh
        RefreshRequest refreshRequest = RefreshRequest.from(store, data, keyPair);
        Result refreshResult = store.refresh(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        dataFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, dataFromMap.getSequenceNumber());

        //remove
        RemoveRequest removeRequest = RemoveRequest.from(store, data, keyPair);
        Result removeRequestResult = store.remove(removeRequest);
        assertTrue(removeRequestResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());

        // refresh on removed fails
        RefreshRequest refreshAfterRemoveRequest = RefreshRequest.from(store, data, keyPair);
        Result refreshAfterRemoveResult = store.refresh(refreshAfterRemoveRequest);
        assertFalse(refreshAfterRemoveResult.isSuccess());

        // request inventory with old seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 2));
        filter = new ProtectedDataFilter(dataType, filterItems);
        inventory = store.getInventory(filter);
        assertEquals(initialMapSize + 1, inventory.getEntries().size());

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 3));
        filter = new ProtectedDataFilter(dataType, filterItems);
        inventory = store.getInventory(filter);
        assertEquals(initialMapSize, inventory.getEntries().size());
    }

    @Test
    public void testGetInv() throws GeneralSecurityException, IOException {
        MockAuthenticatedPayload data = new MockAuthenticatedPayload("test");
        AuthenticatedDataStore store = new AuthenticatedDataStore(appDirPath, data.getMetaData());
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        int initialSeqNumFirstItem = 0;
        MockAuthenticatedPayload first;
        byte[] hashOfFirst = new byte[]{};
        int iterations = 10;
        long ts = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            data = new MockAuthenticatedPayload("test" + UUID.randomUUID());
            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, data, keyPair);
            Result addRequestResult = store.add(addRequest);
            assertTrue(addRequestResult.isSuccess());

            if (i == 0) {
                first = data;
                hashOfFirst = DigestUtil.hash(first.serialize());
                initialSeqNumFirstItem = store.getSequenceNumber(hashOfFirst);
            }
        }

        // request inventory with first item and same seq num
        // We should get iterations-1 items
        String dataType = data.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(new MapKey(hashOfFirst).getHash(), initialSeqNumFirstItem + 1));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        int maxItems = store.getMaxItems();
        int expectedSize = Math.min(maxItems, store.getMap().size() - 1);
        int expectedTruncated = Math.max(0, store.getMap().size() - maxItems - 1);
        log.info("getMap()={}, maxItems={}, iterations={}, maxItems={}, expectedSize {}, expectedTruncated={}",
                store.getMap().size(), maxItems, iterations, maxItems, expectedSize, expectedTruncated);
        log.info("dummy size={}", ObjectSerializer.serialize(data).length); // 251
        Inventory inventory = store.getInventory(filter);
        assertEquals(expectedSize, inventory.getEntries().size());
        assertEquals(expectedTruncated, inventory.getNumDropped());

        log.info("inventory size={}", ObjectSerializer.serialize(inventory).length); //inventory size=238601 for 333 items. 716 bytes per item
        // map with 1440 items: file: 1.068.599 bytes, inventory size=1000517 ,  maxItems=1400
    }
}
