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

package bisq.api.access.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Just for dev testing and mocks. Would be in the mobile client
 */
@Getter
@EqualsAndHashCode
@ToString
public class ClientIdentity {
    private final String deviceName;
    private final KeyPair keyPair;

    public ClientIdentity(String deviceName, KeyPair keyPair) {
        this.deviceName = deviceName;
        this.keyPair = keyPair;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}

