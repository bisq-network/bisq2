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

import bisq.api.ApiConfig;
import bisq.api.access.filter.Headers;
import bisq.api.access.filter.authn.RestApiSessionAuthenticationFilter;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.pairing.PairingCode;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.access.session.SessionService;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.api.dto.access.pairing.PairingRequestDto;
import bisq.api.dto.access.pairing.PairingResponseDto;
import bisq.api.dto.access.session.SessionRequestDto;
import bisq.api.dto.access.session.SessionResponseDto;
import bisq.api.rest_api.endpoints.access.AccessApi;
import bisq.api.util.LogCapture;
import bisq.common.file.FileReaderUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The ID of a pairing code, the pairing QR code, the client secret and the session ID are credentials of the API
 * (docs/specifications/security/secrets-in-logs.md). The tests drive the pairing and session flows with the real
 * services, mock only the persistence, and check every captured log line. The credentials are taken from the objects
 * which the flows return, because the services generate them.
 */
@Isolated("Captures the log of every logger")
class ApiAccessLoggingTest {
    private static final String WEB_SOCKET_URL = "ws://127.0.0.1:8090";
    private static final String UNKNOWN_SESSION_ID = "SESSIONsentinel64";
    private static final String WRONG_CLIENT_SECRET = "SECRETsentinel81";

    @RegisterExtension
    final LogCapture logs = new LogCapture();

    @TempDir
    Path dataDir;

    private PairingService pairingService;
    private AccessApi accessApi;
    private RestApiSessionAuthenticationFilter sessionAuthenticationFilter;

    @BeforeEach
    void setUp() {
        ApiConfig apiConfig = new ApiConfig(ApiAccessTransportType.CLEAR,
                300,
                true,
                false,
                true,
                false,
                "127.0.0.1",
                8090,
                80,
                true,
                true,
                false,
                "",
                List.of(),
                false,
                60);
        ApiAccessStoreService apiAccessStoreService = newApiAccessStoreService();
        PermissionService permissionService = new PermissionService(apiAccessStoreService);
        pairingService = new PairingService(apiConfig, dataDir, apiAccessStoreService, permissionService);
        SessionService sessionService = new SessionService(60);
        accessApi = new AccessApi(newApiAccessService(pairingService, sessionService));
        sessionAuthenticationFilter = new RestApiSessionAuthenticationFilter(
                new SessionAuthenticationService(pairingService, sessionService));
    }

    @Test
    void newPairingCodeIsNotLogged() throws IOException {
        PairingCode pairingCode = pairingService.createPairingCode(Permission.autoGrantable());

        pairingService.createPairingQrCode(pairingCode, WEB_SOCKET_URL, Optional.empty(), Optional.empty());

        // The QR code contains the ID in binary form, so a logged QR code is checked separately
        String qrCode = pairingService.getPairingQrCode().get();
        assertThat(qrCode).isNotNull();
        logs.assertNoLineContains(pairingCode.getId(), qrCode);
        assertThat(logs.lines())
                .contains("Pairing QR code created. The pairing code expires at " + pairingCode.getExpiresAt())
                .anyMatch(line -> line.startsWith("Pairing QR code written to "));
        // The operator reads the code from this file, not from the log
        assertThat(FileReaderUtils.readUTF8String(dataDir.resolve("pairing_qr_code.txt"))).startsWith(qrCode);
    }

    @Test
    void failedPairingQrCodeDoesNotLogTheCodeId() {
        PairingCode pairingCode = pairingService.createPairingCode(Permission.autoGrantable());
        // Longer than the 512 bytes which the QR code format allows for the URL, so the encoder throws
        String tooLongWebSocketUrl = WEB_SOCKET_URL + "/" + "a".repeat(600);

        pairingService.createPairingQrCode(pairingCode, tooLongWebSocketUrl, Optional.empty(), Optional.empty());

        assertThat(pairingService.getPairingQrCode().get()).isNull();
        logs.assertNoLineContains(pairingCode.getId());
        assertThat(logs.lines()).anyMatch(line -> line.startsWith(
                "Could not create the pairing QR code. The pairing code expires at " + pairingCode.getExpiresAt()));
    }

    @Test
    void pairingDoesNotLogCredentials() {
        PairingCode pairingCode = pairingService.createPairingCode(Permission.autoGrantable());

        PairingResponseDto pairing = pair(pairingCode);

        logs.assertNoLineContains(pairingCode.getId(), pairing.getClientSecret(), pairing.getSessionId());
        assertThat(logs.lines()).contains("Pairing code which expires at " + pairingCode.getExpiresAt()
                + " consumed, signalling observers for regeneration");
    }

