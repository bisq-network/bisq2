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

package bisq.api.web_socket;

import bisq.api.access.filter.Headers;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConnectionHandlerTest {
    private static final String REVOKED_CLIENT_ID = "revoked-client";

    /** Exposes the framework's socket registry, which is protected on WebSocketApplication. */
    private static class TestableHandler extends WebSocketConnectionHandler {
        TestableHandler(SubscriptionService subscriptionService,
                        WebSocketRestApiService restApiService,
                        Predicate<String> clientPairedCheck) {
            super(subscriptionService, restApiService, clientPairedCheck);
        }

        boolean isRegistered(DefaultWebSocket webSocket) {
            return getWebSockets().contains(webSocket);
        }
    }

    private Set<String> pairedClientIds;
    private SubscriptionService subscriptionService;
    private WebSocketRestApiService webSocketRestApiService;
    private TestableHandler handler;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        webSocketRestApiService = mock(WebSocketRestApiService.class);
        pairedClientIds = ConcurrentHashMap.newKeySet();
        pairedClientIds.add(REVOKED_CLIENT_ID);
        pairedClientIds.add("other-client");
        pairedClientIds.add("live-client");
        handler = new TestableHandler(subscriptionService, webSocketRestApiService, pairedClientIds::contains);
    }

    /** Mirrors a real revocation: the profile goes first, then the live sockets are dropped. */
    private void revoke(String clientId) {
        pairedClientIds.remove(clientId);
        handler.disconnectClient(clientId);
    }

    private DefaultWebSocket connect(String clientId) {
        return connect(handler, clientId);
    }

    private DefaultWebSocket connect(TestableHandler target, String clientId) {
        DefaultWebSocket webSocket = mock(DefaultWebSocket.class, RETURNS_DEEP_STUBS);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(Headers.CLIENT_ID)).thenReturn(clientId);
        when(webSocket.getUpgradeRequest()).thenReturn(request);
        target.onConnect(webSocket);
        return webSocket;
    }

    @Test
    void aFailingCloseDoesNotLeaveOtherSocketsOfTheClientConnected() {
        DefaultWebSocket failing = connect(REVOKED_CLIENT_ID);
        DefaultWebSocket closing = connect(REVOKED_CLIENT_ID);
        DefaultWebSocket otherClient = connect("other-client");
        doThrow(new RuntimeException("close failed")).when(failing).close();

        handler.disconnectClient(REVOKED_CLIENT_ID);

        verify(failing).close();
        verify(closing).close();
        assertFalse(handler.isRegistered(failing));
        assertFalse(handler.isRegistered(closing));
        assertTrue(handler.isRegistered(otherClient));
    }

    @Test
    void aSocketThatFailedToCloseIsUnsubscribedAndDeregistered() {
        // The close cannot be forced, so the socket may stay alive. Dropping its subscriptions is
        // what actually stops data reaching the revoked client, since subscribing carries no
        // permission check of its own.
        DefaultWebSocket failing = connect(REVOKED_CLIENT_ID);
        doThrow(new RuntimeException("close failed")).when(failing).close();

        handler.disconnectClient(REVOKED_CLIENT_ID);

        verify(subscriptionService).onConnectionClosed(failing);
        assertFalse(handler.isRegistered(failing));
    }

    @Test
    void aSocketThatFailedToCloseCannotProcessMessages() {
        DefaultWebSocket failing = connect(REVOKED_CLIENT_ID);
        doThrow(new RuntimeException("close failed")).when(failing).close();
        handler.disconnectClient(REVOKED_CLIENT_ID);

        handler.onMessage(failing, "{\"anything\":true}");

        verify(subscriptionService, never()).canHandle(anyString());
        verify(subscriptionService, never()).onMessage(anyString(), any());
        verify(webSocketRestApiService, never()).onMessage(anyString(), any());
    }

    @Test
    void aConnectedSocketStillProcessesMessages() {
        DefaultWebSocket webSocket = connect("live-client");
        when(subscriptionService.canHandle(anyString())).thenReturn(true);

        handler.onMessage(webSocket, "{\"anything\":true}");

        verify(subscriptionService, timeout(5000)).onMessage(anyString(), any());
    }

    @Test
    void aHandshakeCompletingAfterTheRevocationDoesNotStayRegistered() {
        // The revocation runs while no socket of that client is registered yet, so its scan cannot
        // see the connection that is still completing its handshake.
        revoke(REVOKED_CLIENT_ID);

        DefaultWebSocket lateSocket = connect(REVOKED_CLIENT_ID);

        verify(lateSocket).close();
        assertFalse(handler.isRegistered(lateSocket));
    }

    @Test
    void aMessageFromAConnectionRejectedAtHandshakeIsNotHandled() {
        revoke(REVOKED_CLIENT_ID);
        DefaultWebSocket lateSocket = connect(REVOKED_CLIENT_ID);

        handler.onMessage(lateSocket, "{\"anything\":true}");

        verify(subscriptionService, never()).onMessage(anyString(), any());
        verify(webSocketRestApiService, never()).onMessage(anyString(), any());
    }

    @Test
    void revokingOneClientDoesNotBlockAnotherFromConnecting() {
        revoke(REVOKED_CLIENT_ID);

        DefaultWebSocket webSocket = connect("other-client");

        assertTrue(handler.isRegistered(webSocket));
    }
}
