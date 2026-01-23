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
import bisq.api.access.transport.TlsContextService;
import bisq.api.web_socket.util.JsonUtil;
import bisq.common.application.Service;
import bisq.security.tls.TlsException;
import bisq.security.tls.TlsTrustManager;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiService implements Service {
    private final ApiConfig apiConfig;
    private final String restServerUrl;
    private final TlsContextService tlsContextService;
    private Optional<HttpClient> httpClient = Optional.empty();

    public WebSocketRestApiService(ApiConfig apiConfig, TlsContextService tlsContextService) {
        this.apiConfig = apiConfig;
        this.restServerUrl = apiConfig.getRestServerUrl();
        this.tlsContextService = tlsContextService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        httpClient.ifPresent(HttpClient::close);
        return CompletableFuture.completedFuture(true);
    }

    public boolean canHandle(String json) {
        return JsonUtil.hasExpectedJsonClassName(WebSocketRestApiRequest.class, json);
    }

    public void onMessage(String json, WebSocket webSocket) {
        WebSocketRestApiRequest.fromJson(json)
                .map(this::sendToRestApiServer)
                .flatMap(WebSocketRestApiResponse::toJson)
                .ifPresentOrElse(webSocket::send,
                        () -> log.warn("Message was not sent to websocket." +
                                "\nJson={}", json));
    }

    private WebSocketRestApiResponse sendToRestApiServer(WebSocketRestApiRequest request) {
        String url = restServerUrl + request.getPath();
        String method = request.getMethod();
        String body = request.getBody();
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(method, body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body));

            // We get the SESSION_ID, NONCE, TIMESTAMP and SIGNATURE headers by the client sent inside the request.headers map.
            String[] headers = request.getHeaders().entrySet().stream()
                    .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                    .toArray(String[]::new);
            if (headers.length > 0) {
                requestBuilder.headers(headers);
            }

            HttpRequest httpRequest = requestBuilder.build();
            log.info("Forwarding {} request to {}", method, url);
            // Blocking send
            HttpClient httpClient = getOrCreateHttpClient();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("httpResponse {}", httpResponse);
            return new WebSocketRestApiResponse(request.getRequestId(), httpResponse.statusCode(), httpResponse.body());
        } catch (Exception e) {
            String errorMessage = String.format("Error at sending a '%s' request to '%s'. Error: %s", method, url, e.getMessage());
            log.error(errorMessage, e);
            return new WebSocketRestApiResponse(request.getRequestId(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
        }
    }

    private HttpClient getOrCreateHttpClient() {
        if (httpClient.isEmpty()) {
            HttpClient.Builder builder = HttpClient.newBuilder()
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