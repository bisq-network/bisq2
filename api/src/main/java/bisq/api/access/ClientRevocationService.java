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

import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Defines what revoking a paired client means, as the single place every caller goes through.
 * <p>
 * The state a client accumulates is spread over several owners: its profile and permissions in
 * {@link PairingService}, its sessions in {@link SessionService}, and its connections and push
 * registrations behind {@link ClientRevocationHandler}s. None of those owners can revoke on its
 * own without depending on the others, so the policy lives here rather than in any of them.
 */
@Slf4j
public class ClientRevocationService {
    private final PairingService pairingService;
    private final SessionService sessionService;
    private final List<ClientRevocationHandler> revocationHandlers;

    public ClientRevocationService(PairingService pairingService,
                                   SessionService sessionService,
                                   List<ClientRevocationHandler> revocationHandlers) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
        this.revocationHandlers = List.copyOf(revocationHandlers);
    }

    /**
     * Revokes a paired client: its stored profile and permissions are removed, all its sessions
     * are invalidated and every {@link ClientRevocationHandler} runs, which closes its live
     * WebSocket connections and drops its push notification registrations. After revocation the
     * client can no longer authenticate and must go through the pairing flow again.
     * <p>
     * Session removal alone is not sufficient: an established WebSocket is authenticated at the
     * handshake only, so without the disconnect the revoked client keeps receiving data until its
     * socket dies, and push registrations are keyed by device rather than by session.
     * <p>
     * All cleanup runs even when no profile was found, as the profile may have been removed
     * already while a session, connection or push registration is still alive. A failing handler
     * is logged and does not stop the remaining ones, so one broken collaborator cannot leave a
     * revoked client half connected.
     *
     * @param clientId The client ID to revoke
     * @return {@code true} if the client was found and revoked; {@code false} if not found
     */
    public boolean revokeClient(String clientId) {
        boolean removed = pairingService.revokeClientProfile(clientId);
        sessionService.removeSessionByClientId(clientId);
        revocationHandlers.forEach(handler -> {
            try {
                handler.onClientRevoked(clientId);
            } catch (Exception e) {
                log.error("Revocation handler failed for client {}", clientId, e);
            }
        });
        if (removed) {
            log.info("Revoked client {}", clientId);
        } else {
            log.warn("Client profile not found for {}, but session, connection and push registration " +
                    "cleanup were still applied", clientId);
        }
        return removed;
    }
}
