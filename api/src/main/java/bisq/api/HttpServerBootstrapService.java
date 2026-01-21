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
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.access.transport.TlsContext;
import bisq.api.access.transport.TlsContextService;
import bisq.api.web_socket.WebSocketService;
import bisq.api.web_socket.util.GrizzlySwaggerHttpHandler;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static com.google.common.base.Preconditions.checkArgument;

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
    private final ResourceConfig resourceConfig;
    private final Optional<WebSocketService> webSocketService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final PermissionService<RestPermissionMapping> permissionService;
    private final TlsContextService tlsContextService;

    private Optional<HttpServer> httpServer = Optional.empty();
    private final Observable<String> errorMessage = new Observable<>();
    private final Observable<State> state = new Observable<>(State.NEW);

    public HttpServerBootstrapService(ApiConfig apiConfig,
                                      ResourceConfig resourceConfig,
                                      Optional<WebSocketService> webSocketService,
                                      SessionAuthenticationService sessionAuthenticationService,
                                      PermissionService<RestPermissionMapping> permissionService,
                                      TlsContextService tlsContextService
    ) {
        this.apiConfig = apiConfig;
        this.resourceConfig = resourceConfig;
        this.webSocketService = webSocketService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.permissionService = permissionService;
        this.tlsContextService = tlsContextService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        setState(State.STARTING);

        return CompletableFuture.supplyAsync(() -> {
                    String webSocketProtocol = apiConfig.getWebSocketProtocol();
                    String bindHost = apiConfig.getBindHost();
                    int bindPort = apiConfig.getBindPort();

                    URI baseUri = UriBuilder
                            .fromUri(webSocketProtocol + "://" + bindHost + "/")
                            .port(bindPort)
                            .build();

                    // In case we do not use the rest api we still provide the ResourceConfig with basic JsonMapper config
                    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
                    httpServer = Optional.of(server);
                    NetworkListener networkListener = server.getListener("grizzly");
                    ServerConfiguration serverConfiguration = server.getServerConfiguration();

                    boolean websocketEnabled = apiConfig.isWebsocketEnabled();
                    if (websocketEnabled) {
                        checkArgument(webSocketService.isPresent(), "If websocketEnabled is true we expect that webSocketService is present");
                        networkListener.registerAddOn(new WebSocketAddOn());
                        WebSocketEngine.getEngine().register("", "/websocket", webSocketService.get().getWebSocketConnectionHandler());
                    }

                    serverConfiguration.addHttpHandler(new GrizzlySwaggerHttpHandler(), "/doc/v1/");

                    // todo filters not called
                    networkListener.registerAddOn(new AccessFilterAddOn(apiConfig, permissionService, sessionAuthenticationService));

                    if (apiConfig.isTlsRequired()) {
                        Optional<TlsContext> tlsContext;
                        try {
                            tlsContext = tlsContextService.getOrCreateTlsContext();
                        } catch (Exception e) {
                            log.error("Could not create TLS context", e);
                            return false;
                        }
                        checkArgument(tlsContext.isPresent(), "If tlsRequired is true we expect that tlsContext is present");

                        networkListener.setSecure(true);

                        SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(tlsContext.get().getSslContext())
                                .setClientMode(false)       // server mode
                                .setNeedClientAuth(false)  // not mutual TLS
                                .setEnabledProtocols(new String[]{"TLSv1.3"})
                                .setEnabledCipherSuites(new String[]{
                                        "TLS_AES_128_GCM_SHA256",
                                        "TLS_AES_256_GCM_SHA384",
                                        "TLS_CHACHA20_POLY1305_SHA256"
                                });
                        networkListener.setSSLEngineConfig(sslEngineConfigurator);
                    }

                    try {
                        server.start();
                        log.info("Server started at {}", baseUri);
                        log.info("WebSocket endpoint available at '{}://{}:{}/websocket'", apiConfig.getWebSocketProtocol(), bindHost, bindPort);
                        if (apiConfig.isRestEnabled()) {
                            log.info("Rest API endpoints available at '{}'", apiConfig.getRestServerApiBasePath());
                        } else {
                            log.info("Rest API is disabled but pairing endpoint is available at '{}/pairing'",
                                    apiConfig.getRestServerApiBasePath());
                        }
                    } catch (BindException e) {
                        log.error("Failed to start websocket server", e);
                        server.shutdownNow();
                        errorMessage.set(e.getMessage());
                        // TODO clear custom api config
                        return true;
                    }catch (IOException e) {
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
                    } else {
                        return webSocketService.map(WebSocketService::initialize)
                                .orElse(CompletableFuture.completedFuture(true));
                    }
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        errorMessage.set(throwable.getMessage());
                    } else if (result != null && result) {
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

        return webSocketService.map(WebSocketService::shutdown).orElse(CompletableFuture.completedFuture(true))
                .thenCompose(result -> CompletableFuture.supplyAsync(() -> {
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
        httpServer.ifPresentOrElse(httpServer ->
                        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(context), path),
                () -> log.error("addStaticFileHandler called before httpServer is set."));
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
