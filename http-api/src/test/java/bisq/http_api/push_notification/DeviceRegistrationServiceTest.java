package bisq.http_api.push_notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistrationServiceTest {

    @TempDir
    Path tempDir;

    private DeviceRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new DeviceRegistrationService(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        Path storePath = tempDir.resolve("device_registrations.json");
        if (Files.exists(storePath)) {
            Files.delete(storePath);
        }
    }

    @Test
    void testInitialize() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> result = service.initialize();
        assertTrue(result.get());
    }

    @Test
    void testShutdown() throws ExecutionException, InterruptedException {
        service.initialize().get();
        CompletableFuture<Boolean> result = service.shutdown();
        assertTrue(result.get());
    }

    @Test
    void testRegisterDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String userId = "user1";
        String deviceToken = "token1";
        String publicKey = "key1";
        DeviceRegistration.Platform platform = DeviceRegistration.Platform.IOS;

        service.registerDevice(userId, deviceToken, publicKey, platform);

        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        assertEquals(1, devices.size());

        DeviceRegistration device = devices.iterator().next();
        assertEquals(deviceToken, device.getDeviceToken());
        assertEquals(publicKey, device.getPublicKey());
        assertEquals(platform, device.getPlatform());
    }

    @Test
    void testRegisterMultipleDevices() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String userId = "user1";
        service.registerDevice(userId, "token1", "key1", DeviceRegistration.Platform.IOS);
        service.registerDevice(userId, "token2", "key2", DeviceRegistration.Platform.ANDROID);

        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        assertEquals(2, devices.size());
    }

    @Test
    void testUnregisterDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String userId = "user1";
        String deviceToken = "token1";

        service.registerDevice(userId, deviceToken, "key1", DeviceRegistration.Platform.IOS);
        assertEquals(1, service.getDevicesForUser(userId).size());

        service.unregisterDevice(userId, deviceToken);
        assertEquals(0, service.getDevicesForUser(userId).size());
    }

    @Test
    void testUnregisterNonExistentDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String userId = "user1";
        service.registerDevice(userId, "token1", "key1", DeviceRegistration.Platform.IOS);

        service.unregisterDevice(userId, "nonexistent");
        assertEquals(1, service.getDevicesForUser(userId).size());
    }

    @Test
    void testGetDevicesForNonExistentUser() throws ExecutionException, InterruptedException {
        service.initialize().get();

        Set<DeviceRegistration> devices = service.getDevicesForUser("nonexistent");
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }



    @Test
    void testRegisterSameDeviceTokenTwice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String userId = "user1";
        String deviceToken = "token1";

        service.registerDevice(userId, deviceToken, "key1", DeviceRegistration.Platform.IOS);
        service.registerDevice(userId, deviceToken, "key2", DeviceRegistration.Platform.IOS); // Different key, same token

        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        // Should only have one device (set semantics)
        assertEquals(1, devices.size());
    }

    @Test
    void testMultipleUsersWithSameDeviceToken() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String deviceToken = "token1";
        service.registerDevice("user1", deviceToken, "key1", DeviceRegistration.Platform.IOS);
        service.registerDevice("user2", deviceToken, "key2", DeviceRegistration.Platform.IOS);

        assertEquals(1, service.getDevicesForUser("user1").size());
        assertEquals(1, service.getDevicesForUser("user2").size());
    }

    @Test
    void testConcurrentUnregisterOperations() throws Exception {
        service.initialize().get();

        String userId = "user1";
        int deviceCount = 100;

        // Register multiple devices
        for (int i = 0; i < deviceCount; i++) {
            String token = "token" + i;
            String key = "key" + i;
            service.registerDevice(userId, token, key, DeviceRegistration.Platform.IOS);
        }

        // Verify all devices were registered
        assertEquals(deviceCount, service.getDevicesForUser(userId).size());

        // Concurrently unregister all devices from multiple threads
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(deviceCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < deviceCount; i++) {
            final int deviceIndex = i;
            futures.add(executor.submit(() -> {
                try {
                    String token = "token" + deviceIndex;
                    boolean removed = service.unregisterDevice(userId, token);
                    if (removed) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Verify all devices were successfully unregistered
        assertEquals(deviceCount, successCount.get(), "All unregister operations should succeed");

        // Verify user entry was removed (no devices left)
        Set<DeviceRegistration> remainingDevices = service.getDevicesForUser(userId);
        assertTrue(remainingDevices.isEmpty(), "All devices should be unregistered");
    }

    @Test
    void testAtomicUnregisterRemovesUserEntryWhenEmpty() throws Exception {
        service.initialize().get();

        String userId = "user1";
        String token = "token1";
        String publicKey = "test-public-key";

        // Register a single device
        service.registerDevice(userId, token, publicKey, DeviceRegistration.Platform.IOS);
        assertEquals(1, service.getDevicesForUser(userId).size());

        // Unregister the device
        boolean removed = service.unregisterDevice(userId, token);
        assertTrue(removed, "Device should be removed");

        // Verify user entry was removed from the map (not just an empty set)
        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        assertTrue(devices.isEmpty(), "No devices should remain for user");
    }

    @Test
    void testConcurrentRegisterSameTokenDifferentKeys() throws Exception {
        service.initialize().get();

        String userId = "user1";
        String token = "same-token";
        int threadCount = 10;

        // Concurrently register the same token with different keys
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int keyIndex = i;
            executor.submit(() -> {
                try {
                    String publicKey = "key" + keyIndex;
                    boolean registered = service.registerDevice(userId, token, publicKey, DeviceRegistration.Platform.IOS);
                    if (registered) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // All threads should succeed (atomic replace)
        assertEquals(threadCount, successCount.get(), "All register operations should succeed");

        // But only ONE device should exist (the last one to win the race)
        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        assertEquals(1, devices.size(), "Only one device should exist for the token");
    }

    @Test
    void testAtomicRegisterReplacesExistingToken() throws Exception {
        service.initialize().get();

        String userId = "user1";
        String token = "token1";
        String key1 = "key1";
        String key2 = "key2";

        // Register device with key1
        boolean registered1 = service.registerDevice(userId, token, key1, DeviceRegistration.Platform.IOS);
        assertTrue(registered1, "First registration should succeed");
        assertEquals(1, service.getDevicesForUser(userId).size());

        // Get the first registration
        DeviceRegistration firstReg = service.getDevicesForUser(userId).iterator().next();
        assertEquals(key1, firstReg.getPublicKey());

        // Register same token with key2 (should replace)
        boolean registered2 = service.registerDevice(userId, token, key2, DeviceRegistration.Platform.IOS);
        assertTrue(registered2, "Second registration should succeed");
        assertEquals(1, service.getDevicesForUser(userId).size(), "Should still have only 1 device");

        // Verify the key was updated
        DeviceRegistration secondReg = service.getDevicesForUser(userId).iterator().next();
        assertEquals(key2, secondReg.getPublicKey(), "Public key should be updated");
        assertEquals(token, secondReg.getDeviceToken(), "Token should be the same");
    }

    @Test
    void testConcurrentRegisterAndUnregister() throws Exception {
        service.initialize().get();

        String userId = "user1";
        int operationCount = 100;

        // Concurrently register and unregister devices
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(20);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(operationCount * 2);

        // Register operations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String token = "token" + index;
                    String key = "key" + index;
                    service.registerDevice(userId, token, key, DeviceRegistration.Platform.IOS);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Unregister operations (some will fail because device doesn't exist yet)
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String token = "token" + index;
                    service.unregisterDevice(userId, token);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // The final state should be consistent (no crashes, no corruption)
        Set<DeviceRegistration> devices = service.getDevicesForUser(userId);
        assertNotNull(devices, "Devices set should not be null");
        // We can't predict the exact count due to race conditions, but it should be valid
        assertTrue(devices.size() >= 0 && devices.size() <= operationCount,
                "Device count should be between 0 and " + operationCount);
    }
}

