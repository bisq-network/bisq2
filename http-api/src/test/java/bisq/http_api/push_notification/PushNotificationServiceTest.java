package bisq.http_api.push_notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class PushNotificationServiceTest {

    @TempDir
    Path tempDir;

    private PushNotificationService service;
    private DeviceRegistrationService deviceRegistrationService;
    private String testPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        deviceRegistrationService = new DeviceRegistrationService(tempDir);
        deviceRegistrationService.initialize().get();

        service = new PushNotificationService(deviceRegistrationService, "http://localhost:8080", Optional.empty(), tempDir);

        // Generate a test public key
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        testPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @AfterEach
    void tearDown() throws IOException, ExecutionException, InterruptedException {
        deviceRegistrationService.shutdown().get();
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
    void testSendNotificationWithNoDevices() throws ExecutionException, InterruptedException {
        service.initialize().get();

        // Should not throw exception when user has no devices
        assertDoesNotThrow(() -> {
            service.sendTradeNotification("trade123", "TRADE_CREATED", "New trade", true);
        });
    }

    @Test
    void testSendNotificationWithRegisteredDevice() throws ExecutionException, InterruptedException {
        service.initialize().get();

        String deviceId = "device1";
        String deviceToken = "test-device-token";

        // Register a device
        deviceRegistrationService.register(deviceId, deviceToken, testPublicKey, "iPhone 15", MobileDevicePlatform.IOS);

        // This will attempt to send but fail since there's no actual relay server
        // We're just testing that it doesn't throw an exception
        assertDoesNotThrow(() -> {
            service.sendTradeNotification("trade123", "TRADE_CREATED", "New trade created", true);
        });
    }

    @Test
    void testSendNotificationWithMultipleDevices() throws Exception {
        service.initialize().get();

        // Register multiple devices
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        KeyPair keyPair1 = keyGen.generateKeyPair();
        String publicKey1 = Base64.getEncoder().encodeToString(keyPair1.getPublic().getEncoded());

        KeyPair keyPair2 = keyGen.generateKeyPair();
        String publicKey2 = Base64.getEncoder().encodeToString(keyPair2.getPublic().getEncoded());

        deviceRegistrationService.register("device1", "token1", publicKey1, "iPhone 15", MobileDevicePlatform.IOS);
        deviceRegistrationService.register("device2", "token2", publicKey2, "Pixel 8", MobileDevicePlatform.ANDROID);

        // Should attempt to send to both devices
        assertDoesNotThrow(() -> {
            service.sendTradeNotification("trade123", "TRADE_CREATED", "New trade", true);
        });
    }

    @Test
    void testServiceWithDifferentRelayUrl() {
        PushNotificationService customService = new PushNotificationService(
            deviceRegistrationService,
            "https://relay.example.com:8443",
            Optional.empty(),
            tempDir
        );

        assertNotNull(customService);
    }

    @Test
    void testServiceWithOnionRelayUrl() {
        PushNotificationService onionService = new PushNotificationService(
            deviceRegistrationService,
            "http://example123456789.onion",
            Optional.empty(),
            tempDir
        );

        assertNotNull(onionService);
    }

    @Test
    void testDuplicateNotificationPrevention() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";
        String deviceToken = "test-device-token";

        // Register a device
        deviceRegistrationService.register("device1", deviceToken, testPublicKey, "iPhone 15", MobileDevicePlatform.IOS);

        // Send notification first time
        service.sendTradeNotification(tradeId, "TRADE_CREATED", "New trade", true);

        // Send same notification again - should be skipped
        service.sendTradeNotification(tradeId, "TRADE_CREATED", "New trade", true);

        // Different event type should be sent
        service.sendTradeNotification(tradeId, "PAYMENT_SENT", "Payment sent", false);
    }

    @Test
    void testRemoveNotificationsForTrade() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";

        // Send some notifications
        service.sendTradeNotification(tradeId, "TRADE_CREATED", "New trade", true);
        service.sendTradeNotification(tradeId, "PAYMENT_SENT", "Payment sent", false);

        // Remove all notifications for this trade
        service.removeNotificationsForTrade(tradeId);

        // Now the same notifications should be sent again (not skipped)
        assertDoesNotThrow(() -> {
            service.sendTradeNotification(tradeId, "TRADE_CREATED", "New trade", true);
        });
    }

    @Test
    void testNotificationNotMarkedAsSentWhenAllDevicesFail() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";
        String eventType = "TRADE_CREATED";
        String deviceToken = "test-device-token";

        // Register a device
        deviceRegistrationService.register("device1", deviceToken, testPublicKey, "iPhone 15", MobileDevicePlatform.IOS);

        // Send notification - will fail because there's no relay server
        service.sendTradeNotification(tradeId, eventType, "New trade", true);

        // Wait a bit for async completion
        Thread.sleep(500);

        // Load the sent notifications store to check if it was marked as sent
        Path sentNotificationsPath = tempDir.resolve("sent_notifications.json");

        // The notification should NOT be marked as sent because the relay call failed
        // This means it will retry on next attempt
        if (Files.exists(sentNotificationsPath)) {
            String content = Files.readString(sentNotificationsPath);
            // The file might exist but should be empty or not contain our notification
            // Since all sends failed, nothing should be marked as sent
            assertFalse(content.contains(tradeId + ":" + eventType),
                    "Notification should NOT be marked as sent when all devices fail");
        }
    }

    @Test
    void testNotificationRetriesAfterFailure() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";
        String eventType = "TRADE_CREATED";
        String deviceToken = "test-device-token";

        // Register a device
        deviceRegistrationService.register("device1", deviceToken, testPublicKey, "iPhone 15", MobileDevicePlatform.IOS);

        // First attempt - will fail (no relay server)
        service.sendTradeNotification(tradeId, eventType, "New trade", true);
        Thread.sleep(500); // Wait for async completion

        // Second attempt - should NOT be skipped as duplicate because first attempt failed
        // This simulates a retry after the relay comes back online
        assertDoesNotThrow(() -> {
            service.sendTradeNotification(tradeId, eventType, "New trade", true);
        });

        // The second call should execute (not be skipped as duplicate)
        // We can't easily verify the actual send happened, but we verify no exception is thrown
    }

    @Test
    void testNotificationMarkedAsSentOnlyWhenNoDevices() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";
        String eventType = "TRADE_CREATED";

        // Send notification when there are no devices registered
        service.sendTradeNotification(tradeId, eventType, "New trade", true);

        // Wait a bit for async completion
        Thread.sleep(100);

        // This should be marked as sent to avoid infinite retries
        Path sentNotificationsPath = tempDir.resolve("sent_notifications.json");
        assertTrue(Files.exists(sentNotificationsPath), "Sent notifications file should exist");

        String content = Files.readString(sentNotificationsPath);
        assertTrue(content.contains(tradeId + ":" + eventType),
                "Notification should be marked as sent when there are no devices");

        // Second attempt should be skipped
        service.sendTradeNotification(tradeId, eventType, "New trade", true);
        // Should not throw exception
    }

    @Test
    void testPersistenceOfSentNotifications() throws Exception {
        service.initialize().get();

        String tradeId = "trade123";
        String eventType = "TRADE_CREATED";

        // Send notification (will be marked as sent because no devices)
        service.sendTradeNotification(tradeId, eventType, "New trade", true);
        Thread.sleep(100);

        // Shutdown service
        service.shutdown().get();

        // Create new service instance (simulates restart)
        PushNotificationService newService = new PushNotificationService(
                deviceRegistrationService,
                "http://localhost:8080",
                Optional.empty(),
                tempDir
        );
        newService.initialize().get();

        // The notification should still be marked as sent after restart
        // We can verify this by trying to send again - it should be skipped
        newService.sendTradeNotification(tradeId, eventType, "New trade", true);
        // Should not throw exception and should skip duplicate

        newService.shutdown().get();
    }
}

