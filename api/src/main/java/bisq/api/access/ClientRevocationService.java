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
     * Revokes a paired client: its access is withdrawn, its sessions are invalidated and every
     * {@link ClientRevocationHandler} runs, which closes its live WebSocket connections and drops
     * its push notification registrations. The profile is removed once that cleanup has succeeded,
     * after which the client must pair again.
     * <p>
     * The order is load-bearing. Permissions go first, so an in-flight subscription re-reading the
     * grant is refused rather than served, and so a client has no access for the rest of this
     * regardless of what follows. The profile goes last, because it is what the management ID
     * resolves against: dropping it before the cleanup succeeded would make the retry this reports
     * impossible, leaving the failed cleanup to never run again.
     * <p>
     * A failing handler does not stop the remaining ones, so one broken collaborator cannot leave a
     * client half revoked, and the failure is reported rather than only logged: the caller would
     * otherwise be told the client was revoked while it can still be connected or receive push
     * notifications.
     *
     * @param clientId The client ID to revoke
     * @return the outcome; see {@link ClientRevocationResult}
     */
    public ClientRevocationResult revokeClient(String clientId) {
        pairingService.revokePermissions(clientId);
        sessionService.removeSessionByClientId(clientId);

        boolean cleanupFailed = false;
        for (ClientRevocationHandler handler : revocationHandlers) {
            try {
                handler.onClientRevoked(clientId);
            } catch (Exception e) {
                cleanupFailed = true;
                log.error("Revocation handler failed for client {}", clientId, e);
            }
        }
        if (cleanupFailed) {
            log.error("Revocation of client {} is incomplete, it may still be connected or receive " +
                    "push notifications. Its access is withdrawn and it stays listed so the " +
                    "revocation can be retried", clientId);
            return ClientRevocationResult.CLEANUP_FAILED;
        }

        if (pairingService.revokeClientProfile(clientId)) {
            log.info("Revoked client {}", clientId);
            return ClientRevocationResult.REVOKED;
        }
        log.warn("Client profile not found for {}, but session, connection and push registration " +
                "cleanup were still applied", clientId);
        return ClientRevocationResult.NOT_FOUND;
    }

    /**
     * Finishes revocations whose cleanup never succeeded, identified by a profile that has no
     * permissions: pairing writes both together, so that combination only exists between the two
     * halves of a revocation.
     * <p>
     * Run at startup because the state a failed cleanup leaves behind is not all transient. Live
     * connections do not survive a restart, but push registrations do, so without this a client
     * whose cleanup failed and was never retried would keep receiving notifications indefinitely.
     */
    public void completeInterruptedRevocations() {
        pairingService.getClientProfiles().stream()
                .map(ClientProfile::getClientId)
                .filter(clientId -> !pairingService.hasPermissions(clientId))
                .forEach(clientId -> {
                    log.warn("Completing the interrupted revocation of client {}", clientId);
                    revokeClient(clientId);
                });
    }
}
