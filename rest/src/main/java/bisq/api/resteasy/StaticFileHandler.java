package bisq.api.resteasy;

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

    public static final String[] VALID_SUFFIX = {".html", ".json", ".css", ".js"};

    @Getter
    @Setter
    @NonNull
    protected String rootContext;
    ClassLoader cl = getClass().getClassLoader();

    public void handle(HttpExchange t) throws IOException {
        URI uri = t.getRequestURI();

        log.debug("requesting: " + uri.getPath());
        String filename = uri.getPath();
        if (filename == null || !filename.startsWith(rootContext) ||
                !Arrays.stream(VALID_SUFFIX).anyMatch(valid -> filename.endsWith(valid))) {
            respond404(t);
            return;
        }
        // resource loading without leading slash
        String resourceName = filename.replace("..", "");
        if (filename.charAt(0) == '/') {
            resourceName = filename.substring(1);
        }

        // we are using getResourceAsStream to ultimately prevent load from parent directories
        try (InputStream resource = cl.getResourceAsStream(resourceName)) {
            if (resource == null) {
                respond404(t);
                return;
            }
            log.debug("sending: " + resourceName);
            // Object exists and is a file: accept with response code 200.
            String mime = "text/html";
            if (resourceName.endsWith(".js")) mime = "application/javascript";
            if (resourceName.endsWith(".json")) mime = "application/json";
            if (resourceName.endsWith(".css")) mime = "text/css";
            if (resourceName.endsWith(".png")) mime = "image/png";

            Headers h = t.getResponseHeaders();
            h.set("Content-Type", mime);
            t.sendResponseHeaders(200, 0);

            try (OutputStream os = t.getResponseBody()) {
                final byte[] buffer = new byte[ 0x10000 ];
                int count = 0;
                while ((count = resource.read(buffer)) >= 0) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }

    private void respond404(HttpExchange t) throws IOException {
        // Object does not exist or is not a file: reject with 404 error.
        String response = "404 (Not Found)\n";
        t.sendResponseHeaders(404, response.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
