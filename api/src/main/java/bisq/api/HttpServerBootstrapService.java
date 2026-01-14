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

package bisq.api;

import bisq.api.access.filter.AccessFilterAddOn;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.http.PairingGrizzlyHttpAdapter;
import bisq.api.access.http.PairingRequestHandler;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.rest_api.RestApiService;
import bisq.api.rest_api.util.StaticFileHandler;
import bisq.api.web_socket.WebSocketService;
import bisq.api.web_socket.util.GrizzlySwaggerHttpHandler;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class HttpServerBootstrapService implements Service {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    private final ApiConfig apiConfig;
    private final RestApiService restApiService;
    @Nullable
    private final WebSocketService webSocketService;
    private final PairingRequestHandler pairingRequestHandler;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final PermissionService<RestPermissionMapping> permissionService;

    private Optional<HttpServer> httpServer = Optional.empty();
    private final Observable<String> errorMessage = new Observable<>();
    private final Observable<State> state = new Observable<>(State.NEW);

    public HttpServerBootstrapService(ApiConfig apiConfig,
                                      RestApiService restApiService,
                                      @Nullable WebSocketService webSocketService,
                                      PairingRequestHandler pairingRequestHandler,
                                      SessionAuthenticationService sessionAuthenticationService,
                                      PermissionService<RestPermissionMapping> permissionService
    ) {
        this.apiConfig = apiConfig;
        this.restApiService = restApiService;
        this.webSocketService = webSocketService;
        this.pairingRequestHandler = pairingRequestHandler;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.permissionService = permissionService;
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
                    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, restApiService.getRestApiResourceConfig(), false);
                    httpServer = Optional.of(server);
                    server.getListener("grizzly").registerAddOn(new WebSocketAddOn());

                    if (apiConfig.isAuthRequired()) {
                        server.getListener("grizzly").registerAddOn(new AccessFilterAddOn(permissionService, sessionAuthenticationService));
                    }

                    if (apiConfig.isWebsocketEnabled() && webSocketService != null) {
                        WebSocketEngine.getEngine().register("", "/websocket", webSocketService.getWebSocketConnectionHandler());
                    }

                    server.getServerConfiguration().addHttpHandler(new PairingGrizzlyHttpAdapter(pairingRequestHandler), "/pair");

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
                    if (!result) {
                        return CompletableFuture.completedFuture(false);
                    }

                    CompletableFuture<Boolean> restInit = restApiService.initialize();

                    CompletableFuture<Boolean> wsInit = webSocketService == null
                            ? CompletableFuture.completedFuture(true)
                            : webSocketService.initialize();

                    return CompletableFuture.allOf(restInit, wsInit)
                            .thenApply(v -> restInit.join() && wsInit.join());
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

        CompletableFuture<?> restShutdown = restApiService.shutdown();

        CompletableFuture<?> wsShutdown = webSocketService == null
                ? CompletableFuture.completedFuture(null)
                : webSocketService.shutdown();

        return CompletableFuture.allOf(restShutdown, wsShutdown)
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    httpServer.ifPresent(HttpServer::shutdown);
                    httpServer = Optional.empty();
                    setState(State.TERMINATED);
                    return true;
                }, commonForkJoinPool()))
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        errorMessage.set(ex.getMessage());
                    }
                });
    }

    public void addStaticFileHandler(String path, String context) {
        httpServer.ifPresent(httpServer ->
                httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(context), path));
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
