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

package bisq.api.rest_api;

import bisq.api.ApiConfig;
import bisq.api.access.http.PairingJdkHttpAdapter;
import bisq.api.access.http.PairingRequestHandler;
import bisq.api.rest_api.util.StaticFileHandler;
import bisq.common.application.Service;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html or http://localhost:8082/doc/v1/index.html in case RestAPI
 * is used without websockets
 */
@Slf4j
public class RestApiService implements Service {

    private final ApiConfig apiConfig;
    private final PairingRequestHandler pairingRequestHandler;
    private final BaseRestApiResourceConfig restApiResourceConfig;
    private Optional<HttpServer> httpServer = Optional.empty();

    public RestApiService(ApiConfig apiConfig,
                          PairingRequestHandler pairingRequestHandler,
                          BaseRestApiResourceConfig restApiResourceConfig,
                          Path appDataDirPath,
                          SecurityService securityService,
                          NetworkService networkService
    ) {
        this.apiConfig = apiConfig;
        this.pairingRequestHandler = pairingRequestHandler;
        this.restApiResourceConfig = restApiResourceConfig;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isRestEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            URI uri = URI.create(apiConfig.getRestServerUrl());
            HttpServer server = JdkHttpServerFactory.createHttpServer(uri, restApiResourceConfig);
            httpServer = Optional.of(server);

            // Pairing endpoint (bootstrap)
            server.createContext(
                    "/pair",
                    new PairingJdkHttpAdapter(pairingRequestHandler)
            );

            addStaticFileHandler("/doc", new StaticFileHandler("/doc/v1/"));
            log.info("Server started at {}.", apiConfig.getRestServerUrl());
            return true;
        }, commonForkJoinPool());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (!apiConfig.isRestEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            httpServer.ifPresent(httpServer -> httpServer.stop(1));
            return true;
        }, commonForkJoinPool());
    }

    public void addStaticFileHandler(String path, StaticFileHandler handler) {
        httpServer.ifPresent(httpServer -> httpServer.createContext(path, handler));
    }
}
