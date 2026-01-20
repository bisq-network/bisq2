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
        this.devicesByUserProfileId = new ConcurrentHashMap<>(devicesByUserProfileId);
    }

    @JsonIgnore
    public DeviceRegistrationStore getClone() {
        return new DeviceRegistrationStore(new ConcurrentHashMap<>(devicesByUserProfileId));
    }

    public void applyPersisted(DeviceRegistrationStore persisted) {
        devicesByUserProfileId.clear();
        devicesByUserProfileId.putAll(persisted.devicesByUserProfileId);
    }
}