    @Test
    void rejectedPairingDoesNotLogTheSubmittedCodeId() {
        String unknownCodeId = UUID.randomUUID().toString();
        PairingCode usedPairingCode = pairingService.createPairingCode(Permission.autoGrantable());
        PairingCode pairingCodeOfAnotherVersion = pairingService.createPairingCode(Permission.autoGrantable());

        Response unknown = accessApi.requestPairing(
                new PairingRequestDto(PairingService.VERSION, unknownCodeId, "phone"));
        pair(usedPairingCode);
        Response used = accessApi.requestPairing(
                new PairingRequestDto(PairingService.VERSION, usedPairingCode.getId(), "phone"));
        Response otherVersion = accessApi.requestPairing(new PairingRequestDto((byte) (PairingService.VERSION + 1),
                pairingCodeOfAnotherVersion.getId(),
                "phone"));

        assertThat(List.of(unknown.getStatus(), used.getStatus(), otherVersion.getStatus()))
                .containsExactly(401, 401, 400);
        logs.assertNoLineContains(unknownCodeId, usedPairingCode.getId(), pairingCodeOfAnotherVersion.getId());
        assertThat(logs.lines())
                .anyMatch(line -> line.startsWith("Pairing request rejected"))
                .anyMatch(line -> line.startsWith("Invalid pairing request payload"));
    }

    @Test
    void sessionRequestDoesNotLogCredentials() {
        PairingResponseDto pairing = pair(pairingService.createPairingCode(Permission.autoGrantable()));
        String clientId = pairing.getClientId();
        String clientSecret = pairing.getClientSecret();

        Response accepted = accessApi.requestSession(new SessionRequestDto(clientId, clientSecret));
        Response wrongSecret = accessApi.requestSession(new SessionRequestDto(clientId, WRONG_CLIENT_SECRET));
        Response emptySecret = accessApi.requestSession(new SessionRequestDto(clientId, ""));
        Response missingSecret = accessApi.requestSession(new SessionRequestDto(clientId, null));
        Response unknownClient = accessApi.requestSession(
                new SessionRequestDto(UUID.randomUUID().toString(), clientSecret));

        assertThat(List.of(accepted.getStatus(),
                wrongSecret.getStatus(),
                emptySecret.getStatus(),
                missingSecret.getStatus(),
                unknownClient.getStatus()))
                .containsExactly(201, 401, 401, 400, 401);
        String sessionId = ((SessionResponseDto) accepted.getEntity()).sessionId();
        logs.assertNoLineContains(clientSecret, WRONG_CLIENT_SECRET, pairing.getSessionId(), sessionId);
        assertThat(logs.lines())
                .anyMatch(line -> line.startsWith("Session request rejected"))
                .anyMatch(line -> line.startsWith("Invalid session request payload"));
    }

    @Test
    void sessionAuthenticationDoesNotLogTheSessionId() {
        PairingResponseDto clientA = pair(pairingService.createPairingCode(Permission.autoGrantable()));
        PairingResponseDto clientB = pair(pairingService.createPairingCode(Permission.autoGrantable()));

        sessionAuthenticationFilter.doFilter(requestContext(clientA.getClientId(), clientA.getSessionId()));
        assertThatThrownBy(() -> sessionAuthenticationFilter.doFilter(
                requestContext(clientA.getClientId(), UNKNOWN_SESSION_ID)))
                .isInstanceOf(WebApplicationException.class);
        assertThatThrownBy(() -> sessionAuthenticationFilter.doFilter(
                requestContext(clientA.getClientId(), clientB.getSessionId())))
                .isInstanceOf(WebApplicationException.class);

        logs.assertNoLineContains(clientA.getSessionId(), clientB.getSessionId(), UNKNOWN_SESSION_ID);
        assertThat(logs.lines())
                .filteredOn(line -> line.startsWith("Authentication failed"))
                .hasSize(2);
    }

    private PairingResponseDto pair(PairingCode pairingCode) {
        Response response = accessApi.requestPairing(
                new PairingRequestDto(PairingService.VERSION, pairingCode.getId(), "phone"));
        assertThat(response.getStatus()).isEqualTo(201);
        return (PairingResponseDto) response.getEntity();
    }

    private static ContainerRequestContext requestContext(String clientId, String sessionId) {
        ContainerRequestContext context = mock(ContainerRequestContext.class, RETURNS_DEEP_STUBS);
        when(context.getUriInfo().getRequestUri())
                .thenReturn(URI.create("http://127.0.0.1:8090/api/v1/offerbook/markets"));
        when(context.getHeaderString(Headers.CLIENT_ID)).thenReturn(clientId);
        when(context.getHeaderString(Headers.SESSION_ID)).thenReturn(sessionId);
        return context;
    }

    // The only call of the constructor, so that a change of its parameters needs one change in this class
    private static ApiAccessService newApiAccessService(PairingService pairingService, SessionService sessionService) {
        return new ApiAccessService(pairingService, sessionService,
                new ClientRevocationService(pairingService, sessionService, List.of()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ApiAccessStoreService newApiAccessStoreService() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // The shutdown hook of the persistence client reads the store path
        when(persistence.getStorePath()).thenReturn(Path.of("test-store"));
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        return new ApiAccessStoreService(persistenceService);
    }
}
