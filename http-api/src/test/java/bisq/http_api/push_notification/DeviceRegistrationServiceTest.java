package bisq.http_api.push_notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
}

