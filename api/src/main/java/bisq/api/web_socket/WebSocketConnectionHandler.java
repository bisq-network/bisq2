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
import bisq.api.web_socket.util.JsonUtil;
import bisq.api.web_socket.util.WebSocketIdentity;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class WebSocketConnectionHandler extends WebSocketApplication implements Service {
    public final ExecutorService executor = ExecutorFactory.newCachedThreadPool("WebSocketConnectionHandler",
            1, 50, 30);

    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    @Getter
    private final ObservableSet<WebSocketClient> websocketClients = new ObservableSet<>();

    /** Whether a client is still paired, read from the authoritative store at connection time. */
    private final Predicate<String> clientPairedCheck;

    public WebSocketConnectionHandler(SubscriptionService subscriptionService,
                                      WebSocketRestApiService webSocketRestApiService,
                                      Predicate<String> clientPairedCheck) {
        this.subscriptionService = subscriptionService;
        this.webSocketRestApiService = webSocketRestApiService;
        this.clientPairedCheck = clientPairedCheck;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        getWebSockets().forEach(WebSocket::close);
        ExecutorFactory.shutdownAndAwaitTermination(executor);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onConnect(WebSocket socket) {
        // todo use config to check if multiple clients are permitted
        super.onConnect(socket);
        // The handshake authenticated the client, and it can have been revoked since. Checked
        // after registering rather than before, because a check before would leave the window it
        // closes: the revocation scan could pass between the check and the registration, and the
        // connection would survive a revocation that had already reported success. Registering
        // first means either that scan finds this socket or this check finds the client gone.
        if (WebSocketIdentity.findClientId(socket)
                .filter(clientId -> !clientPairedCheck.test(clientId))
                .isPresent()) {
            log.warn("Rejecting connection of a client revoked during the handshake");
            remove(socket);
            closeQuietly(socket);
            return;
        }
        log.info("Client connected: {}", socket);

        updateWebsocketClients();
    }

    @Override
    public void onClose(WebSocket webSocket, DataFrame frame) {
        super.onClose(webSocket, frame);
        subscriptionService.onConnectionClosed(webSocket);
        log.info("Client disconnected: {}", webSocket);
        updateWebsocketClients();
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        // A socket dropped by disconnectClient is deregistered even when its close failed, so
        // nothing is queued for a connection the node has already given up on.
        if (!getWebSockets().contains(webSocket)) {
            log.warn("Ignoring message from a disconnected WebSocket");
            closeQuietly(webSocket);
            return;
        }
        // Debug rather than info: this is one full message plus a redaction pass per message, which
        // production should not pay for. Redacted because a client released before the node took the
        // identity from the handshake puts its session id into the payload.
        if (log.isDebugEnabled()) {
            log.debug("Received message {}", JsonUtil.redactCredentials(message));
        }
        CompletableFuture.runAsync(() -> {
            if (subscriptionService.canHandle(message)) {
                subscriptionService.onMessage(message, webSocket);
            } else if (webSocketRestApiService.canHandle(message)) {
                webSocketRestApiService.onMessage(message, webSocket);
            } else {
                log.error("No service found for handling message: {}", JsonUtil.redactCredentials(message));
            }
        }, executor);
    }

    /**
     * Closes all WebSocket connections of a specific client.
     * Called after revoking a client profile to force immediate disconnection.
     * <p>
     * A client can hold more than one connection, so a failing close is caught per socket: an
     * exception escaping here would leave the client's remaining sockets connected, and a revoked
     * client must not keep a live connection. A close that fails cannot be forced at this level,
     * so the socket is unsubscribed and deregistered anyway, which stops the data it had already
     * subscribed to. New subscriptions are refused by the authorization in
     * {@code SubscriptionService}, which reads the grant this revocation has removed.
     *
     * @param clientId The client ID whose connections should be closed
     */
    public void disconnectClient(String clientId) {
        // Deregistered before closing, so a socket whose close fails is already rejected by
        // onMessage. A handshake still in flight is not in this snapshot; onConnect revalidates
        // against the store after registering, which is what covers that ordering.
        List<WebSocket> sockets = getWebSockets().stream()
                .filter(webSocket -> WebSocketIdentity.findClientId(webSocket).filter(clientId::equals).isPresent())
                .toList();
        sockets.forEach(this::remove);
        sockets.forEach(webSocket -> {
            log.info("Disconnecting revoked client: {}", clientId);
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Could not close WebSocket of revoked client {}", clientId, e);
            } finally {
                // Applied regardless of whether the close succeeded. Existing subscriptions are
                // authorized once, when they are taken out, so nothing re-checks them on the way
                // out: dropping them here is what stops data reaching a revoked client whose
                // socket stayed alive. Normally the close triggers onClose which does the same,
                // which is exactly why a failing close would otherwise leave them streaming.
                // Idempotent, so the onClose after a successful close is harmless.
                subscriptionService.onConnectionClosed(webSocket);
            }
        });
        updateWebsocketClients();
    }

    private void closeQuietly(WebSocket webSocket) {
        try {
            webSocket.close();
        } catch (Exception e) {
            log.debug("Repeated close of an already dropped WebSocket failed", e);
        }
    }

    private void updateWebsocketClients() {
        try {
            websocketClients.setAll(getWebSockets().stream().map(webSocket -> {
                Optional<String> clientId = WebSocketIdentity.findClientId(webSocket);
                Optional<String> clientAddress = Optional.empty();
                Optional<String> userAgent = Optional.empty();
                if (webSocket instanceof DefaultWebSocket defaultWebSocket) {
                    HttpServletRequest request = defaultWebSocket.getUpgradeRequest();
                    if (request != null) {
                        // Get remote address directly from the request
                        String clientAddressHeader = request.getRemoteAddr();
                        if (StringUtils.isNotEmpty(clientAddressHeader)) {
                            clientAddress = Optional.of(clientAddressHeader);
                        }

                        // Get userAgent from HTTP headers
                        String userAgentHeader = request.getHeader(Headers.USER_AGENT);
                        if (StringUtils.isNotEmpty(userAgentHeader)) {
                            userAgent = Optional.of(userAgentHeader);
                        }
                    }
                }
                return new WebSocketClient(clientId, clientAddress, userAgent);
            }).collect(Collectors.toSet()));
        } catch (Exception t) {
            log.warn("Could not notify clients listeners", t);
        }
    }

    private static Optional<String> getAttribute(HttpServletRequest request, String key) {
        Object attribute = request.getAttribute(key);
        if (attribute instanceof String value) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }
}
