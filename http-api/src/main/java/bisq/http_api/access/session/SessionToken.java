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

package bisq.http_api.access.session;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode
public class SessionToken {
    private final long ttlInMinutes;

    @Getter
    private final String sessionId;
    @Getter
    private final String clientId;
    @Getter
    private final Instant expiresAt;

    public SessionToken(int ttlInMinutes, String clientId) {
        this.ttlInMinutes = ttlInMinutes;
        this.sessionId = UUID.randomUUID().toString();
        this.clientId = clientId;
        this.expiresAt = Instant.now().plusSeconds(ttlInMinutes * 60L);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
