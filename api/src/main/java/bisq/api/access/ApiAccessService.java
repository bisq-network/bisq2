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

package bisq.api.access;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.InvalidPairingRequestException;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingResponse;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.InvalidSessionRequestException;
import bisq.api.access.session.SessionRequest;
import bisq.api.access.session.SessionResponse;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class ApiAccessService {
    private final PairingService pairingService;
    private final SessionService sessionService;

    public ApiAccessService(PairingService pairingService,
                            SessionService sessionService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
    }

    public PairingResponse handlePairingRequest(PairingRequest request) throws InvalidPairingRequestException {
        ClientProfile deviceProfile = pairingService.pairDevice(request);
        String clientSecret = deviceProfile.getClientSecret();
        String clientId = deviceProfile.getClientId();
        SessionToken sessionToken = sessionService.createSession(clientId);
        long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
        return new PairingResponse(clientId,clientSecret, sessionToken.getSessionId(), expiresAt);
    }

    public SessionResponse handleSessionRequest(SessionRequest request) throws InvalidSessionRequestException {
        String clientId = request.getClientId();
        ClientProfile deviceProfile = pairingService.findDeviceProfile(clientId)
                .orElseThrow(() -> new InvalidSessionRequestException("No client profile found for Client ID"));
        String clientSecret = deviceProfile.getClientSecret();

        if (!clientSecret.equals(request.getClientSecret())) {
            throw new InvalidSessionRequestException("Client secret is not matching");
        }

        SessionToken sessionToken = sessionService.createSession(clientId);
        long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
        return new SessionResponse(sessionToken.getSessionId(), expiresAt);
    }
}
