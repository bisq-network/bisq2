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
import bisq.api.access.pairing.PairingResponse;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.InvalidSessionRequestException;
import bisq.api.access.session.SessionResponse;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import bisq.api.web_socket.WebSocketService;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Slf4j

public class ApiAccessService {

    private final PairingService pairingService;
    private final SessionService sessionService;
    // Empty where no WebSocket server runs (node-monitor), so there is no connection to end.
    private final Optional<WebSocketService> webSocketService;

    public ApiAccessService(PairingService pairingService,
                            SessionService sessionService,
                            Optional<WebSocketService> webSocketService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
        this.webSocketService = webSocketService;
    }

    public PairingResponse requestPairing(byte version,
                                          String pairingCodeId,
                                          String clientName) throws InvalidPairingRequestException {
        ClientProfile clientProfile = pairingService.requestPairing(version, pairingCodeId, clientName);
        String clientSecret = clientProfile.getClientSecret();
        String clientId = clientProfile.getClientId();
        SessionToken sessionToken = sessionService.createSession(clientId);
        long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
        return new PairingResponse(clientId, clientSecret, sessionToken.getSessionId(), expiresAt);
    }

    /**
     * Revokes a paired client by invalidating all its active sessions, removing its stored profile
     * and permissions, and closing its WebSocket connection. After revocation the client can no
     * longer authenticate and must go through the pairing flow again.
     * <p>
     * Closing the connection is part of the revocation and not a courtesy. A subscription is
     * authorised once, when it is taken out: a {@code Subscriber} holds a topic and a socket, never
     * a clientId, so nothing re-checks the grant on the way out and there is no way to drop one
     * client's subscriptions by name. Leave the socket open and the revoked client keeps receiving
     * everything it had already subscribed to, indefinitely — it is only listening, so nothing else
     * would ever make that connection fail. Closing it is what makes the repository forget those
     * subscribers, through {@code SubscriptionService#onConnectionClosed}.
     * <p>
     * The order is load-bearing: the grant goes before the socket. Closing only moves the socket to
     * CLOSING, and until the close frame is flushed it still accepts sends. A subscription that
     * passed its check before this call re-reads the grant right after adding its subscriber
     * ({@code SubscriptionService#subscribe}) so that it is refused instead of being served its
     * snapshot on that still-sending socket, and that read only finds the grant gone because it
     * was removed first.
     *
     * @param clientId The client ID to revoke
     * @return {@code true} if the client was found and revoked; {@code false} if not found
     */
    public boolean revokeClient(String clientId) {
        boolean removed = pairingService.revokeClientProfile(clientId);
        sessionService.removeSessionByClientId(clientId);
        // Unconditional, like the session removal above: the profile may already be gone while the
        // connection it was paired with is still open and still being pushed to.
        webSocketService.ifPresent(webSocket -> webSocket.disconnectClient(clientId));
        if (removed) {
            log.info("Revoked client {}", clientId);
        } else {
            log.warn("Client profile not found for {}, but session and connection were still cleaned up", clientId);
        }
        return removed;
    }

    public SessionResponse requestSession(String clientId, String clientSecret) throws InvalidSessionRequestException {
        ClientProfile clientProfile = pairingService.findClientProfile(clientId)
                .orElseThrow(() -> new InvalidSessionRequestException("No client profile found for Client ID"));

        if (!MessageDigest.isEqual(
                                clientSecret.getBytes(StandardCharsets.UTF_8),
                                clientProfile.getClientSecret().getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidSessionRequestException("Client secret is not matching");
        }

        SessionToken sessionToken = sessionService.createSession(clientId);
        long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
        return new SessionResponse(sessionToken.getSessionId(), expiresAt);
    }
}
