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

package bisq.api.web_socket.rest_api_proxy;

import bisq.api.ApiConfig;
import bisq.api.access.filter.Headers;
import bisq.api.access.filter.authz.AuthorizationException;
import bisq.api.access.filter.authz.UriValidator;
import bisq.api.access.transport.TlsContextService;
import bisq.api.web_socket.util.JsonUtil;
import bisq.common.application.Service;
import bisq.common.util.StringUtils;
import bisq.security.tls.TlsException;
import bisq.security.tls.TlsTrustManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.http.HttpClient.Version.HTTP_1_1;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiService implements Service {
    private static final UriValidator URI_VALIDATOR = new UriValidator();

    private final ApiConfig apiConfig;
    private final String restServerUrl;
    private final TlsContextService tlsContextService;
    private Optional<HttpClient> httpClient = Optional.empty();

    private final Object httpClientLock = new Object();

    public WebSocketRestApiService(ApiConfig apiConfig, TlsContextService tlsContextService) {
        this.apiConfig = apiConfig;
        this.restServerUrl = apiConfig.getRestProtocol() + "://" + toUriHost(apiConfig.getBindHost())
                + ":" + apiConfig.getBindPort();
        this.tlsContextService = tlsContextService;
    }

    /**
     * An IPv6 literal is only a valid URI host in brackets. Without them the authority does not parse
     * and every forwarded request would fail on the host comparison in {@link #resolveRestApiUri}.
     */
    static String toUriHost(String bindHost) {
        return bindHost.contains(":") && !bindHost.startsWith("[") ? "[" + bindHost + "]" : bindHost;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        synchronized (httpClientLock) {
            httpClient.ifPresent(HttpClient::close);
        }
        return CompletableFuture.completedFuture(true);
    }

    public boolean canHandle(String json) {
        return JsonUtil.hasExpectedJsonClassName(WebSocketRestApiRequest.class, json);
    }

    public void onMessage(String json, WebSocket webSocket) {
        Optional<WebSocketRestApiRequest> webSocketRestApiRequest = WebSocketRestApiRequest.fromJson(json);
        webSocketRestApiRequest.ifPresent(WebSocketRestApiRequest::clearHeaders);
        webSocketRestApiRequest
                .map(request -> sendToRestApiServer(request, webSocket))
                .ifPresent(future -> {
                    future.whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            response.toJson()
                                    .ifPresentOrElse(webSocket::send,
                                            () -> log.warn("Message was not sent to websocket." +
                                                    "\nJson={}", JsonUtil.redactCredentials(json)));
                        } else {
                            log.warn("REST API call failed for request: {}", JsonUtil.redactCredentials(json), throwable);
                            String requestId = webSocketRestApiRequest.get().getRequestId();
                            new WebSocketRestApiResponse(requestId, 500, throwable.getMessage()).toJson()
                                    .ifPresent(webSocket::send);
                        }
                    });
                });
    }

    private CompletableFuture<WebSocketRestApiResponse> sendToRestApiServer(WebSocketRestApiRequest request,
                                                                           WebSocket webSocket) {
        CompletableFuture<WebSocketRestApiResponse> future = new CompletableFuture<>();
        String method = request.getMethod();
        String body = request.getBody();
        URI uri;
        try {
            uri = resolveRestApiUri(restServerUrl, request.getPath());
        } catch (AuthorizationException | IllegalArgumentException e) {
            // The client sent a path we refuse to forward, which is a bad request and not a server error.
            // The reason is not echoed back, so that the response cannot be used to probe the check.
            log.warn("Rejected the path of a forwarded request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    new WebSocketRestApiResponse(request.getRequestId(), 400, "Invalid path"));
        }
        String url = uri.toString();
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body));

            // The identity is taken from the authenticated upgrade request, never from the message
            // payload: a client must not be able to act under an identity it did not authenticate with,
            // and keeping credentials out of the frames keeps them out of the deflate window.
            forwardAuthenticatedIdentity(webSocket, requestBuilder);

            HttpRequest httpRequest = requestBuilder.build();
            log.info("Forwarding {} request to {}", method, url);
            HttpClient httpClient = getOrCreateHttpClient();
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response ->
                            new WebSocketRestApiResponse(request.getRequestId(), response.statusCode(), response.body()))
                    .whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            log.info("httpResponse {}", response);
                            future.complete(response);
                        } else {
                            log.warn("Request failed", throwable);
                            future.completeExceptionally(throwable);
                        }
                    });
            return future;
        } catch (Exception e) {
            String errorMessage = String.format("Error at sending a '%s' request to '%s'. Error: %s", method, url, e.getMessage());
            log.error(errorMessage, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * The path comes from the message payload and therefore from the client, so it decides on its own
     * what the forwarded request addresses. Appending it to the server URL unchecked lets a client
     * redirect the call to a host of its choosing, because a path such as {@code @example.com/} turns
     * the configured host into mere user info of the resulting URI, and the forwarded request carries
     * the session credentials.
     *
     * <p>The checks run on the normalized URI, so that a traversal cannot pass them and still change
     * where the request ends up.
     */
    static URI resolveRestApiUri(String restServerUrl, String path) throws AuthorizationException {
        checkArgument(StringUtils.isNotEmpty(path), "Path must not be empty");
        checkArgument(path.startsWith("/") && !path.startsWith("//"), "Path must be server absolute: %s", path);

        URI uri = URI.create(restServerUrl + path).normalize();
        URI restServerUri = URI.create(restServerUrl);
        checkArgument(restServerUri.getScheme().equals(uri.getScheme())
                        && restServerUri.getHost().equals(uri.getHost())
                        && restServerUri.getPort() == uri.getPort()
                        && uri.getRawUserInfo() == null,
                "Path must not change the target of the request: %s", path);
        // Checked after normalizing, so that a traversal cannot walk out of the REST API and reach
        // another handler of the same server — the docs endpoint or a static file handler — with the
        // credentials of the connection attached.
        checkArgument(uri.getPath().equals(ApiConfig.REST_API_BASE_PATH)
                        || uri.getPath().startsWith(ApiConfig.REST_API_BASE_PATH + "/"),
                "Path must address the REST API: %s", path);

        // The REST API applies this to incoming requests as well, but only when authorization is
        // required, whereas a forwarded request has to be safe regardless of that setting.
        URI_VALIDATOR.validate(uri);
        return uri;
    }

    /**
     * Copies the identity of the connection onto the forwarded request. A connection whose upgrade
     * request cannot be resolved at all is a state we do not expect and refuse to forward for, whereas
     * an upgrade request carrying no identity headers forwards none: with session handling enabled the
     * handshake filter has already rejected such a connection, and with it disabled there is no
     * identity to pass on and the REST API asks for none.
     */
    private static void forwardAuthenticatedIdentity(WebSocket webSocket, HttpRequest.Builder requestBuilder) {
        HttpServletRequest upgradeRequest = findUpgradeRequest(webSocket)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not resolve the upgrade request of the WebSocket connection"));
        copyHeader(upgradeRequest, requestBuilder, Headers.SESSION_ID);
        copyHeader(upgradeRequest, requestBuilder, Headers.CLIENT_ID);
    }

    /**
     * The upgrade request is where the connection's authenticated identity lives, so it is read from
     * the WebSocket rather than from the message.
     */
    private static Optional<HttpServletRequest> findUpgradeRequest(WebSocket webSocket) {
        return webSocket instanceof DefaultWebSocket defaultWebSocket
                ? Optional.ofNullable(defaultWebSocket.getUpgradeRequest())
                : Optional.empty();
    }

    private static void copyHeader(HttpServletRequest upgradeRequest,
                                   HttpRequest.Builder requestBuilder,
                                   String name) {
        Optional.ofNullable(upgradeRequest.getHeader(name))
                .filter(StringUtils::isNotEmpty)
                .ifPresent(value -> requestBuilder.header(name, value));
    }

    private HttpClient getOrCreateHttpClient() {
        synchronized (httpClientLock) {
            if (httpClient.isEmpty()) {
                // Use HTTP/1.1 explicitly to avoid HTTP/2 behavior where headers and body
                // are sent as separate frames, which can appear as duplicate requests on the server, and cause requests to fail
                // This is important to make sure WebSocket forwarded methods with body such as POST do not fail
                HttpClient.Builder builder = HttpClient.newBuilder()
                        .version(HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(10));
                if (apiConfig.isTlsRequired()) {
                    try {
                        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                        String fingerprint = tlsContextService.getOrCreateTlsContext().orElseThrow().getTlsFingerprint();
                        sslContext.init(
                                null,
                                new TrustManager[]{new TlsTrustManager(fingerprint)},
                                new SecureRandom()
                        );
                        builder.sslContext(sslContext);
                    } catch (NoSuchAlgorithmException | TlsException | KeyManagementException e) {
                        log.error("Could not apply SSL context", e);
                        throw new RuntimeException(e);
                    }
                }
                HttpClient client = builder.build();
                httpClient = Optional.of(client);
            }

            return httpClient.get();
        }
    }
}