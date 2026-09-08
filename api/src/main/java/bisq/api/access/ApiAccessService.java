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

import bisq.api.access.identity.ClientManagementId;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.InvalidPairingRequestException;
import bisq.api.access.pairing.PairingResponse;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.InvalidSessionRequestException;
import bisq.api.access.session.SessionResponse;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * The single entry point of the access layer. Callers outside this package depend on this class
 * only, never on the services behind it, so there is exactly one way to perform each operation.
 * <p>
 * Logic belongs in a dedicated service and reaches callers as a delegation here, as
 * {@link ClientRevocationService} does for revocation.
 */
@Slf4j

public class ApiAccessService {

    private final PairingService pairingService;
    private final SessionService sessionService;
    private final ClientRevocationService clientRevocationService;

    public ApiAccessService(PairingService pairingService,
                            SessionService sessionService,
                            ClientRevocationService clientRevocationService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
        this.clientRevocationService = clientRevocationService;
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
     * All paired clients, as full domain objects carrying their credentials. Callers must map to a
     * representation without the client secret and without the client ID before anything leaves the
     * process; the REST layer does so via {@code PairedClientDto} and {@link ClientManagementId}.
     */
    public List<ClientProfile> getClientProfiles() {
        return pairingService.getClientProfiles();
    }

    /**
     * Revokes a paired client. See {@link ClientRevocationService#revokeClient(String)} for what
     * revocation covers.
     *
     * @param clientId The client ID to revoke
     * @return the outcome; see {@link ClientRevocationResult}
     */
    public ClientRevocationResult revokeClient(String clientId) {
        return clientRevocationService.revokeClient(clientId);
    }

    /** See {@link ClientRevocationService#completeInterruptedRevocations()}. */
    public void completeInterruptedRevocations() {
        clientRevocationService.completeInterruptedRevocations();
    }

    /**
     * Revokes the client a management ID names, for callers that must not be given client IDs. See
     * {@link ClientManagementId}.
     * <p>
     * A handle that resolves to nothing is reported as not found. A client whose cleanup failed
     * still resolves, as its profile is kept until the revocation completes, so the retry the
     * endpoint asks for reaches the same client.
     *
     * @param managementId The management ID of the client to revoke
     * @return the outcome; see {@link ClientRevocationResult}
     */
    public ClientRevocationResult revokeClientByManagementId(String managementId) {
        return pairingService.getClientProfiles().stream()
                .filter(clientProfile -> ClientManagementId.matches(clientProfile, managementId))
                .findFirst()
                .map(clientProfile -> revokeClient(clientProfile.getClientId()))
                .orElse(ClientRevocationResult.NOT_FOUND);
    }

    public SessionResponse requestSession(String clientId, String clientSecret) throws InvalidSessionRequestException {
        ClientProfile clientProfile = pairingService.findClientProfile(clientId)
                .orElseThrow(() -> new InvalidSessionRequestException("No client profile found for Client ID"));

        if (!MessageDigest.isEqual(
                                clientSecret.getBytes(StandardCharsets.UTF_8),
                                clientProfile.getClientSecret().getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidSessionRequestException("Client secret is not matching");
        }

        // Checked after the secret, so only the holder learns anything, and checked at all because
        // a profile outlives its permissions while a revocation waits for its cleanup to succeed.
        // Handing that client a session would mint a credential for an access that is already gone.
        if (!pairingService.hasPermissions(clientId)) {
            throw new InvalidSessionRequestException("No client profile found for Client ID");
        }

        SessionToken sessionToken = sessionService.createSession(clientId);
        long expiresAt = sessionToken.getExpiresAt().toEpochMilli();
        return new SessionResponse(sessionToken.getSessionId(), expiresAt);
    }
}
