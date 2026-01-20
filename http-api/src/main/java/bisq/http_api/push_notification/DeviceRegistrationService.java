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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing device registrations for push notifications.
 * Handles registration, unregistration, and lookup of devices by user profile ID.
 */
@Slf4j
public class DeviceRegistrationService implements Service {
    private static final String STORE_FILE_NAME = "device_registrations.json";

    @Getter
    private final DeviceRegistrationStore persistableStore;
    private final Path storePath;
    private final ObjectMapper objectMapper;

    private final Object persistLock = new Object(); // Lock for thread-safe persistence

    public DeviceRegistrationService(Path dataDir) {
        this.storePath = dataDir.resolve(STORE_FILE_NAME);
        this.objectMapper = new ObjectMapper();
        this.persistableStore = loadStore();
    }

    private DeviceRegistrationStore loadStore() {
        if (Files.exists(storePath)) {
            try {
                log.info("Loading device registrations from {}", storePath);
                DeviceRegistrationStore store = objectMapper.readValue(storePath.toFile(), DeviceRegistrationStore.class);
                int userCount = store.getDevicesByUserProfileId().size();
                int totalDevices = store.getDevicesByUserProfileId().values().stream()
                        .mapToInt(Set::size)
                        .sum();
                log.info("✓ Loaded {} user profiles with {} total devices from {}",
                        userCount, totalDevices, storePath);
                return store;
            } catch (IOException e) {
                log.warn("✗ Failed to load device registrations from {} - starting with empty store. Error: {}",
                        storePath, e.getMessage());
                log.debug("Full error details:", e); // Only show stack trace in debug mode
                log.info("The file will be overwritten on next device registration");
            }
        } else {
            log.info("No existing device registrations file found at {} - starting with empty store", storePath);
        }
        return new DeviceRegistrationStore();
    }

    private void persist() {
        synchronized (persistLock) {
            try {
                Files.createDirectories(storePath.getParent());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), persistableStore);
            } catch (IOException e) {
                log.error("Failed to persist device registrations to {}", storePath, e);
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("DeviceRegistrationService initialized with {} user profiles",
                persistableStore.getDevicesByUserProfileId().size());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        persist();
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Register a device for a user profile.
     *
     * @param userProfileId The user profile ID
     * @param deviceToken   The APNs device token
     * @param publicKey     The public key for encrypting notifications (Base64 encoded)
     * @param platform      The platform (iOS, Android)
     * @return true if registration was successful
     */
    public boolean registerDevice(String userProfileId, String deviceToken, String publicKey,
                                   DeviceRegistration.Platform platform) {
        if (userProfileId == null || userProfileId.isBlank() ||
                deviceToken == null || deviceToken.isBlank() ||
                publicKey == null || publicKey.isBlank()) {
            log.warn("Invalid registration parameters - userProfileId: {}, deviceToken: {}, publicKey: {}, platform: {}",
                    userProfileId != null && !userProfileId.isBlank() ? "OK" : "INVALID",
                    deviceToken != null && !deviceToken.isBlank() ? "OK" : "INVALID",
                    publicKey != null && !publicKey.isBlank() ? "OK" : "INVALID",
                    platform);
            return false;
        }

        String tokenPreview = deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...";
        String publicKeyPreview = publicKey.substring(0, Math.min(20, publicKey.length())) + "...";

        log.info("Registering device - userProfileId: {}, token: {}, publicKey: {}, platform: {}",
                userProfileId, tokenPreview, publicKeyPreview, platform);

        DeviceRegistration registration = new DeviceRegistration(deviceToken, publicKey, platform);
        Set<DeviceRegistration> devices = persistableStore.getDevicesByUserProfileId()
                .computeIfAbsent(userProfileId, k -> new HashSet<>());

        int deviceCountBefore = devices.size();

        // Remove any existing registration with the same device token to avoid duplicates
        boolean hadExisting = devices.removeIf(d -> d.getDeviceToken().equals(deviceToken));
        if (hadExisting) {
            log.info("Removed existing registration with same token for user {}", userProfileId);
        }

        boolean added = devices.add(registration);
        if (added) {
            persist();
            log.info("✓ Device registered successfully for user {}: token={}, platform={}, total devices for user: {} (was: {})",
                    userProfileId, tokenPreview, platform, devices.size(), deviceCountBefore);
        } else {
            log.warn("✗ Failed to add device registration for user {}: token={}, platform={}",
                    userProfileId, tokenPreview, platform);
        }
        return added;
    }

    /**
     * Unregister a device for a user profile.
     *
     * @param userProfileId The user profile ID
     * @param deviceToken   The APNs device token to remove
     * @return true if a device was removed
     */
    public boolean unregisterDevice(String userProfileId, String deviceToken) {
        if (userProfileId == null || deviceToken == null) {
            return false;
        }

        Set<DeviceRegistration> devices = persistableStore.getDevicesByUserProfileId().get(userProfileId);
        if (devices == null) {
            return false;
        }

        boolean removed = devices.removeIf(d -> d.getDeviceToken().equals(deviceToken));
        if (removed) {
            if (devices.isEmpty()) {
                persistableStore.getDevicesByUserProfileId().remove(userProfileId);
            }
            persist();
            log.info("Unregistered device for user {}: token={}",
                    userProfileId, deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...");
        }
        return removed;
    }

    /**
     * Get all registered devices for a user profile.
     *
     * @param userProfileId The user profile ID
     * @return Set of device registrations, or empty set if none found
     */
    public Set<DeviceRegistration> getDevicesForUser(String userProfileId) {
        Set<DeviceRegistration> devices = persistableStore.getDevicesByUserProfileId().get(userProfileId);
        return devices != null ? new HashSet<>(devices) : Collections.emptySet();
    }

    /**
     * Get all registered user profile IDs.
     *
     * @return Set of user profile IDs that have registered devices
     */
    public Set<String> getAllUserProfileIds() {
        return new HashSet<>(persistableStore.getDevicesByUserProfileId().keySet());
    }
}

