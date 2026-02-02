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

package bisq.http_api.push_notification;

import bisq.common.application.Service;
import bisq.network.NetworkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for sending push notifications to registered devices.
 * Coordinates between device registrations and the Bisq Relay client.
 * Tracks sent notifications to prevent duplicates on restart.
 * Automatically purges old notification records to prevent unbounded growth.
 */
@Slf4j
public class PushNotificationService implements Service {
    private static final String STORE_FILE_NAME = "sent_notifications.json";
    // Keep notification records for 90 days (same as bisq2's trade data redaction period)
    private static final long MAX_NOTIFICATION_AGE_MS = 90L * 24 * 60 * 60 * 1000; // 90 days

    private final DeviceRegistrationService deviceRegistrationService;
    private final BisqRelayClient bisqRelayClient;
    private final SentNotificationStore sentNotificationStore;
    private final Path storePath;
    private final ObjectMapper objectMapper;
    private final Object persistLock = new Object(); // Lock for thread-safe persistence

    // Guard against concurrent sends of the same notification (race between wasNotificationSent and markNotificationAsSent)
    private final Set<String> inFlightNotifications = ConcurrentHashMap.newKeySet();

    public PushNotificationService(DeviceRegistrationService deviceRegistrationService,
                                   String relayBaseUrl,
                                   Optional<NetworkService> networkService,
                                   Path dataDir) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.bisqRelayClient = new BisqRelayClient(relayBaseUrl, networkService);
        this.storePath = dataDir.resolve(STORE_FILE_NAME);
        this.objectMapper = new ObjectMapper();
        this.sentNotificationStore = loadStore();
    }

    private SentNotificationStore loadStore() {
        if (Files.exists(storePath)) {
            try {
                log.info("Loading sent notifications from {}", storePath);
                SentNotificationStore store = objectMapper.readValue(storePath.toFile(), SentNotificationStore.class);
                log.info("✓ Loaded {} sent notification IDs from {}",
                        store.size(), storePath);

                // Purge old notifications on startup to keep the store size manageable
                int purged = store.purgeOldNotifications(MAX_NOTIFICATION_AGE_MS);
                if (purged > 0) {
                    log.info("Purged {} old notification(s) on startup. Remaining: {}", purged, store.size());
                    persist(store); // Save the purged store
                }

                return store;
            } catch (IOException e) {
                log.warn("✗ Failed to load sent notifications from {} - starting with empty store. Error: {}",
                        storePath, e.getMessage());
                log.debug("Full error details:", e); // Only show stack trace in debug mode
                log.info("The file will be recreated as notifications are sent");
            }
        } else {
            log.info("No existing sent notifications file found at {} - starting with empty store", storePath);
        }
        return new SentNotificationStore();
    }

    /**
     * Thread-safe persistence of the notification store.
     * Uses synchronization to prevent concurrent writes that could corrupt the file.
     */
    private void persist() {
        persist(sentNotificationStore);
    }

    /**
     * Thread-safe persistence of the notification store.
     * Uses synchronization to prevent concurrent writes that could corrupt the file.
     *
     * @param store The store instance to persist (allows persisting during initialization)
     */
    private void persist(SentNotificationStore store) {
        synchronized (persistLock) {
            try {
                Files.createDirectories(storePath.getParent());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), store);
                log.trace("Persisted {} notification records to {}", store.size(), storePath);
            } catch (IOException e) {
                log.error("Failed to persist sent notifications to {}", storePath, e);
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("PushNotificationService initialized with relay URL: {} and {} sent notifications tracked",
                bisqRelayClient, sentNotificationStore.size());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        persist();
        bisqRelayClient.shutdown();
        log.info("PushNotificationService shutdown complete");
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Send a trade event notification to all registered mobile devices.
     * Checks if the notification was already sent to prevent duplicates.
     *
     * @param tradeId   The trade ID
     * @param eventType The type of trade event
     * @param message   A human-readable message
     * @param isUrgent  Whether the notification is urgent
     */
    public void sendTradeNotification(String tradeId, String eventType,
                                       String message, boolean isUrgent) {
        // Create unique key for this notification (no longer tied to userProfileId)
        String notificationKey = tradeId + ":" + eventType;

        // Atomically check-and-add to prevent concurrent sends of the same notification
        if (!inFlightNotifications.add(notificationKey)) {
            log.debug("Skipping in-flight notification for trade {} event {}", tradeId, eventType);
            return;
        }

        // Check if this notification was already sent (use empty string for userProfileId for backward compat)
        if (sentNotificationStore.wasNotificationSent("", tradeId, eventType)) {
            log.debug("Skipping duplicate notification for trade {} event {}", tradeId, eventType);
            inFlightNotifications.remove(notificationKey);
            return;
        }

        Set<MobileDeviceProfile> devices = deviceRegistrationService.getMobileDeviceProfiles();

        if (devices.isEmpty()) {
            log.debug("No devices registered");
            sentNotificationStore.markNotificationAsSent("", tradeId, eventType);
            persist();
            inFlightNotifications.remove(notificationKey);
            return;
        }

        // Create notification payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "trade_event");
        payload.put("tradeId", tradeId);
        payload.put("eventType", eventType);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        // Send to all registered devices
        List<CompletableFuture<NotificationResult>> futures = new ArrayList<>();
        for (MobileDeviceProfile device : devices) {
            CompletableFuture<NotificationResult> future = bisqRelayClient.sendNotification(
                    device.getDeviceToken(),
                    device.getPublicKeyBase64(),
                    payload,
                    isUrgent
            );
            futures.add(future);
        }

        // Wait for all notifications to complete, then process results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, throwable) -> {
                    try {
                        // Process results and auto-unregister bad devices
                        List<NotificationResult> results = futures.stream()
                                .map(f -> f.getNow(null))
                                .filter(r -> r != null)
                                .toList();

                        // Auto-unregister devices that should be removed (find by deviceToken)
                        results.stream()
                                .filter(NotificationResult::isShouldUnregister)
                                .forEach(result -> {
                                    // Find the device by token and unregister by deviceId
                                    devices.stream()
                                            .filter(d -> d.getDeviceToken().equals(result.getDeviceToken()))
                                            .findFirst()
                                            .ifPresent(device -> {
                                                log.warn("Auto-unregistering invalid device: deviceIdLength={}",
                                                        device.getDeviceId().length());
                                                deviceRegistrationService.unregister(device.getDeviceId());
                                            });
                                });

                        // Check if at least one notification succeeded
                        boolean anySuccess = results.stream()
                                .anyMatch(NotificationResult::isSuccess);

                        if (anySuccess) {
                            sentNotificationStore.markNotificationAsSent("", tradeId, eventType);
                            persist();
                            log.info("✓ Sent trade notification to {} device(s) for trade {} event {} - marked as sent",
                                    devices.size(), tradeId, eventType);
                        } else {
                            log.warn("✗ All notifications failed for trade {} event {} - will retry on next trigger",
                                    tradeId, eventType);
                        }
                    } finally {
                        inFlightNotifications.remove(notificationKey);
                    }
                });
    }

    /**
     * Send a trade event notification for a specific trade state change.
     *
     * @param tradeId   The trade ID
     * @param eventType The type of trade event
     */
    public void sendTradeStateChangeNotification(String tradeId, String eventType) {
        String message = formatTradeEventMessage(eventType);
        boolean isUrgent = isUrgentEvent(eventType);
        sendTradeNotification(tradeId, eventType, message, isUrgent);
    }

    /**
     * Remove all notification records for a specific trade.
     * Should be called when a trade is removed/closed.
     *
     * @param tradeId The trade ID
     */
    public void removeNotificationsForTrade(String tradeId) {
        sentNotificationStore.removeNotificationsForTrade(tradeId);
        persist();
        log.debug("Removed notification records for trade {}", tradeId);
    }

    private String formatTradeEventMessage(String eventType) {
        return switch (eventType) {
            case "TRADE_TAKEN" -> "Your offer was taken";
            case "PAYMENT_SENT" -> "Payment sent";
            case "PAYMENT_RECEIVED" -> "Payment received";
            case "TRADE_COMPLETED" -> "Trade completed";
            case "TRADE_FAILED" -> "Trade failed";
            case "MESSAGE_RECEIVED" -> "New trade message";
            default -> "Trade update: " + eventType;
        };
    }

    private boolean isUrgentEvent(String eventType) {
        return switch (eventType) {
            case "TRADE_TAKEN", "PAYMENT_RECEIVED", "TRADE_FAILED" -> true;
            default -> false;
        };
    }
}

