package bisq.http_api.push_notification;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests thread safety of DeviceRegistrationStore.
 * Verifies that deep copying prevents shared mutable state and that
 * concurrent modifications are safe.
 */
class DeviceRegistrationStoreThreadSafetyTest {

    private MobileDeviceProfile createProfile(String deviceId, String token, String key, MobileDevicePlatform platform) {
        return new MobileDeviceProfile(deviceId, token, key, "Test Device", platform, System.currentTimeMillis());
    }

    @Test
    void testDeepCopyInConstructor() {
        // Create a store with some devices
        Map<String, MobileDeviceProfile> originalMap = new ConcurrentHashMap<>();
        originalMap.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        // Create a new store from the original map
        DeviceRegistrationStore store = new DeviceRegistrationStore(originalMap);

        // Modify the original map
        originalMap.put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.IOS));

        // Verify the store's map was NOT affected (deep copy)
        assertEquals(1, store.getDeviceByDeviceId().size(),
                "Store should have deep-copied the map, not shared it");
    }

    @Test
    void testDeepCopyInGetClone() {
        DeviceRegistrationStore original = new DeviceRegistrationStore();
        original.getDeviceByDeviceId().put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        // Clone the store
        DeviceRegistrationStore clone = original.getClone();

        // Modify the original
        original.getDeviceByDeviceId().put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.IOS));

        // Verify the clone was NOT affected (deep copy)
        assertEquals(1, clone.getDeviceByDeviceId().size(),
                "Clone should have deep-copied the map, not shared it");
        assertEquals(2, original.getDeviceByDeviceId().size(),
                "Original should have 2 devices");
    }

    @Test
    void testDeepCopyInApplyPersisted() {
        DeviceRegistrationStore store1 = new DeviceRegistrationStore();
        DeviceRegistrationStore store2 = new DeviceRegistrationStore();

        store1.getDeviceByDeviceId().put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        // Apply persisted from store1 to store2
        store2.applyPersisted(store1);

        // Modify store1's map
        store1.getDeviceByDeviceId().put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.IOS));

        // Verify store2 was NOT affected (deep copy)
        assertEquals(1, store2.getDeviceByDeviceId().size(),
                "applyPersisted should deep-copy map, not share it");
        assertEquals(2, store1.getDeviceByDeviceId().size(),
                "Original should have 2 devices");
    }

    @Test
    void testThreadSafeMapUsed() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();

        // Verify the map is thread-safe (ConcurrentHashMap)
        assertTrue(store.getDeviceByDeviceId() instanceof ConcurrentHashMap,
                "Map should be ConcurrentHashMap for thread safety");
    }

    @Test
    void testConcurrentModifications() throws InterruptedException {
        DeviceRegistrationStore store = new DeviceRegistrationStore();
        int threadCount = 10;
        int devicesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Concurrently add devices from multiple threads
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < devicesPerThread; i++) {
                        String deviceId = "thread" + threadId + "_device" + i;
                        String token = "thread" + threadId + "_token" + i;
                        String key = "key" + i;
                        store.getDeviceByDeviceId().put(deviceId, createProfile(deviceId, token, key, MobileDevicePlatform.IOS));
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verify all devices were added successfully
        assertEquals(threadCount * devicesPerThread, store.getDeviceByDeviceId().size(),
                "All concurrent additions should succeed with thread-safe map");
        assertEquals(threadCount * devicesPerThread, successCount.get(),
                "All additions should have been attempted");
    }

    @Test
    void testNoSharedMutableStateAcrossStores() {
        // Create original store
        Map<String, MobileDeviceProfile> map1 = new ConcurrentHashMap<>();
        map1.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        DeviceRegistrationStore store1 = new DeviceRegistrationStore(map1);
        DeviceRegistrationStore store2 = new DeviceRegistrationStore(map1);

        // Modify store1
        store1.getDeviceByDeviceId().put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.IOS));

        // Verify store2 is NOT affected
        assertEquals(1, store2.getDeviceByDeviceId().size(),
                "Stores should not share mutable state");
    }

    @Test
    void testAtomicRemovalWithCompute() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();

        // Add some devices
        store.getDeviceByDeviceId().put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));
        store.getDeviceByDeviceId().put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.IOS));

        assertEquals(2, store.getDeviceByDeviceId().size());

        // Atomically remove a device
        MobileDeviceProfile removed = store.getDeviceByDeviceId().remove("device1");

        assertNotNull(removed, "Device should have been removed");
        assertEquals(1, store.getDeviceByDeviceId().size(),
                "Should have 1 device remaining");

        // Remove the last device
        removed = store.getDeviceByDeviceId().remove("device2");

        assertNotNull(removed, "Device should have been removed");
        assertTrue(store.getDeviceByDeviceId().isEmpty(),
                "Store should be empty after removing all devices");
    }
}

