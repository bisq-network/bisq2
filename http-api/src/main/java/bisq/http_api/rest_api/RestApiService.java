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

package bisq.http_api.rest_api;

import bisq.common.application.Service;
import bisq.http_api.ApiTorOnionService;
import bisq.http_api.config.CommonApiConfig;
import bisq.http_api.rest_api.util.StaticFileHandler;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html or http://localhost:8082/doc/v1/index.html in case RestAPI
 * is used without websockets
 */
@Slf4j
public class RestApiService implements Service {
    @Getter
    public static class Config extends CommonApiConfig {
        public Config(boolean enabled,
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
        }

        public static Config from(com.typesafe.config.Config config) {
            com.typesafe.config.Config server = config.getConfig("server");
            return new Config(
                    config.getBoolean("enabled"),
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

    private final RestApiService.Config config;
    private final BaseRestApiResourceConfig restApiResourceConfig;
    private final ApiTorOnionService apiTorOnionService;
    private Optional<HttpServer> httpServer = Optional.empty();

    public RestApiService(Config config,
                          BaseRestApiResourceConfig restApiResourceConfig,
                          Path appDataDirPath,
                          SecurityService securityService,
                          NetworkService networkService) {
        this.config = config;
        this.restApiResourceConfig = restApiResourceConfig;

        apiTorOnionService = new ApiTorOnionService(appDataDirPath, securityService, networkService, config.getPort(), "restApiServer", config.isPublishOnionService());

        if (config.isEnabled() && config.isLocalhostOnly()) {
            String host = config.getHost();
            checkArgument(host.equals("127.0.0.1") || host.equals("localhost"),
                    "The localhostOnly flag is set true but the server host is not localhost. host=" + host);
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (config.isEnabled()) {
            return CompletableFuture.supplyAsync(() -> {
                        HttpServer server = JdkHttpServerFactory.createHttpServer(URI.create(config.getRestApiBaseUrl()), restApiResourceConfig);
                        httpServer = Optional.of(server);
                        addStaticFileHandler("/doc", new StaticFileHandler("/doc/v1/"));
                        log.info("Server started at {}.", config.getRestApiBaseUrl());
                        return true;
                    }, commonForkJoinPool())
                    .thenCompose(result -> apiTorOnionService.initialize());
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> {
            httpServer.ifPresent(httpServer -> httpServer.stop(1));
            return true;
        }, commonForkJoinPool());
    }

    public void addStaticFileHandler(String path, StaticFileHandler handler) {
        httpServer.ifPresent(httpServer -> httpServer.createContext(path, handler));
    }
}
