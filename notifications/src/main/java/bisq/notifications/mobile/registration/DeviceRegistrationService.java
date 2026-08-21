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
import bisq.common.util.StringUtils;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class DeviceRegistrationService extends RateLimitedPersistenceClient<DeviceRegistrationStore> implements Service {
    @Getter
    private final DeviceRegistrationStore persistableStore = new DeviceRegistrationStore();
    @Getter
    private final Persistence<DeviceRegistrationStore> persistence;
    /**
     * Clients revoked in this process. A registration request that passed authorization before the
     * revocation can resume afterwards and would otherwise recreate a registration owned by a
     * client that no longer exists, which nothing would ever revoke again.
     * <p>
     * Not persisted, and it does not need to be: it only has to outlive in-flight requests. Once
     * the profile is gone, any new request from that client fails authorization, and after a
     * restart there are no in-flight requests. Client IDs are random and never reused, so entries
     * never turn into false rejections.
     */
    private final Set<String> revokedClientIds = ConcurrentHashMap.newKeySet();

    public DeviceRegistrationService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    /**
     * Creates or updates the registration of a device on behalf of the given API client.
     * <p>
     * A device ID is chosen by the client, so without an owner check any client could register
     * another client's device ID, take ownership of the entry and redirect that device's push
     * notifications to its own token. Registrations without an owner predate the ownership link
     * and are claimable, which is how they gain one.
     *
     * @return the outcome of the request; see {@link DeviceRegistrationResult}
     */
    public DeviceRegistrationResult register(String deviceId,
                            String deviceToken,
                            String publicKeyBase64,
                            String deviceDescriptor,
                            MobileDevicePlatform platform,
                            Optional<String> symmetricKeyBase64,
                            String clientId) {
        checkArgument(StringUtils.isNotEmpty(deviceId), "deviceId must not be null or empty");
        checkArgument(StringUtils.isNotEmpty(clientId), "clientId must not be null or empty");
        checkArgument(StringUtils.isNotEmpty(deviceToken), "deviceToken must not be null or empty");
        checkArgument(StringUtils.isNotEmpty(publicKeyBase64), "publicKeyBase64 must not be null or empty");
        checkArgument(StringUtils.isNotEmpty(deviceDescriptor), "deviceDescriptor must not be null or empty");
        checkNotNull(platform, "platform must not be null");

        // Log at DEBUG level to avoid exposing sensitive device identifiers
        log.debug("Registering device - deviceId: {}, deviceDescriptor: {}, platform: {}, hasSymmetricKey: {}",
                deviceId, deviceDescriptor, platform, symmetricKeyBase64.isPresent());
        // Log minimal info at INFO level for monitoring
        log.info("Device registration: platform={}, deviceIdLength={}, descriptorLength={}, hasSymmetricKey={}",
                platform, deviceId.length(), deviceDescriptor.length(), symmetricKeyBase64.isPresent());

        MobileDeviceProfile mobileDeviceProfile = new MobileDeviceProfile(deviceId,
                deviceToken,
                publicKeyBase64,
                deviceDescriptor,
                platform,
                symmetricKeyBase64,
                Optional.of(clientId));
        // The owner check and the write have to be one step: otherwise two clients registering
        // the same new device ID both pass the check and the later write silently takes the
        // device, which is the takeover the check exists to prevent.
        synchronized (persistableStore) {
            // Checked under the same monitor as the revocation removal, so a request that resumes
            // mid-revocation is either removed by it or rejected here, never left behind.
            if (revokedClientIds.contains(clientId)) {
                log.warn("Rejecting device registration of revoked client {}", clientId);
                return DeviceRegistrationResult.CLIENT_REVOKED;
            }

            MobileDeviceProfile existing = persistableStore.getDeviceByDeviceId().get(deviceId);
            if (existing != null && existing.getClientId().filter(owner -> !owner.equals(clientId)).isPresent()) {
                log.warn("Client {} tried to register a device ID owned by another client", clientId);
                return DeviceRegistrationResult.DEVICE_OWNED_BY_ANOTHER_CLIENT;
            }

            persistableStore.getDeviceByDeviceId().put(deviceId, mobileDeviceProfile);
            if (!mobileDeviceProfile.equals(existing)) {
                persist();
            }
            return DeviceRegistrationResult.REGISTERED;
        }
    }

    /**
     * Removes a registration on behalf of the given API client. A client may only remove its own
     * registrations, so a device ID alone is not enough to unregister someone else's device.
     * <p>
     * Registrations persisted before the ownership link existed have no owner and stay removable
     * by any client; they lose that exemption as soon as the owning app registers again and claims
     * them.
     *
     * @param deviceId The device to unregister
     * @param clientId The API client requesting the removal
     * @return {@code true} if a registration was removed; {@code false} if none was found or it is
     * owned by another client
     */
    public boolean unregister(String deviceId, String clientId) {
        checkArgument(StringUtils.isNotEmpty(deviceId), "deviceId must not be null or empty");
        checkArgument(StringUtils.isNotEmpty(clientId), "clientId must not be null or empty");

        synchronized (persistableStore) {
            MobileDeviceProfile profile = persistableStore.getDeviceByDeviceId().get(deviceId);
            if (profile == null) {
                return false;
            }
            Optional<String> owner = profile.getClientId();
            if (owner.isPresent() && !owner.get().equals(clientId)) {
                log.warn("Client {} tried to unregister a device owned by another client", clientId);
                return false;
            }

            persistableStore.getDeviceByDeviceId().remove(deviceId);
            persist();
            return true;
        }
    }

    /**
     * Removes all registrations owned by the given API client. Called when the client is revoked:
     * a revoked client must stop receiving push notifications, not just lose API access.
     * <p>
     * Registrations persisted before the ownership link existed carry no client ID and are not
     * matched here; they have to be removed by device ID.
     *
     * @param clientId The API client whose registrations should be removed
     * @return The device IDs that were removed
     */
    public Set<String> unregisterByClientId(String clientId) {
        checkArgument(StringUtils.isNotEmpty(clientId), "clientId must not be null or empty");

        synchronized (persistableStore) {
            // Recorded before the removal so a concurrent registration of this client is rejected
            // rather than landing after the removal has taken its snapshot.
            revokedClientIds.add(clientId);
            Set<String> deviceIds = persistableStore.getDeviceByDeviceId().values().stream()
                    .filter(profile -> profile.getClientId().filter(clientId::equals).isPresent())
                    .map(MobileDeviceProfile::getDeviceId)
                    .collect(Collectors.toSet());
            if (deviceIds.isEmpty()) {
                return Set.of();
            }
            // Selecting and removing under the same monitor, so a registration claimed by another
            // client in between is not removed on this client's behalf.
            deviceIds.forEach(persistableStore.getDeviceByDeviceId()::remove);
            persist();
            log.info("Removed {} push registration(s) of revoked client {}", deviceIds.size(), clientId);
            return deviceIds;
        }
    }

    /** Snapshot taken under the same monitor as the mutators, so it never straddles a write. */
    public Set<MobileDeviceProfile> getMobileDeviceProfiles() {
        synchronized (persistableStore) {
            return Set.copyOf(persistableStore.getDeviceByDeviceId().values());
        }
    }
}
