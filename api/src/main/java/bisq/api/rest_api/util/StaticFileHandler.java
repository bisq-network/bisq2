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

package bisq.api.rest_api.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Server needs handler for serving files, will change in JDK 18
 * Currently this is only to serve the swagger-ui content to the client.
 * So any call to this handler must begin with api/v1. We keep v1 in case
 * we will have incompatible changes in the future.
 * This handler is limited to html,css,json and javascript files.
 */
@Slf4j
public class StaticFileHandler extends HttpHandler {
    private static final String NOT_FOUND = "404 (Not Found)\n";

    public static final String[] VALID_SUFFIX = {".html", ".json", ".css", ".js"};

    public StaticFileHandler(@NonNull String rootContext) {
        this.rootContext = rootContext;
    }

    @Getter
    @Setter
    @NonNull
    protected String rootContext;
    final ClassLoader classLoader = getClass().getClassLoader();

    public void service(Request request, Response response) throws IOException {
        String filename = request.getRequestURI();

        log.debug("requesting: {}", filename);
        if (filename == null || !filename.startsWith(rootContext) ||
                Arrays.stream(VALID_SUFFIX).noneMatch(filename::endsWith)) {
            respond404(response);
            return;
        }
        // resource loading without leading slash
        String resourceName = filename.replace("..", "");
        if (filename.charAt(0) == '/') {
            resourceName = filename.substring(1);
        }

        // we are using getResourceAsStream to ultimately prevent load from parent directories
        try (InputStream resource = classLoader.getResourceAsStream(resourceName)) {
            if (resource == null) {
                respond404(response);
                return;
            }
            log.debug("sending: {}", resourceName);
            // Object exists and is a file: accept with response code 200.
            String mime = "text/html";
            if (resourceName.endsWith(".js")) mime = "application/javascript";
            if (resourceName.endsWith(".json")) mime = "application/json";
            if (resourceName.endsWith(".css")) mime = "text/css";
            if (resourceName.endsWith(".png")) mime = "image/png";

            response.setHeader("Content-Type", mime);
            response.setStatus(200);

            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[0x10000];
                int count;
                while ((count = resource.read(buffer)) >= 0) {
                    outputStream.write(buffer, 0, count);
                }
            }
        }
    }

    private void respond404(Response response) throws IOException {
        // Object does not exist or is not a file: reject with 404 error.
        response.setStatus(404);
        try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(NOT_FOUND.getBytes());
        }
    }
}
