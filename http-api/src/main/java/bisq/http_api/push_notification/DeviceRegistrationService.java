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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing device registrations for push notifications.
 * Handles registration, unregistration, and lookup of devices by device ID.
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
                int deviceCount = store.getDeviceByDeviceId().size();
                log.info("✓ Loaded {} registered devices from {}", deviceCount, storePath);
                return store;
            } catch (IOException e) {
                log.warn("✗ Failed to load device registrations from {} - starting with empty store. Error: {}",
                        storePath, e.getMessage());
                log.debug("Full error details:", e);
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
        log.info("DeviceRegistrationService initialized with {} registered devices",
                persistableStore.getDeviceByDeviceId().size());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        persist();
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Register a mobile device for push notifications.
     *
     * @param deviceId         Unique device identifier
     * @param deviceToken      The APNs/FCM device token
     * @param publicKeyBase64  The public key for encrypting notifications (Base64 encoded)
     * @param deviceDescriptor Human-readable device description (e.g., "iPhone 15 Pro")
     * @param platform         The platform (iOS, Android)
     */
    public void register(String deviceId,
                         String deviceToken,
                         String publicKeyBase64,
                         String deviceDescriptor,
                         MobileDevicePlatform platform) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be null or empty");
        }
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new IllegalArgumentException("deviceToken must not be null or empty");
        }
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new IllegalArgumentException("publicKeyBase64 must not be null or empty");
        }
        if (deviceDescriptor == null || deviceDescriptor.isBlank()) {
            throw new IllegalArgumentException("deviceDescriptor must not be null or empty");
        }
        if (platform == null) {
            throw new IllegalArgumentException("platform must not be null");
        }

        // Log at DEBUG level to avoid exposing sensitive device identifiers
        log.debug("Registering device - deviceId: {}, deviceDescriptor: {}, platform: {}",
                deviceId, deviceDescriptor, platform);
        // Log minimal info at INFO level for monitoring
        log.info("Device registration: platform={}, deviceIdLength={}, descriptorLength={}",
                platform, deviceId.length(), deviceDescriptor.length());

        MobileDeviceProfile mobileDeviceProfile = new MobileDeviceProfile(
                deviceId,
                deviceToken,
                publicKeyBase64,
                deviceDescriptor,
                platform
        );

        MobileDeviceProfile previous = persistableStore.getDeviceByDeviceId().put(deviceId, mobileDeviceProfile);
        if (previous == null || !previous.equals(mobileDeviceProfile)) {
            persist();
            if (previous == null) {
                log.info("✓ New device registered: platform={}, descriptor={}", platform, deviceDescriptor);
            } else {
                log.info("✓ Device registration updated: platform={}, descriptor={}", platform, deviceDescriptor);
            }
        }
    }

    /**
     * Unregister a device by its device ID.
     *
     * @param deviceId The device ID to unregister
     * @return true if a device was removed
     */
    public boolean unregister(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be null or empty");
        }

        MobileDeviceProfile previous = persistableStore.getDeviceByDeviceId().remove(deviceId);
        boolean hadValue = previous != null;
        if (hadValue) {
            persist();
            log.info("✓ Device unregistered: deviceIdLength={}", deviceId.length());
        }
        return hadValue;
    }

    /**
     * Get all registered mobile device profiles.
     *
     * @return Set of all registered mobile device profiles
     */
    public Set<MobileDeviceProfile> getMobileDeviceProfiles() {
        return Set.copyOf(persistableStore.getDeviceByDeviceId().values());
    }
}

