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

package bisq.wallets.electrum.notifications;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

@Slf4j
public class ElectrumNotifyWebServer extends ResourceConfig {

    private final String baseUrl;
    private HttpServer httpServer;

    public ElectrumNotifyWebServer(int port) {
        super();
        this.baseUrl = "http://localhost:" + port + "/";
    }


    public void startServer() {
        ResourceConfig app = register(ElectrumNotifyApi.class);
        URI uri = URI.create(baseUrl);
        httpServer = JdkHttpServerFactory.createHttpServer(uri, app);

        log.info("Server started at {}.", baseUrl);
    }

    public void stopServer() {
        log.info("Stopping server at {}.", baseUrl);
        httpServer.stop(2);
    }

    public String getNotifyEndpointUrl() {
        return baseUrl + ElectrumNotifyApi.ENDPOINT_NAME;
    }
}
