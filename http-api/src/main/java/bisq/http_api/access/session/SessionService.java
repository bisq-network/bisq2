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

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SessionService {
    private final Map<String, SessionToken> sessionTokenBySessionIdMap = new ConcurrentHashMap<>();
    private final int ttlInMinutes;

    public SessionService(int ttlInMinutes) {
        this.ttlInMinutes = ttlInMinutes;
    }

    public SessionToken createSession(String clientId) {
        SessionToken token = new SessionToken(ttlInMinutes, clientId);
        sessionTokenBySessionIdMap.put(token.getSessionId(), token);
        return token;
    }

    public Optional<SessionToken> find(String sessionId) {
        // Use computeIfPresent for atomic check-and-evict of expired tokens (prevents TOCTOU race)
        SessionToken result = sessionTokenBySessionIdMap.computeIfPresent(sessionId, (key, token) -> {
            if (token.isExpired()) {
                return null; // Evict expired token
            }
            return token; // Keep valid token
        });
        return Optional.ofNullable(result);
    }

    /**
     * Explicitly removes a session (e.g., for logout functionality).
     * This prevents memory leaks from accumulated expired sessions.
     *
     * @param sessionId The session ID to remove
     */
    public void remove(String sessionId) {
        sessionTokenBySessionIdMap.remove(sessionId);
    }
}
