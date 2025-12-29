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
import bisq.http_api.ApiTorOnionService;
import bisq.http_api.auth.AuthenticationAddOn;
import bisq.http_api.auth.WebSocketAddressAddOn;
import bisq.http_api.config.CommonApiConfig;
import bisq.http_api.validator.WebSocketRequestValidator;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.http_api.web_socket.subscription.SubscriptionService;
import bisq.http_api.web_socket.util.GrizzlySwaggerHttpHandler;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class WebSocketService implements Service {
    @Getter
    public static class Config extends CommonApiConfig {
        private final boolean includeRestApi;

        public Config(boolean enabled,
                      boolean includeRestApi,
                      String protocol,
                      String host,
                      int port,
                      boolean localhostOnly,
                      List<String> whiteListEndPoints,
                      List<String> blackListEndPoints,
                      List<String> supportedAuth,
                      String password,
                      boolean publishOnionService) {
            super(enabled, protocol, host, port, localhostOnly, whiteListEndPoints, blackListEndPoints, supportedAuth, password, publishOnionService);
            this.includeRestApi = includeRestApi;
        }

        public static Config from(com.typesafe.config.Config config) {
            com.typesafe.config.Config server = config.getConfig("server");
            return new Config(
                    config.getBoolean("enabled"),
                    config.getBoolean("includeRestApi"),
                    server.getString("protocol"),
                    server.getString("host"),
                    server.getInt("port"),
                    config.getBoolean("localhostOnly"),
                    config.getStringList("whiteListEndPoints"),
                    config.getStringList("blackListEndPoints"),
                    config.getStringList("supportedAuth"),
                    config.getString("password"),
                    config.getBoolean("publishOnionService")
            );
        }
    }

    private final Config config;

    private final WebSocketRestApiResourceConfig restApiResourceConfig;

    private final WebSocketConnectionHandler webSocketConnectionHandler;
    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    private Optional<HttpServer> httpServer = Optional.empty();
    private final ApiTorOnionService apiTorOnionService;
    private final Observable<Boolean> initializedObservable = new Observable<>(false);
    private final Observable<String> errorObservable = new Observable<>("");
    private final Observable<Optional<Address>> addressObservable = new Observable<>(Optional.empty());

    public WebSocketService(Config config,
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
        this.config = config;
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
        WebSocketRequestValidator requestValidator = WebSocketRequestValidator.from(config);
        webSocketRestApiService = new WebSocketRestApiService(objectMapper, config.getRestApiBaseAddress(), requestValidator);
        webSocketConnectionHandler = new WebSocketConnectionHandler(subscriptionService, webSocketRestApiService);

        if (config.isEnabled() && config.isLocalhostOnly()) {
            String host = config.getHost();
            checkArgument(host.equals("127.0.0.1") || host.equals("localhost"),
                    "The localhostOnly flag is set true but the server host is not localhost. host=" + host);
        }

        apiTorOnionService = new ApiTorOnionService(appDataDirPath, securityService, networkService, config.getPort(), "webSocketServer", config.isPublishOnionService());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> initFuture = CompletableFuture.supplyAsync(() -> {
                    String protocol = config.getProtocol();
                    String host = config.getHost();
                    int port = config.getPort();
                    URI baseUri = UriBuilder.fromUri(protocol + host + "/").port(port).build();
                    HttpServer server = config.includeRestApi
                            ? GrizzlyHttpServerFactory.createHttpServer(baseUri, restApiResourceConfig, false)
                            : GrizzlyHttpServerFactory.createHttpServer(baseUri, false);
                    httpServer = Optional.of(server);
                    server.getListener("grizzly").registerAddOn(new WebSocketAddressAddOn());
                    String password = config.getPassword();
                    if (StringUtils.isNotEmpty(password)) {
                        server.getListener("grizzly").registerAddOn(new AuthenticationAddOn(password));
                    }
                    server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
                    WebSocketEngine.getEngine().register("", "/websocket", webSocketConnectionHandler);

                    if (config.isIncludeRestApi()) {
                        server.getServerConfiguration().addHttpHandler(new GrizzlySwaggerHttpHandler(), "/doc/v1/");
                    }

                    try {
                        server.start();
                        log.info("Server started at {}", baseUri);
                        log.info("WebSocket endpoint available at 'ws://{}:{}/websocket'", host, port);
                        if (config.isIncludeRestApi()) {
                            log.info("Rest API endpoints available at '{}'", config.getRestApiBaseUrl());
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
                    addressObservable.set(Optional.of(new ClearnetAddress("0.0.0.0", config.getPort())));
                }
            } else {
                notifyErrorListeners("Initialization completed with failure");
            }
        });
        return initFuture;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
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
