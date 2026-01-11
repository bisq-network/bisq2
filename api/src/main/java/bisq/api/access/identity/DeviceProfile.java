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

package bisq.api.access.identity;

import lombok.*;

import java.security.PublicKey;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class DeviceProfile {
    private final UUID deviceId;
    private final String deviceName;
    private final PublicKey publicKey;

    public DeviceProfile(UUID deviceId, String deviceName, PublicKey publicKey) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.publicKey = publicKey;
    }
}