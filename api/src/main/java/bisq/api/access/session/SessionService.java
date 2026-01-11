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

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SessionService {
    private final Map<String, SessionToken> sessionTokenBySessionIdMap = new ConcurrentHashMap<>();

    public SessionToken createSession(UUID deviceId) {
        SessionToken token = new SessionToken(deviceId);
        sessionTokenBySessionIdMap.put(token.getSessionId(), token);
        return token;
    }

    public Optional<SessionToken> find(String sessionId) {
        return Optional.ofNullable(sessionTokenBySessionIdMap.get(sessionId));
    }
}
