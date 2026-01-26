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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores device registrations mapped by user profile ID.
 * Each user can have multiple devices registered.
 *
 * Thread-safety: Uses ConcurrentHashMap for the outer map and thread-safe sets
 * (ConcurrentHashMap.newKeySet()) for the inner device sets to ensure safe
 * concurrent access and modifications.
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeviceRegistrationStore implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    private final Map<String, Set<DeviceRegistration>> devicesByUserProfileId;

    public DeviceRegistrationStore() {
        this(new ConcurrentHashMap<>());
    }

    @JsonCreator
    public DeviceRegistrationStore(@JsonProperty("devicesByUserProfileId") Map<String, Set<DeviceRegistration>> devicesByUserProfileId) {
        // Deep copy: create new ConcurrentHashMap and deep-copy each Set to avoid shared mutable state
        this.devicesByUserProfileId = new ConcurrentHashMap<>();
        devicesByUserProfileId.forEach((userId, devices) -> {
            // Create a new thread-safe set and copy all devices
            Set<DeviceRegistration> devicesCopy = ConcurrentHashMap.newKeySet();
            devicesCopy.addAll(devices);
            this.devicesByUserProfileId.put(userId, devicesCopy);
        });
    }

    @JsonIgnore
    public DeviceRegistrationStore getClone() {
        // Deep copy: create new store with deep-copied sets
        Map<String, Set<DeviceRegistration>> clonedMap = new ConcurrentHashMap<>();
        devicesByUserProfileId.forEach((userId, devices) -> {
            // Create a new thread-safe set and copy all devices
            Set<DeviceRegistration> devicesCopy = ConcurrentHashMap.newKeySet();
            devicesCopy.addAll(devices);
            clonedMap.put(userId, devicesCopy);
        });
        return new DeviceRegistrationStore(clonedMap);
    }

    public void applyPersisted(DeviceRegistrationStore persisted) {
        devicesByUserProfileId.clear();
        // Deep copy: don't share Set instances with the persisted store
        persisted.devicesByUserProfileId.forEach((userId, devices) -> {
            // Create a new thread-safe set and copy all devices
            Set<DeviceRegistration> devicesCopy = ConcurrentHashMap.newKeySet();
            devicesCopy.addAll(devices);
            devicesByUserProfileId.put(userId, devicesCopy);
        });
    }
}

