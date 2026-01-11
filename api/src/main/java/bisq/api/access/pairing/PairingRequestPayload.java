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

package bisq.api.access.pairing;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.PublicKey;
import java.time.Instant;

@Getter
@EqualsAndHashCode
@ToString
public final class PairingRequestPayload {
    public static final byte VERSION = 1;

    private final String pairingCodeId;
    private final PublicKey devicePublicKey;
    private final String deviceName;
    private final Instant timestamp;

    public PairingRequestPayload(String pairingCodeId,
                                 PublicKey devicePublicKey,
                                 String deviceName,
                                 Instant timestamp) {
        this.pairingCodeId = pairingCodeId;
        this.devicePublicKey = devicePublicKey;
        this.deviceName = deviceName;
        this.timestamp = timestamp;
    }
}
