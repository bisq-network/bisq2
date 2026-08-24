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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class WebSocketConnectionHandler extends WebSocketApplication implements Service {
    public final ExecutorService executor = ExecutorFactory.newCachedThreadPool("WebSocketConnectionHandler",
            1, 50, 30);

    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    @Getter
    private final ObservableSet<WebSocketClient> websocketClients = new ObservableSet<>();
    /**
     * Serializes registering a connection against revoking a client, so a handshake in flight
     * cannot register after the revocation has already scanned the registry.
     */
    private final Object revocationLock = new Object();
    /**
     * Clients revoked in this process. Authentication happens during the handshake, so a client
     * revoked right after that point would otherwise register a live connection that the
     * revocation scan could not see.
     * <p>
     * In memory on purpose: it only has to outlive an in-flight handshake. A later handshake of a
     * revoked client fails authentication, and a restart leaves none in flight. Client IDs are
     * random and never reused, so entries never turn into false rejections.
     */
    private final Set<String> revokedClientIds = ConcurrentHashMap.newKeySet();

    public WebSocketConnectionHandler(SubscriptionService subscriptionService,
                                      WebSocketRestApiService webSocketRestApiService) {
        this.subscriptionService = subscriptionService;
        this.webSocketRestApiService = webSocketRestApiService;
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
        boolean revoked;
        synchronized (revocationLock) {
            // Checked and registered as one step against disconnectClient. Authentication happened
            // during the handshake, so without this a client revoked since then would register a
            // connection the revocation scan has already passed, and onMessage would accept it as
            // a registered socket.
            revoked = findClientId(socket).filter(revokedClientIds::contains).isPresent();
            if (!revoked) {
                super.onConnect(socket);
            }
        }
        if (revoked) {
            // Closed outside the lock, as it writes to the connection and must not hold up a
            // concurrent revocation.
            log.warn("Rejecting connection of a revoked client");
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
        // A socket dropped by disconnectClient is deregistered even when its close failed, and a
        // revoked client must not keep subscribing or receiving data through a socket that stayed
        // alive. Checked before the async dispatch so no work is queued for it.
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
            // Rechecked here because the check above happened before this task was queued, and a
            // revocation in between must not be able to subscribe the socket again.
            if (!getWebSockets().contains(webSocket)) {
                log.warn("Dropping queued message of a disconnected WebSocket");
                closeQuietly(webSocket);
                return;
            }
            try {
                if (subscriptionService.canHandle(message)) {
                    subscriptionService.onMessage(message, webSocket);
                } else if (webSocketRestApiService.canHandle(message)) {
                    webSocketRestApiService.onMessage(message, webSocket);
                } else {
                    log.error("No service found for handling message: {}", JsonUtil.redactCredentials(message));
                }
            } finally {
                // The recheck and the handling are still two steps, so a revocation can land while
                // the message is being handled and its cleanup would run before the subscription
                // exists. Sweeping afterwards makes that self correcting: a subscription added by
                // a revoked socket is removed again instead of outliving the revocation.
                if (!getWebSockets().contains(webSocket)) {
                    subscriptionService.onConnectionClosed(webSocket);
                    closeQuietly(webSocket);
                }
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
     * so the socket is instead unsubscribed and deregistered, which stops both pushed data and
     * anything it tries to send.
     *
     * @param clientId The client ID whose connections should be closed
     */
    public void disconnectClient(String clientId) {
        List<WebSocket> sockets;
        synchronized (revocationLock) {
            // Recorded and deregistered under the same lock as onConnect, so a handshake in flight
            // is rejected instead of registering after this scan. Closing and unsubscribing happen
            // outside the lock, as they must not block a connection attempt.
            revokedClientIds.add(clientId);
            sockets = getWebSockets().stream()
                    .filter(webSocket -> findClientId(webSocket).filter(clientId::equals).isPresent())
                    .toList();
            sockets.forEach(this::remove);
        }
        sockets.forEach(webSocket -> {
            log.info("Disconnecting revoked client: {}", clientId);
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Could not close WebSocket of revoked client {}", clientId, e);
            } finally {
                // Applied regardless of whether the close succeeded. A socket whose close failed
                // stays connected, and subscriptions carry no permission check of their own, so
                // dropping the subscriptions is what actually stops data reaching a revoked
                // client. The socket is already deregistered, which makes onMessage reject
                // anything it still sends. Both are idempotent, so the onClose that follows a
                // successful close is harmless.
                subscriptionService.onConnectionClosed(webSocket);
            }
        });
        updateWebsocketClients();
    }

    private static Optional<String> findClientId(WebSocket webSocket) {
        if (webSocket instanceof DefaultWebSocket defaultWebSocket) {
            HttpServletRequest request = defaultWebSocket.getUpgradeRequest();
            if (request != null) {
                return Optional.ofNullable(request.getHeader(Headers.CLIENT_ID))
                        .filter(StringUtils::isNotEmpty);
            }
        }
        return Optional.empty();
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
                Optional<String> clientId = findClientId(webSocket);
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
