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

package bisq.http_api.web_socket.util;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class GrizzlySwaggerHttpHandler extends HttpHandler {
    private final ClassLoader classLoader = getClass().getClassLoader();

    public GrizzlySwaggerHttpHandler() {
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        String resourceName = request.getRequestURI();
        if (resourceName.startsWith("/")) {
            resourceName = resourceName.substring(1);
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                response.setStatus(404);
                response.getWriter().write("404 Not Found");
                return;
            }

            response.setStatus(200);
            URL resourceUrl = classLoader.getResource(resourceName);
            if (resourceUrl != null) {
                Path path = Paths.get(resourceUrl.toURI());
                response.setContentLengthLong(Files.size(path));
            }
            inputStream.transferTo(response.getOutputStream());
        } catch (IOException e) {
            response.setStatus(500);
            response.getWriter().write("500 Internal Server Error");
            log.error("Error", e);
        }
    }
}


