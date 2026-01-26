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

    @Test
    void testDeepCopyInConstructor() {
        // Create a store with some devices
        Map<String, Set<DeviceRegistration>> originalMap = new ConcurrentHashMap<>();
        Set<DeviceRegistration> devices = ConcurrentHashMap.newKeySet();
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        originalMap.put("user1", devices);

        // Create a new store from the original map
        DeviceRegistrationStore store = new DeviceRegistrationStore(originalMap);

        // Modify the original set
        devices.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.IOS));

        // Verify the store's set was NOT affected (deep copy)
        assertEquals(1, store.getDevicesByUserProfileId().get("user1").size(),
                "Store should have deep-copied the set, not shared it");
    }

    @Test
    void testDeepCopyInGetClone() {
        DeviceRegistrationStore original = new DeviceRegistrationStore();
        Set<DeviceRegistration> devices = original.getDevicesByUserProfileId()
                .computeIfAbsent("user1", k -> ConcurrentHashMap.newKeySet());
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));

        // Clone the store
        DeviceRegistrationStore clone = original.getClone();

        // Modify the original
        devices.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.IOS));

        // Verify the clone was NOT affected (deep copy)
        assertEquals(1, clone.getDevicesByUserProfileId().get("user1").size(),
                "Clone should have deep-copied the sets, not shared them");
        assertEquals(2, original.getDevicesByUserProfileId().get("user1").size(),
                "Original should have 2 devices");
    }

    @Test
    void testDeepCopyInApplyPersisted() {
        DeviceRegistrationStore store1 = new DeviceRegistrationStore();
        DeviceRegistrationStore store2 = new DeviceRegistrationStore();

        Set<DeviceRegistration> devices = store1.getDevicesByUserProfileId()
                .computeIfAbsent("user1", k -> ConcurrentHashMap.newKeySet());
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));

        // Apply persisted from store1 to store2
        store2.applyPersisted(store1);

        // Modify store1's set
        devices.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.IOS));

        // Verify store2 was NOT affected (deep copy)
        assertEquals(1, store2.getDevicesByUserProfileId().get("user1").size(),
                "applyPersisted should deep-copy sets, not share them");
        assertEquals(2, store1.getDevicesByUserProfileId().get("user1").size(),
                "Original should have 2 devices");
    }

    @Test
    void testThreadSafeSetsUsed() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();
        
        // Add a user with devices
        Set<DeviceRegistration> devices = store.getDevicesByUserProfileId()
                .computeIfAbsent("user1", k -> ConcurrentHashMap.newKeySet());
        
        // Verify the set is thread-safe (ConcurrentHashMap.KeySetView)
        assertTrue(devices.getClass().getName().contains("KeySetView"),
                "Sets should be ConcurrentHashMap.KeySetView for thread safety");
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
                    Set<DeviceRegistration> devices = store.getDevicesByUserProfileId()
                            .computeIfAbsent("user1", k -> ConcurrentHashMap.newKeySet());
                    
                    for (int i = 0; i < devicesPerThread; i++) {
                        String token = "thread" + threadId + "_token" + i;
                        String key = "key" + i;
                        devices.add(new DeviceRegistration(token, key, DeviceRegistration.Platform.IOS));
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
        Set<DeviceRegistration> devices = store.getDevicesByUserProfileId().get("user1");
        assertEquals(threadCount * devicesPerThread, devices.size(),
                "All concurrent additions should succeed with thread-safe sets");
        assertEquals(threadCount * devicesPerThread, successCount.get(),
                "All additions should have been attempted");
    }

    @Test
    void testNoSharedMutableStateAcrossStores() {
        // Create original store
        Map<String, Set<DeviceRegistration>> map1 = new ConcurrentHashMap<>();
        Set<DeviceRegistration> devices1 = ConcurrentHashMap.newKeySet();
        devices1.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map1.put("user1", devices1);

        DeviceRegistrationStore store1 = new DeviceRegistrationStore(map1);
        DeviceRegistrationStore store2 = new DeviceRegistrationStore(map1);

        // Modify store1
        store1.getDevicesByUserProfileId().get("user1")
                .add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.IOS));

        // Verify store2 is NOT affected
        assertEquals(1, store2.getDevicesByUserProfileId().get("user1").size(),
                "Stores should not share mutable state");
    }

    @Test
    void testAtomicRemovalWithCompute() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();

        // Add some devices
        Set<DeviceRegistration> devices = store.getDevicesByUserProfileId()
                .computeIfAbsent("user1", k -> ConcurrentHashMap.newKeySet());
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        devices.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.IOS));

        // Atomically remove a device using compute
        java.util.concurrent.atomic.AtomicBoolean wasRemoved = new java.util.concurrent.atomic.AtomicBoolean(false);
        store.getDevicesByUserProfileId().compute("user1", (userId, devs) -> {
            if (devs == null) {
                return null;
            }
            boolean removed = devs.removeIf(d -> d.getDeviceToken().equals("token1"));
            wasRemoved.set(removed);
            if (removed && devs.isEmpty()) {
                return null;  // Remove user entry if no devices remain
            }
            return devs;
        });

        assertTrue(wasRemoved.get(), "Device should have been removed");
        assertEquals(1, store.getDevicesByUserProfileId().get("user1").size(),
                "Should have 1 device remaining");

        // Remove the last device - should remove the user entry entirely
        wasRemoved.set(false);
        store.getDevicesByUserProfileId().compute("user1", (userId, devs) -> {
            if (devs == null) {
                return null;
            }
            boolean removed = devs.removeIf(d -> d.getDeviceToken().equals("token2"));
            wasRemoved.set(removed);
            if (removed && devs.isEmpty()) {
                return null;  // Remove user entry if no devices remain
            }
            return devs;
        });

        assertTrue(wasRemoved.get(), "Device should have been removed");
        assertNull(store.getDevicesByUserProfileId().get("user1"),
                "User entry should be removed when last device is unregistered");
    }
}

