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

package bisq.notifications.mobile.registration;

import bisq.common.application.Service;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.common.util.StringUtils;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class DeviceRegistrationService extends RateLimitedPersistenceClient<DeviceRegistrationStore> implements Service {
    @Getter
    private final DeviceRegistrationStore persistableStore = new DeviceRegistrationStore();
    @Getter
    private final Persistence<DeviceRegistrationStore> persistence;

    public DeviceRegistrationService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    public boolean register(String deviceId,
                            String deviceToken,
                            String publicKeyBase64,
                            String deviceDescriptor,
                            MobileDevicePlatform platform) {
        checkArgument(StringUtils.isEmpty(deviceId), "deviceId must not be null or empty");
        checkArgument(StringUtils.isEmpty(deviceToken), "deviceToken must not be null or empty");
        checkArgument(StringUtils.isEmpty(publicKeyBase64), "publicKeyBase64 must not be null or empty");
        checkArgument(StringUtils.isEmpty(deviceDescriptor), "deviceDescriptor must not be null or empty");
        checkNotNull(platform, "platform must not be null");

        String tokenPreview = deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...";
        String publicKeyPreview = publicKeyBase64.substring(0, Math.min(20, publicKeyBase64.length())) + "...";

        log.info("Registering device - deviceId: {}, deviceDescriptor: {}, token: {}, publicKeyBase64: {}, platform: {}",
                deviceId, deviceDescriptor, tokenPreview, publicKeyPreview, platform);

        MobileDeviceProfile registration = new MobileDeviceProfile(deviceId,
                deviceToken,
                publicKeyBase64,
                deviceDescriptor,
                platform);

        // Use AtomicBoolean and AtomicInteger to capture state from inside the compute lambda
        AtomicBoolean wasAdded = new AtomicBoolean(false);
        AtomicBoolean hadExisting = new AtomicBoolean(false);
        AtomicInteger deviceCountBefore = new AtomicInteger(0);
        AtomicInteger deviceCountAfter = new AtomicInteger(0);

        // Atomically remove existing registration and add new one
        persistableStore.getDevicesByDeviceId()
                .compute(deviceToken, (key, devices) -> {
                    // Create new set if user has no devices yet
                    if (devices == null) {
                        devices = ConcurrentHashMap.newKeySet();
                    }

                    deviceCountBefore.set(devices.size());

                    // Remove any existing registration with the same device token to avoid duplicates
                    boolean removed = devices.removeIf(d -> d.getDeviceToken().equals(deviceToken));
                    hadExisting.set(removed);

                    // Add the new registration
                    boolean added = devices.add(registration);
                    wasAdded.set(added);

                    deviceCountAfter.set(devices.size());

                    return devices;
                });

        // Only persist and log if a device was actually added
        if (wasAdded.get()) {
            persist();
            if (hadExisting.get()) {
                log.info("✓ Updated device registration for deviceId {}: token={}, platform={}, total devices: {} (was: {})",
                        deviceId, tokenPreview, platform, deviceCountAfter.get(), deviceCountBefore.get());
            } else {
                log.info("✓ Device registered successfully for deviceId {}: token={}, platform={}, total devices: {} (was: {})",
                        deviceId, tokenPreview, platform, deviceCountAfter.get(), deviceCountBefore.get());
            }
        } else {
            log.warn("✗ Failed to add device registration for deviceId {}: token={}, platform={}",
                    deviceId, tokenPreview, platform);
        }

        return wasAdded.get();
    }

    public boolean unregister(String deviceId) {
        checkArgument(StringUtils.isEmpty(deviceId), "deviceId must not be null or empty");

        // Use AtomicBoolean to capture whether removal occurred inside the compute lambda
        AtomicBoolean wasRemoved = new AtomicBoolean(false);

        // Atomically remove the device and clean up empty user entries
        persistableStore.getDevicesByDeviceId()
                .compute(deviceId, (userId, devices) -> {
                    // No devices for this user
                    if (devices == null) {
                        return null;
                    }

                    // Attempt to remove the device token
                    boolean removed = devices.removeIf(d -> d.getDeviceToken().equals(deviceId));
                    wasRemoved.set(removed);

                    // If removal occurred and no devices remain, remove the user entry entirely
                    if (removed && devices.isEmpty()) {
                        return null;  // Returning null removes the entry from the map
                    }

                    // Return the (possibly modified) device set
                    return devices;
                });

        // Only persist and log if a device was actually removed
        if (wasRemoved.get()) {
            persist();
            log.info("Unregistered device {}", deviceId);
        }

        return wasRemoved.get();
    }

    /**
     * Get all registered devices for a user profile.
     *
     * @param userProfileId The user profile ID
     * @return Set of device registrations, or empty set if none found
     */
    public Set<MobileDeviceProfile> findDeviceRegistrations(String userProfileId) {
        return Optional.ofNullable(persistableStore.getDevicesByDeviceId().get(userProfileId))
                .map(Set::copyOf)
                .orElse(Collections.emptySet());
    }

    /**
     * Get all registered user profile IDs.
     *
     * @return Set of user profile IDs that have registered devices
     */
    public Set<String> getAllUserProfileIds() {
        return Set.copyOf(persistableStore.getDevicesByDeviceId().keySet());
    }

    public ReadOnlyObservableMap<String, Set<MobileDeviceProfile>> getDevicesByUserProfileId() {
        return persistableStore.getDevicesByDeviceId();
    }
}
