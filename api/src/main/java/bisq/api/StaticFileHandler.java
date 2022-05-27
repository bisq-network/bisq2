package bisq.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

/**
 * JDK Server needs handler for serving files, will change in JDK 18
 * Currently this is only to serve the swagger-ui content to the client.
 * So any call to this handler must begin with api/v1. We keep v1 in case
 * we will have incompatible changes in the future.
 * This handler is limited to html,css,json and javascript files.
 */
@Slf4j
@RequiredArgsConstructor
public class StaticFileHandler implements HttpHandler {
    private static final String NOT_FOUND = "404 (Not Found)\n";

    public static final String[] VALID_SUFFIX = {".html", ".json", ".css", ".js"};

    @Getter
    @Setter
    @NonNull
    protected String rootContext;
    ClassLoader classLoader = getClass().getClassLoader();

    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        log.debug("requesting: " + uri.getPath());
        String filename = uri.getPath();
        if (filename == null || !filename.startsWith(rootContext) ||
                Arrays.stream(VALID_SUFFIX).noneMatch(filename::endsWith)) {
            respond404(exchange);
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
                respond404(exchange);
                return;
            }
            log.debug("sending: " + resourceName);
            // Object exists and is a file: accept with response code 200.
            String mime = "text/html";
            if (resourceName.endsWith(".js")) mime = "application/javascript";
            if (resourceName.endsWith(".json")) mime = "application/json";
            if (resourceName.endsWith(".css")) mime = "text/css";
            if (resourceName.endsWith(".png")) mime = "image/png";

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", mime);
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                byte[] buffer = new byte[0x10000];
                int count;
                while ((count = resource.read(buffer)) >= 0) {
                    outputStream.write(buffer, 0, count);
                }
            }
        }
    }

    private void respond404(HttpExchange exchange) throws IOException {
        // Object does not exist or is not a file: reject with 404 error.
        exchange.sendResponseHeaders(404, NOT_FOUND.length());
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(NOT_FOUND.getBytes());
        }
    }
}
