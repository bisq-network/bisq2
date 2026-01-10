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
import bisq.api.ApiTorOnionService;
import bisq.api.auth.AuthenticationAddOn;
import bisq.api.auth.WebSocketMetadataAddOn;
import bisq.api.validator.WebSocketRequestValidator;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.api.web_socket.util.GrizzlySwaggerHttpHandler;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
import java.util.function.Consumer;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class WebSocketService implements Service {
    @Getter
    private final ApiConfig apiConfig;
    private final WebSocketRestApiResourceConfig restApiResourceConfig;

    private final WebSocketConnectionHandler webSocketConnectionHandler;
    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    private Optional<HttpServer> httpServer = Optional.empty();
    private final ApiTorOnionService apiTorOnionService;
    private final Observable<Boolean> initializedObservable = new Observable<>(false);
    private final Observable<String> errorObservable = new Observable<>("");
    private final Observable<Optional<Address>> addressObservable = new Observable<>(Optional.empty());

    public WebSocketService(ApiConfig apiConfig,
                            WebSocketRestApiResourceConfig restApiResourceConfig,
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
        this.restApiResourceConfig = restApiResourceConfig;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());

        //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        subscriptionService = new SubscriptionService(objectMapper,
                bondedRolesService,
                chatService,
                tradeService,
                userService,
                bisqEasyService,
                openTradeItemsService);
        WebSocketRequestValidator requestValidator = new WebSocketRequestValidator(apiConfig.getRestAllowEndpoints(), apiConfig.getRestDenyEndpoints());
        webSocketRestApiService = new WebSocketRestApiService(objectMapper, apiConfig.getRestServerUrl(), requestValidator);
        webSocketConnectionHandler = new WebSocketConnectionHandler(subscriptionService, webSocketRestApiService);

        boolean publishOnionService = true; // TODO apiConfig.isPublishOnionService();
        apiTorOnionService = new ApiTorOnionService(appDataDirPath, securityService, networkService, apiConfig.getBindPort(), "webSocketServer", publishOnionService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isWebsocketEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        String webSocketProtocol = apiConfig.getWebSocketProtocol();
        String restProtocol = apiConfig.getRestProtocol();
        String bindHost = apiConfig.getBindHost();
        int bindPort = apiConfig.getBindPort();

        CompletableFuture<Boolean> initFuture = CompletableFuture.supplyAsync(() -> {
                    // We use `http(s)` not `ws(s)` here
                    String uri = restProtocol + "://" + bindHost + "/";
                    URI baseUri = UriBuilder.fromUri(uri).port(bindPort).build();
                    boolean restEnabled = apiConfig.isRestEnabled();
                    HttpServer server = restEnabled
                            ? GrizzlyHttpServerFactory.createHttpServer(baseUri, restApiResourceConfig, false)
                            : GrizzlyHttpServerFactory.createHttpServer(baseUri, false);
                    httpServer = Optional.of(server);
                    server.getListener("grizzly").registerAddOn(new WebSocketMetadataAddOn());
                    String password = ""; //TODO
                    if (StringUtils.isNotEmpty(password)) {
                        server.getListener("grizzly").registerAddOn(new AuthenticationAddOn(password));
                    }
                    server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
                    WebSocketEngine.getEngine().register("", "/websocket", webSocketConnectionHandler);

                    if (restEnabled) {
                        server.getServerConfiguration().addHttpHandler(new GrizzlySwaggerHttpHandler(), "/doc/v1/");
                    }

                    try {
                        server.start();
                        log.info("Server started at {}", baseUri);
                        log.info("WebSocket endpoint available at: {}/websocket", apiConfig.getWebSocketServerUrl());
                        if (restEnabled) {
                            log.info("Rest API endpoints available at: {}", apiConfig.getRestServerUrl());
                        }
                    } catch (IOException e) {
                        log.error("Failed to start websocket server", e);
                        server.shutdownNow();
                        initializedObservable.set(false);
                        notifyErrorListeners(e.getMessage());
                        return false;
                    }
                    initializedObservable.set(true);
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
                .thenCompose(result -> apiTorOnionService.initialize());
        initFuture.whenComplete((res, ex) -> {
            if (ex != null) {
                notifyErrorListeners(ex.getMessage());
            } else if (res != null && res) {
                if (isPublishOnionService()) {
                    addressObservable.set(apiTorOnionService.getPublishedAddress());
                } else {
                    addressObservable.set(Optional.of(new ClearnetAddress("0.0.0.0", bindPort)));
                }
            } else {
                initializedObservable.set(false);
                notifyErrorListeners("Initialization completed with failure");
            }
        });
        return initFuture;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (!apiConfig.isWebsocketEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        return subscriptionService.shutdown()
                .thenCompose(r -> webSocketRestApiService.shutdown())
                .thenCompose(r -> webSocketConnectionHandler.shutdown())
                .thenCompose(r -> CompletableFuture.supplyAsync(() -> {
                    httpServer.ifPresent(HttpServer::shutdown);
                    httpServer = Optional.empty();
                    return true;
                }, commonForkJoinPool()));
    }

    public ObservableSet<BisqConnectClientInfo> getWebsocketClients() {
        return webSocketConnectionHandler.getWebsocketClients();
    }

    public Boolean isPublishOnionService() {
        return apiTorOnionService.isPublishOnionService();
    }

    public Pin addInitObserver(Consumer<Boolean> observer) {
        return initializedObservable.addObserver(observer);
    }

    public Pin addErrorObserver(Consumer<String> observer) {
        return errorObservable.addObserver(observer);
    }

    public Pin addAddressObserver(Consumer<Optional<Address>> observer) {
        return addressObservable.addObserver(observer);
    }

    private void notifyErrorListeners(String msg) {
        errorObservable.set(msg == null ? "" : msg);
    }
}
