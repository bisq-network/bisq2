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

import bisq.api.access.client.ClientIdentity;
import bisq.api.access.pairing.PairingCode;
import bisq.api.access.pairing.PairingResponse;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.pairing.qr.PairingQrCode;
import bisq.api.access.pairing.qr.PairingQrCodeDecoder;
import bisq.api.access.pairing.qr.PairingQrCodeGenerator;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.access.session.SessionService;
import bisq.common.util.NetworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style test covering the full API access bootstrap flow:
 *
 * <ol>
 *   <li>Server creates a pairing code with permissions</li>
 *   <li>Pairing code is encoded into a QR code</li>
 *   <li>Client scans the QR code and extracts connection metadata</li>
 *   <li>Client creates a pairing request</li>
 *   <li>Server validates the request and issues a session token</li>
 *   <li>Client receives and stores the session token</li>
 * </ol>
 *
 * <p>
 * This test intentionally avoids network I/O and cryptography side-effects.
 * All transport and client behavior is simulated via {@link MockClient}.
 * </p>
 *
 * <p>
 * The purpose of this test is to validate the end-to-end orchestration
 * of pairing, not low-level encoding or transport mechanics.
 * </p>
 */
@Slf4j
class ApiAccessIntegrationTest {

    @Test
    void testPairingFlow_endToEndHappyPath() throws Exception {
        // ---------------------------------------------------------------------
        // Given: Server-side services
        // ---------------------------------------------------------------------
        SessionService sessionService = new SessionService(60);

        ApiAccessStoreService apiAccessStoreService = new MockApiAccessStoreService();

        PermissionService<RestPermissionMapping> permissionService = new PermissionService<>(apiAccessStoreService, new RestPermissionMapping());

        PairingService pairingService = new PairingService(apiAccessStoreService, permissionService);
        ApiAccessService apiAccessService = new ApiAccessService(pairingService, sessionService);

        // ---------------------------------------------------------------------
        // Given: Client protocol (simulated)
        // ---------------------------------------------------------------------
        MockClient client = new MockClient();
        CountDownLatch pairingCompleted = new CountDownLatch(1);

        // ---------------------------------------------------------------------
        // Given: Server generates pairing QR code
        // ---------------------------------------------------------------------
        String webSocketUrl = NetworkUtils
                .findLANHostAddress(Optional.empty())
                .orElse("127.0.0.1");

        Set<Permission> permissions = Set.of(Permission.values());

        PairingCode grantedPermissions = pairingService.createPairingCode(permissions);

        String qrCode = PairingQrCodeGenerator.generateQrCode(
                grantedPermissions,
                webSocketUrl,
                Optional.empty(),   // TLS fingerprint
                Optional.empty()    // Tor client auth secret
        );

        // ---------------------------------------------------------------------
        // When: Client scans QR code
        // ---------------------------------------------------------------------
        client.setQrCode(qrCode);

        client.readQrCode().thenAccept(scannedQrCode -> {
            try {
                // Client identity creation (device-side)
                ClientIdentity clientIdentity = client.createClientIdentity("Alice phone");

                // Decode QR code contents
                PairingQrCode qrData = PairingQrCodeDecoder.decode(scannedQrCode);

                // Apply qrData.getWebSocketUrl();

                PairingCode decodedPairingCode = qrData.getPairingCode();
                client.setGrantedPermissions(decodedPairingCode.getGrantedPermissions());

                // -----------------------------------------------------------------
                // When: Server handles pairing request
                // -----------------------------------------------------------------
                PairingResponse pairingResponse = apiAccessService.requestPairing(PairingService.VERSION,
                        decodedPairingCode.getId(),
                        clientIdentity.getClientName());
                String sessionId = pairingResponse.getSessionId();

                // -----------------------------------------------------------------
                // Then: Client receives session ID
                // -----------------------------------------------------------------
                client.setSessionId(sessionId);

                pairingCompleted.countDown();
            } catch (Exception e) {
                log.error("Pairing flow failed", e);
                throw new RuntimeException(e);
            }
        });

        // ---------------------------------------------------------------------
        // Then: Pairing completes successfully
        // ---------------------------------------------------------------------
        assertTrue(
                pairingCompleted.await(1, TimeUnit.SECONDS),
                "Pairing flow did not complete in time"
        );

        // Optional future assertions:
        //
        assertNotNull(client.getSessionId());
        assertEquals(permissions, client.getGrantedPermissions());
    }
}
