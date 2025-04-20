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
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.http_api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.http_api.web_socket.subscription.SubscriptionService;
import bisq.http_api.web_socket.util.GrizzlySwaggerHttpHandler;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class WebSocketService implements Service {
    @Getter
    public static class Config {
        private final boolean enabled;
        private final boolean includeRestApi;
        private final String protocol;
        private final String host;
        private final int port;
        private final boolean localhostOnly;
        private final List<String> whiteListEndPoints;
        private final List<String> blackListEndPoints;
        private final List<String> supportedAuth;
        private final String restApiBaseAddress;
        private final String restApiBaseUrl;

        public Config(boolean enabled,
                      boolean includeRestApi,
                      String protocol,
                      String host,
                      int port,
                      boolean localhostOnly,
                      List<String> whiteListEndPoints,
                      List<String> blackListEndPoints,
                      List<String> supportedAuth) {
            this.enabled = enabled;
            this.includeRestApi = includeRestApi;
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.localhostOnly = localhostOnly;
            this.whiteListEndPoints = whiteListEndPoints;
            this.blackListEndPoints = blackListEndPoints;
            this.supportedAuth = supportedAuth;

            restApiBaseAddress = protocol + host + ":" + port;
            restApiBaseUrl = restApiBaseAddress + REST_API_BASE_PATH;
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
                    config.getStringList("supportedAuth")
            );
        }
    }

    public static final String REST_API_BASE_PATH = "/api/v1";

    private final Config config;

    private final WebSocketRestApiResourceConfig restApiResourceConfig;

    private final WebSocketConnectionHandler webSocketConnectionHandler;
    private final SubscriptionService subscriptionService;
    private final WebSocketRestApiService webSocketRestApiService;
    private Optional<HttpServer> httpServer = Optional.empty();

    public WebSocketService(Config config,
                            String restApiBaseAddress,
                            WebSocketRestApiResourceConfig restApiResourceConfig,
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
        webSocketRestApiService = new WebSocketRestApiService(objectMapper, restApiBaseAddress);
        webSocketConnectionHandler = new WebSocketConnectionHandler(subscriptionService, webSocketRestApiService);

        if (config.isEnabled() && config.isLocalhostOnly()) {
            String host = config.getHost();
            checkArgument(host.equals("127.0.0.1") || host.equals("localhost"),
                    "The localhostOnly flag is set true but the server host is not localhost. host=" + host);
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.supplyAsync(() -> {
                    String protocol = config.getProtocol();
                    String host = config.getHost();
                    int port = config.getPort();
                    URI baseUri = UriBuilder.fromUri(protocol + host + "/").port(port).build();
                    HttpServer server = config.includeRestApi
                            ? GrizzlyHttpServerFactory.createHttpServer(baseUri, restApiResourceConfig, false)
                            : GrizzlyHttpServerFactory.createHttpServer(baseUri, false);
                    httpServer = Optional.of(server);
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
                        return false;
                    }
                    return true;
                })
                .thenCompose(result -> {
                    if (result) {
                        return webSocketConnectionHandler.initialize()
                                .thenCompose(r -> webSocketRestApiService.initialize())
                                .thenCompose(r -> subscriptionService.initialize());
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                });
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
                }));
    }
}
