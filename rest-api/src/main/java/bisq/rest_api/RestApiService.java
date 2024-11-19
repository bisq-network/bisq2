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

package bisq.rest_api;

import bisq.common.application.Service;
import bisq.rest_api.util.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.ConfigList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8082/doc/v1/index.html
 */
@Slf4j
public class RestApiService implements Service {
    @Getter
    public static class Config {
        private final boolean enabled;
        private final String host;
        private final int port;
        private final boolean localhostOnly;
        private final ConfigList whiteListEndPoints;
        private final ConfigList blackListEndPoints;
        private final ConfigList supportedAuth;
        private final String baseUrl;

        public Config(boolean enabled,
                      String host,
                      int port,
                      boolean localhostOnly,
                      ConfigList whiteListEndPoints,
                      ConfigList blackListEndPoints,
                      ConfigList supportedAuth) {
            this.enabled = enabled;
            this.host = host;
            this.port = port;
            this.localhostOnly = localhostOnly;
            this.whiteListEndPoints = whiteListEndPoints;
            this.blackListEndPoints = blackListEndPoints;
            this.supportedAuth = supportedAuth;

            baseUrl = host + ":" + port + BASE_PATH;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getBoolean("enabled"),
                    config.getConfig("server").getString("host"),
                    config.getConfig("server").getInt("port"),
                    config.getBoolean("localhostOnly"),
                    config.getList("whiteListEndPoints"),
                    config.getList("blackListEndPoints"),
                    config.getList("supportedAuth")
            );
        }
    }

    public static final String BASE_PATH = "/api/v1";
    private final RestApiService.Config config;
    private final BaseRestApiResourceConfig restApiResourceConfig;
    private Optional<HttpServer> httpServer = Optional.empty();

    public RestApiService(Config config,
                          BaseRestApiResourceConfig restApiResourceConfig) {
        this.config = config;
        this.restApiResourceConfig = restApiResourceConfig;

        if (config.isEnabled() && config.isLocalhostOnly()) {
            String host = config.getHost();
            checkArgument(host.equals("http://127.0.0.1") || host.equals("http://localhost"),
                    "The localhostOnly flag is set true but the server host is not localhost. host=" + host);
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (config.isEnabled()) {
            return CompletableFuture.supplyAsync(() -> {
                HttpServer server = JdkHttpServerFactory.createHttpServer(URI.create(config.getBaseUrl()), restApiResourceConfig);
                httpServer = Optional.of(server);
                addDocs();
                log.info("Server started at {}.", config.getBaseUrl());
                return true;
            });
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }

    protected void addDocs() {
        addStaticFileHandler("/doc", new StaticFileHandler("/doc/v1/"));
    }

    public void addStaticFileHandler(String path, StaticFileHandler handler) {
        httpServer.ifPresent(httpServer -> httpServer.createContext(path, handler));
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> {
            httpServer.ifPresent(httpServer -> httpServer.stop(1));
            return true;
        });
    }
}
