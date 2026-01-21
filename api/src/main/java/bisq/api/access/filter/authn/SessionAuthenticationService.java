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

package bisq.api.access.filter.authn;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public final class SessionAuthenticationService {
    private final PairingService pairingService;
    private final SessionService sessionService;

    public SessionAuthenticationService(PairingService pairingService, SessionService sessionService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
    }


    public AuthenticatedSession authenticate(
            String clientId,
            String sessionId
    ) throws AuthenticationException {
        try {
            checkNotNull(sessionId, "Missing sessionId");
            checkNotNull(clientId, "Missing clientId");

            //todo
            if (sessionService.find(sessionId).isEmpty()) {
                sessionService.createSession(clientId);
            }
            SessionToken session = sessionService.find(sessionId)
                    .orElseThrow(() -> new AuthenticationException("Invalid session"));

            if (session.isExpired()) {
                throw new AuthenticationException("Session expired");
            }

            checkArgument(clientId.equals(session.getClientId()),
                    "ClientId from header not matching client ID from session");

            ClientProfile deviceProfile = pairingService.findDeviceProfile(session.getClientId())
                    .orElseThrow(() -> new AuthenticationException("Unknown device"));

            return new AuthenticatedSession(
                    session.getSessionId(),
                    session.getClientId()
            );
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }
}
