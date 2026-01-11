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

import bisq.api.ApiConfig;
import bisq.api.access.filter.AccessFilterAddOn;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.http.PairingGrizzlyHttpAdapter;
import bisq.api.access.http.PairingRequestHandler;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.api.web_socket.util.GrizzlySwaggerHttpHandler;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import jakarta.ws.rs.core.UriBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class WebSocketService implements Service {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    @Getter
    private final ApiConfig apiConfig;
    private final PairingRequestHandler apiAccessService;
    private final WebSocketRestApiResourceConfig webSocketRestApiResourceConfig;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final PermissionService<RestPermissionMapping> permissionService;
    private final WebSocketConnectionHandler webSocketConnectionHandler;
    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    private Optional<HttpServer> httpServer = Optional.empty();
    private final Observable<String> errorMessage = new Observable<>();
    private final Observable<State> state = new Observable<>(State.NEW);

    public WebSocketService(ApiConfig apiConfig,
                            PairingRequestHandler apiAccessService,
                            WebSocketRestApiResourceConfig webSocketRestApiResourceConfig,
                            SessionAuthenticationService sessionAuthenticationService,
                            PermissionService<RestPermissionMapping> permissionService,
                            Path appDataDirPath,
                            SecurityService securityService,
                            NetworkService networkService,
                            BondedRolesService bondedRolesService,
                            ChatService chatService,
                            TradeService tradeService,
                            UserService userService,
                            BisqEasyService bisqEasyService,
                            OpenTradeItemsService openTradeItemsService) {
        this.apiConfig = apiConfig;
        this.apiAccessService = apiAccessService;
        this.webSocketRestApiResourceConfig = webSocketRestApiResourceConfig;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.permissionService = permissionService;

        subscriptionService = new SubscriptionService(bondedRolesService,
                chatService,
                tradeService,
                userService,
                bisqEasyService,
                openTradeItemsService);
        webSocketRestApiService = new WebSocketRestApiService(apiConfig, apiConfig.getRestServerUrl());
        webSocketConnectionHandler = new WebSocketConnectionHandler(subscriptionService, webSocketRestApiService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        setState(State.STARTING);
        return CompletableFuture.supplyAsync(() -> {
                    String protocol = apiConfig.getWebSocketProtocol();
                    String bindHost = apiConfig.getBindHost();
                    int bindPort = apiConfig.getBindPort();

                    URI baseUri = UriBuilder
                            .fromUri(protocol + "://" + bindHost + "/")
                            .port(bindPort)
                            .build();

                    boolean restEnabled = apiConfig.isRestEnabled();
                    HttpServer server = restEnabled
                            ? GrizzlyHttpServerFactory.createHttpServer(baseUri, webSocketRestApiResourceConfig, false)
                            : GrizzlyHttpServerFactory.createHttpServer(baseUri, false);
                    httpServer = Optional.of(server);
                    server.getListener("grizzly").registerAddOn(new WebSocketAddOn());

                    if (apiConfig.isAuthRequired()) {
                        server.getListener("grizzly").registerAddOn(new AccessFilterAddOn(permissionService, sessionAuthenticationService));
                    }

                    WebSocketEngine.getEngine().register("", "/websocket", webSocketConnectionHandler);

                    server.getServerConfiguration().addHttpHandler(new PairingGrizzlyHttpAdapter(apiAccessService), "/pair");

                    if (restEnabled) {
                        server.getServerConfiguration().addHttpHandler(new GrizzlySwaggerHttpHandler(), "/doc/v1/");
                    }

                    try {
                        server.start();
                        log.info("Server started at {}", baseUri);
                        log.info("WebSocket endpoint available at 'ws://{}:{}/websocket'", bindHost, bindPort);
                        if (restEnabled) {
                            log.info("Rest API endpoints available at '{}'", apiConfig.getRestServerApiBasePath());
                        }
                    } catch (IOException e) {
                        log.error("Failed to start websocket server", e);
                        server.shutdownNow();
                        errorMessage.set(e.getMessage());
                        return false;
                    }
                    return true;
                }, commonForkJoinPool())
                .thenCompose(result -> {
                    if (result) {
                        return webSocketConnectionHandler.initialize()
                                .thenCompose(r -> webSocketRestApiService.initialize())
                                .thenCompose(r -> subscriptionService.initialize());
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        errorMessage.set(ex.getMessage());
                    } else if (res != null && res) {
                        setState(State.RUNNING);
                    } else {
                        errorMessage.set("Initialization completed without success state");
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        setState(State.STOPPING);
        return subscriptionService.shutdown()
                .thenCompose(r -> webSocketRestApiService.shutdown())
                .thenCompose(r -> webSocketConnectionHandler.shutdown())
                .thenCompose(r -> CompletableFuture.supplyAsync(() -> {
                    httpServer.ifPresent(HttpServer::shutdown);
                    httpServer = Optional.empty();
                    setState(State.TERMINATED);
                    return true;
                }, commonForkJoinPool()));
    }

    public ObservableSet<WebsocketClient1> getWebsocketClients() {
        return webSocketConnectionHandler.getWebsocketClients();
    }

    public ReadOnlyObservable<State> getState() {
        return state;
    }

    private void setState(State value) {
        log.info("New state: {}", value);
        state.set(value);
    }

    public ReadOnlyObservable<String> getErrorMessage() {
        return errorMessage;
    }
}
