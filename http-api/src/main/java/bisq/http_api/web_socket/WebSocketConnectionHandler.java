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

package bisq.http_api.web_socket;


import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.http_api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.http_api.web_socket.subscription.SubscriptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static bisq.http_api.auth.WebSocketRequestMetadataFilter.ATTR_WS_REMOTE_ADDRESS;
import static bisq.http_api.auth.WebSocketRequestMetadataFilter.ATTR_WS_USER_AGENT;

@Slf4j
public class WebSocketConnectionHandler extends WebSocketApplication implements Service {
    public final ExecutorService executor = ExecutorFactory.newCachedThreadPool("WebSocketConnectionHandler", 1, 50, 30);
    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    @Getter
    private final ObservableSet<BisqConnectClientInfo> websocketClients = new ObservableSet<>();

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
            websocketClients.setAll(getWebSockets().stream().map(ws -> {
                Optional<String> addr = Optional.empty();
                Optional<String> ua = Optional.empty();
                try {
                    if (ws instanceof DefaultWebSocket dws) {
                        try {
                            addr = Optional.of((String) dws.getUpgradeRequest().getAttribute(ATTR_WS_REMOTE_ADDRESS));
                        } catch (Exception ignore) {
                        }
                        try {
                            ua = Optional.of((String) dws.getUpgradeRequest().getAttribute(ATTR_WS_USER_AGENT));
                        } catch (Exception ignore) {
                        }
                        return new BisqConnectClientInfo(addr, ua);
                    }
                } catch (Exception ignore) {
                }
                return new BisqConnectClientInfo(addr, ua);
            }).collect(Collectors.toSet()));
        } catch (Exception t) {
            log.warn("Could not notify clients listeners", t);
        }
    }
}
