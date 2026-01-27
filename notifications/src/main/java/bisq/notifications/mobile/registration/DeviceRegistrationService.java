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

import java.util.Set;

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

    public void register(String deviceId,
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

        MobileDeviceProfile mobileDeviceProfile = new MobileDeviceProfile(deviceId,
                deviceToken,
                publicKeyBase64,
                deviceDescriptor,
                platform);
        persistableStore.getDeviceByDeviceId().putIfAbsent(deviceId, mobileDeviceProfile);
        persist();
    }

    public boolean unregister(String deviceId) {
        checkArgument(StringUtils.isEmpty(deviceId), "deviceId must not be null or empty");

        MobileDeviceProfile previous = persistableStore.getDeviceByDeviceId().remove(deviceId);
        boolean hadValue = previous != null;
        if (hadValue) {
            persist();
        }
        return hadValue;
    }

    public Set<MobileDeviceProfile> getMobileDeviceProfiles() {
        return Set.copyOf(persistableStore.getDeviceByDeviceId().values());
    }
}
