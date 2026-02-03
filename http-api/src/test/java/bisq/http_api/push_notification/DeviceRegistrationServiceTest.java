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

        String deviceId = "device1";
        String deviceToken = "token1";
        String publicKeyBase64 = "key1";
        String deviceDescriptor = "iPhone 15 Pro";
        MobileDevicePlatform platform = MobileDevicePlatform.IOS;

        service.register(deviceId, deviceToken, publicKeyBase64, deviceDescriptor, platform);

        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        assertEquals(1, devices.size());

        MobileDeviceProfile device = devices.iterator().next();
        assertEquals(deviceId, device.getDeviceId());
        assertEquals(deviceToken, device.getDeviceToken());
        assertEquals(publicKeyBase64, device.getPublicKeyBase64());
        assertEquals(deviceDescriptor, device.getDeviceDescriptor());
        assertEquals(platform, device.getPlatform());
    }

    @Test
    void testRegisterMultipleDevices() throws ExecutionException, InterruptedException {
        service.initialize().get();

        service.register("device1", "token1", "key1", "iPhone 15", MobileDevicePlatform.IOS);
        service.register("device2", "token2", "key2", "Pixel 8", MobileDevicePlatform.ANDROID);

        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        assertEquals(2, devices.size());
    }

    @Test
    void testUnregisterDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String deviceId = "device1";

        service.register(deviceId, "token1", "key1", "iPhone 15", MobileDevicePlatform.IOS);
        assertEquals(1, service.getMobileDeviceProfiles().size());

        boolean removed = service.unregister(deviceId);
        assertTrue(removed);
        assertEquals(0, service.getMobileDeviceProfiles().size());
    }

    @Test
    void testUnregisterNonExistentDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        service.register("device1", "token1", "key1", "iPhone 15", MobileDevicePlatform.IOS);

        boolean removed = service.unregister("nonexistent");
        assertFalse(removed);
        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void testGetMobileDeviceProfilesWhenEmpty() throws ExecutionException, InterruptedException {
        service.initialize().get();

        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }

    @Test
    void testRegisterSameDeviceIdTwice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String deviceId = "device1";

        service.register(deviceId, "token1", "key1", "iPhone 15", MobileDevicePlatform.IOS);
        service.register(deviceId, "token2", "key2", "iPhone 15 Pro", MobileDevicePlatform.IOS);

        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        // Should only have one device (replaced by deviceId)
        assertEquals(1, devices.size());

        // Should have the updated values
        MobileDeviceProfile device = devices.iterator().next();
        assertEquals("token2", device.getDeviceToken());
        assertEquals("key2", device.getPublicKeyBase64());
        assertEquals("iPhone 15 Pro", device.getDeviceDescriptor());
    }

    @Test
    void testConcurrentUnregisterOperations() throws Exception {
        service.initialize().get();

        int deviceCount = 100;

        // Register multiple devices
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = "device" + i;
            String token = "token" + i;
            String key = "key" + i;
            service.register(deviceId, token, key, "Device " + i, MobileDevicePlatform.IOS);
        }

        // Verify all devices were registered
        assertEquals(deviceCount, service.getMobileDeviceProfiles().size());

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
                    String deviceId = "device" + deviceIndex;
                    boolean removed = service.unregister(deviceId);
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

        // Verify no devices remain
        Set<MobileDeviceProfile> remainingDevices = service.getMobileDeviceProfiles();
        assertTrue(remainingDevices.isEmpty(), "All devices should be unregistered");
    }

    @Test
    void testConcurrentRegisterSameDeviceIdDifferentData() throws Exception {
        service.initialize().get();

        String deviceId = "device1";
        int threadCount = 10;

        // Concurrently register the same deviceId with different data
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int keyIndex = i;
            executor.submit(() -> {
                try {
                    String publicKey = "key" + keyIndex;
                    service.register(deviceId, "token" + keyIndex, publicKey, "Device " + keyIndex, MobileDevicePlatform.IOS);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Only ONE device should exist (the last one to win the race)
        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        assertEquals(1, devices.size(), "Only one device should exist for the deviceId");
    }

    @Test
    void testRegisterReplacesExistingDevice() throws Exception {
        service.initialize().get();

        String deviceId = "device1";
        String key1 = "key1";
        String key2 = "key2";

        // Register device with key1
        service.register(deviceId, "token1", key1, "iPhone 15", MobileDevicePlatform.IOS);
        assertEquals(1, service.getMobileDeviceProfiles().size());

        // Get the first registration
        MobileDeviceProfile firstReg = service.getMobileDeviceProfiles().iterator().next();
        assertEquals(key1, firstReg.getPublicKeyBase64());

        // Register same deviceId with key2 (should replace)
        service.register(deviceId, "token2", key2, "iPhone 15 Pro", MobileDevicePlatform.IOS);
        assertEquals(1, service.getMobileDeviceProfiles().size(), "Should still have only 1 device");

        // Verify the data was updated
        MobileDeviceProfile secondReg = service.getMobileDeviceProfiles().iterator().next();
        assertEquals(key2, secondReg.getPublicKeyBase64(), "Public key should be updated");
        assertEquals("token2", secondReg.getDeviceToken(), "Token should be updated");
        assertEquals("iPhone 15 Pro", secondReg.getDeviceDescriptor(), "Descriptor should be updated");
    }

    @Test
    void testConcurrentRegisterAndUnregister() throws Exception {
        service.initialize().get();

        int operationCount = 100;

        // Concurrently register and unregister devices
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(operationCount * 2);

        // Register operations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String deviceId = "device" + index;
                    String token = "token" + index;
                    String key = "key" + index;
                    service.register(deviceId, token, key, "Device " + index, MobileDevicePlatform.IOS);
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
                    String deviceId = "device" + index;
                    service.unregister(deviceId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // The final state should be consistent (no crashes, no corruption)
        Set<MobileDeviceProfile> devices = service.getMobileDeviceProfiles();
        assertNotNull(devices, "Devices set should not be null");
        // We can't predict the exact count due to race conditions, but it should be valid
        assertTrue(devices.size() >= 0 && devices.size() <= operationCount,
                "Device count should be between 0 and " + operationCount);
    }

    @Test
    void testRegisterWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () ->
                service.register(null, "token", "key", "desc", MobileDevicePlatform.IOS));
        assertThrows(IllegalArgumentException.class, () ->
                service.register("", "token", "key", "desc", MobileDevicePlatform.IOS));
        assertThrows(IllegalArgumentException.class, () ->
                service.register("id", null, "key", "desc", MobileDevicePlatform.IOS));
        assertThrows(IllegalArgumentException.class, () ->
                service.register("id", "token", null, "desc", MobileDevicePlatform.IOS));
        assertThrows(IllegalArgumentException.class, () ->
                service.register("id", "token", "key", null, MobileDevicePlatform.IOS));
        assertThrows(IllegalArgumentException.class, () ->
                service.register("id", "token", "key", "desc", null));
    }

    @Test
    void testUnregisterWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> service.unregister(null));
        assertThrows(IllegalArgumentException.class, () -> service.unregister(""));
    }
}

