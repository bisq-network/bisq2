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

package bisq.api.access.session;

import bisq.common.util.ByteArrayUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@EqualsAndHashCode
public class SessionToken {
    public static final long TTL = TimeUnit.MINUTES.toSeconds(15);

    private final String sessionId;
    private final UUID deviceId;
    private final Instant expiresAt;
    private final byte[] hmacKey;

    public SessionToken(UUID deviceId) {
        this.sessionId = UUID.randomUUID().toString();
        this.deviceId = deviceId;
        this.expiresAt = Instant.now().plusSeconds(TTL);
        hmacKey = ByteArrayUtils.getRandomBytes(32); // 256-bit
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
