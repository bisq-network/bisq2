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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores device registrations mapped by device ID.
 * Each device ID maps to a single MobileDeviceProfile.
 *
 * Thread-safety: Uses ConcurrentHashMap for safe concurrent access and modifications.
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeviceRegistrationStore implements Serializable {
    private static final long serialVersionUID = 2L;

    @Getter
    private final Map<String, MobileDeviceProfile> deviceByDeviceId;

    public DeviceRegistrationStore() {
        this(new ConcurrentHashMap<>());
    }

    @JsonCreator
    public DeviceRegistrationStore(@JsonProperty("deviceByDeviceId") Map<String, MobileDeviceProfile> deviceByDeviceId) {
        // Create new ConcurrentHashMap and copy all entries
        this.deviceByDeviceId = new ConcurrentHashMap<>();
        if (deviceByDeviceId != null) {
            this.deviceByDeviceId.putAll(deviceByDeviceId);
        }
    }

    @JsonIgnore
    public DeviceRegistrationStore getClone() {
        return new DeviceRegistrationStore(new ConcurrentHashMap<>(deviceByDeviceId));
    }

    public void applyPersisted(DeviceRegistrationStore persisted) {
        deviceByDeviceId.clear();
        deviceByDeviceId.putAll(persisted.deviceByDeviceId);
    }
}

