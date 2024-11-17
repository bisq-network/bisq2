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

import bisq.application.Executable;
import bisq.common.threading.ThreadName;
import bisq.rest_api.util.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

import java.net.URI;
import java.util.Optional;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8082/doc/v1/index.html
 */
@Slf4j
public class RestApiApp extends Executable<RestApiApplicationService> {
    public static void main(String[] args) {
        ThreadName.set(RestApiApp.class, "main");
        new RestApiApp(args);
    }

    public static final String BASE_PATH = "/api/v1";
    private String baseUrl;

    private RestApiResourceConfig restApiResourceConfig;
    private Optional<HttpServer> httpServer = Optional.empty();

    public RestApiApp(String[] args) {
        super(args);
    }

    @Override
    protected void launchApplication(String[] args) {
        Config restApiConfig = applicationService.getRestApiConfig();
        String host = restApiConfig.getString("host");
        int port = restApiConfig.getInt("port");
        baseUrl = host + ":" + port + BASE_PATH;

        restApiResourceConfig = new RestApiResourceConfig(applicationService, baseUrl);

        super.launchApplication(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        var server = JdkHttpServerFactory.createHttpServer(URI.create(baseUrl), restApiResourceConfig);
        server.createContext("/doc", new StaticFileHandler("/doc/v1/"));
        server.createContext("/node-monitor", new StaticFileHandler("/node-monitor/"));
        log.info("Server started at {}.", baseUrl);
        httpServer = Optional.of(server);
    }

    @Override
    protected RestApiApplicationService createApplicationService(String[] args) {
        return new RestApiApplicationService(args);
    }

    @Override
    public void shutdown() {
        httpServer.ifPresent(httpServer -> httpServer.stop(1));

        super.shutdown();
    }
}
