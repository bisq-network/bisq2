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

    /**
     * Register a device for a user profile.
     * <p>
     * This method is thread-safe: it atomically removes any existing registration with
     * the same device token and adds the new registration, all within a single compute
     * operation to prevent races with concurrent register/unregister calls.
     *
     * @param userProfileId The user profile ID
     * @param deviceToken   The APNs device token
     * @param publicKey     The public key for encrypting notifications (Base64 encoded)
     * @param platform      The platform (iOS, Android)
     * @return true if registration was successful
     */
    public boolean registerDevice(String userProfileId,
                                  String deviceToken,
                                  String publicKey,
                                  DeviceRegistrationPlatform platform) {
        checkArgument(StringUtils.isEmpty(userProfileId), "userProfileId must not be null or empty");
        checkArgument(StringUtils.isEmpty(deviceToken), "deviceToken must not be null or empty");
        checkArgument(StringUtils.isEmpty(publicKey), "publicKey must not be null or empty");
        checkNotNull(platform, "platform must not be null");

        String tokenPreview = deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...";
        String publicKeyPreview = publicKey.substring(0, Math.min(20, publicKey.length())) + "...";

        log.info("Registering device - userProfileId: {}, token: {}, publicKey: {}, platform: {}",
                userProfileId, tokenPreview, publicKeyPreview, platform);

        DeviceRegistration registration = new DeviceRegistration(deviceToken, publicKey, platform);

        // Use AtomicBoolean and AtomicInteger to capture state from inside the compute lambda
        AtomicBoolean wasAdded = new AtomicBoolean(false);
        AtomicBoolean hadExisting = new AtomicBoolean(false);
        AtomicInteger deviceCountBefore = new AtomicInteger(0);
        AtomicInteger deviceCountAfter = new AtomicInteger(0);

        // Atomically remove existing registration and add new one
        persistableStore.getDevicesByUserProfileId()
                .compute(userProfileId, (userId, devices) -> {
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
                log.info("✓ Updated device registration for user {}: token={}, platform={}, total devices: {} (was: {})",
                        userProfileId, tokenPreview, platform, deviceCountAfter.get(), deviceCountBefore.get());
            } else {
                log.info("✓ Device registered successfully for user {}: token={}, platform={}, total devices: {} (was: {})",
                        userProfileId, tokenPreview, platform, deviceCountAfter.get(), deviceCountBefore.get());
            }
        } else {
            log.warn("✗ Failed to add device registration for user {}: token={}, platform={}",
                    userProfileId, tokenPreview, platform);
        }

        return wasAdded.get();
    }

    /**
     * Unregister a device for a user profile.
     * <p>
     * This method is thread-safe: it atomically removes the device token from the user's
     * device set and removes the user entry if no devices remain, all within a single
     * compute operation to prevent races with concurrent register/unregister calls.
     *
     * @param userProfileId The user profile ID
     * @param deviceToken   The APNs device token to remove
     * @return true if a device was removed
     */
    public boolean unregisterDevice(String userProfileId, String deviceToken) {
        checkArgument(StringUtils.isEmpty(userProfileId), "userProfileId must not be null or empty");
        checkArgument(StringUtils.isEmpty(deviceToken), "deviceToken must not be null or empty");

        // Use AtomicBoolean to capture whether removal occurred inside the compute lambda
        AtomicBoolean wasRemoved = new AtomicBoolean(false);

        // Atomically remove the device and clean up empty user entries
        persistableStore.getDevicesByUserProfileId()
                .compute(userProfileId, (userId, devices) -> {
                    // No devices for this user
                    if (devices == null) {
                        return null;
                    }

                    // Attempt to remove the device token
                    boolean removed = devices.removeIf(d -> d.getDeviceToken().equals(deviceToken));
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
            log.info("Unregistered device for user {} (tokenLength: {})",
                    userProfileId, deviceToken.length());
        }

        return wasRemoved.get();
    }

    /**
     * Get all registered devices for a user profile.
     *
     * @param userProfileId The user profile ID
     * @return Set of device registrations, or empty set if none found
     */
    public Set<DeviceRegistration> findDeviceRegistrations(String userProfileId) {
        return Optional.ofNullable(persistableStore.getDevicesByUserProfileId().get(userProfileId))
                .map(Set::copyOf)
                .orElse(Collections.emptySet());
    }

    /**
     * Get all registered user profile IDs.
     *
     * @return Set of user profile IDs that have registered devices
     */
    public Set<String> getAllUserProfileIds() {
        return Set.copyOf(persistableStore.getDevicesByUserProfileId().keySet());
    }

    public ReadOnlyObservableMap<String, Set<DeviceRegistration>> getDevicesByUserProfileId() {
        return persistableStore.getDevicesByUserProfileId();
    }
}
