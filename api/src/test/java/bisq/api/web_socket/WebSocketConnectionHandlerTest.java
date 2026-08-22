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
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
        TestableHandler(SubscriptionService subscriptionService, WebSocketRestApiService restApiService) {
            super(subscriptionService, restApiService);
        }

        boolean isRegistered(DefaultWebSocket webSocket) {
            return getWebSockets().contains(webSocket);
        }
    }

    /**
     * Once armed, reports the socket as registered for one check and as gone afterwards, as a
     * revocation landing between the two checks would. Arming is explicit because connection setup
     * queries the registry too and would otherwise consume the one-time answer.
     */
    private static class DropAfterFirstCheckHandler extends TestableHandler {
        private final AtomicBoolean armed = new AtomicBoolean();
        private final AtomicBoolean firstCheckDone = new AtomicBoolean();

        DropAfterFirstCheckHandler(SubscriptionService subscriptionService,
                                   WebSocketRestApiService restApiService) {
            super(subscriptionService, restApiService);
        }

        void armRevocationAfterNextCheck() {
            armed.set(true);
        }

        @Override
        protected Set<WebSocket> getWebSockets() {
            Set<WebSocket> sockets = super.getWebSockets();
            if (!armed.get()) {
                return sockets;
            }
            return firstCheckDone.compareAndSet(false, true) ? sockets : Set.of();
        }
    }

    private SubscriptionService subscriptionService;
    private WebSocketRestApiService webSocketRestApiService;
    private TestableHandler handler;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        webSocketRestApiService = mock(WebSocketRestApiService.class);
        handler = new TestableHandler(subscriptionService, webSocketRestApiService);
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
    void aRevocationDuringMessageHandlingSweepsWhatTheMessageAdded() {
        // The registration recheck and the handling are two steps, so a revocation can land in
        // between and its cleanup runs before the subscription exists. Without the sweep the
        // subscription would outlive the revocation and keep feeding a revoked client.
        DefaultWebSocket webSocket = connect(REVOKED_CLIENT_ID);
        when(subscriptionService.canHandle(anyString())).thenReturn(true);
        doAnswer(invocation -> {
            handler.disconnectClient(REVOKED_CLIENT_ID);
            return null;
        }).when(subscriptionService).onMessage(anyString(), any());

        handler.onMessage(webSocket, "{\"anything\":true}");

        // Once from the revocation itself, once from the sweep after the message was handled.
        verify(subscriptionService, timeout(5000).times(2)).onConnectionClosed(webSocket);
    }

    @Test
    void aMessageIsDroppedWhenTheSocketIsRevokedBeforeItsTaskRuns() {
        // The socket passes the synchronous check and is deregistered before the queued task runs.
        // Simulated rather than raced: the executor hands each message to its own thread, so a
        // real revocation cannot be timed against the task deterministically.
        DropAfterFirstCheckHandler racingHandler =
                new DropAfterFirstCheckHandler(subscriptionService, webSocketRestApiService);
        DefaultWebSocket webSocket = connect(racingHandler, REVOKED_CLIENT_ID);
        when(subscriptionService.canHandle(anyString())).thenReturn(true);
        // Armed after setup, so the synchronous check still sees the socket and the message is
        // dispatched; only the recheck inside the task finds it gone.
        racingHandler.armRevocationAfterNextCheck();

        racingHandler.onMessage(webSocket, "{\"anything\":true}");

        verify(webSocket, timeout(5000).atLeastOnce()).close();
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
}
