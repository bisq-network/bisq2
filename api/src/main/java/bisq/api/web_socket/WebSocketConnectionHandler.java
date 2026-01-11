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


import bisq.api.access.filter.Attributes;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class WebSocketConnectionHandler extends WebSocketApplication implements Service {
    public final ExecutorService executor = ExecutorFactory.newCachedThreadPool("WebSocketConnectionHandler",
            1, 50, 30);

    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    @Getter
    private final ObservableSet<WebsocketClient1> websocketClients = new ObservableSet<>();

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
        super.onConnect(socket);
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
        log.info("Received message {}", message);
        CompletableFuture.runAsync(() -> {
            if (subscriptionService.canHandle(message)) {
                subscriptionService.onMessage(message, webSocket);
            } else if (webSocketRestApiService.canHandle(message)) {
                webSocketRestApiService.onMessage(message, webSocket);
            } else {
                log.error("No service found for handling message: {}", message);
            }
        }, executor);
    }

    private void updateWebsocketClients() {
        try {
            websocketClients.setAll(getWebSockets().stream().map(webSocket -> {
                Optional<String> address = Optional.empty();
                Optional<String> userAgent = Optional.empty();
                if (webSocket instanceof DefaultWebSocket defaultWebSocket) {
                    HttpServletRequest request = defaultWebSocket.getUpgradeRequest();
                    address = getAttribute(request, Attributes.REMOTE_ADDRESS);
                    userAgent = getAttribute(request, Attributes.USER_AGENT);
                }
                return new WebsocketClient1(address, userAgent);
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
