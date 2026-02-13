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

package bisq.http_api.web_socket.rest_api_proxy;

import bisq.common.application.Service;
import bisq.http_api.auth.AuthUtils;
import bisq.http_api.validator.WebSocketRequestValidator;
import bisq.http_api.web_socket.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpClient.Version.HTTP_1_1;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiService implements Service {
    private final ObjectMapper objectMapper;
    private final String restApiAddress;
    private final WebSocketRequestValidator requestValidator;
    private Optional<HttpClient> httpClient = Optional.empty();

    public WebSocketRestApiService(ObjectMapper objectMapper,
                                   String restApiAddress,
                                   WebSocketRequestValidator requestValidator) {
        this.objectMapper = objectMapper;
        this.restApiAddress = restApiAddress;
        this.requestValidator = requestValidator;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // Use HTTP/1.1 explicitly to avoid HTTP/2 behavior where headers and body
        // are sent as separate frames, which can appear as duplicate requests on the server, and cause requests to fail
        // This is important to make sure WebSocket forwarded methods with body such as POST do not fail
        HttpClient.Builder builder = HttpClient.newBuilder().version(HTTP_1_1);

        // When the REST API base address uses HTTPS (TLS enabled), the internal proxy
        // connects to the same server on localhost with a self-signed certificate.
        // We use a trust-all SSLContext here because this is a localhost-to-localhost call
        // within the same process — no network exposure, no MITM risk.
        if (restApiAddress.startsWith("https://")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }}, new SecureRandom());
                builder.sslContext(sslContext);
                log.info("Internal REST API proxy configured with trust-all SSL for localhost self-signed cert");
            } catch (Exception e) {
                log.error("Failed to configure SSL context for internal proxy", e);
            }
        }

        httpClient = Optional.of(builder.build());
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
        WebSocketRestApiRequest.fromJson(objectMapper, json)
                .map(this::sendToRestApiServer)
                .flatMap(response -> response.toJson(objectMapper))
                .ifPresentOrElse(webSocket::send,
                        () -> log.warn("Message was not sent to websocket." +
                                "\nJson={}", json));
    }

    private WebSocketRestApiResponse sendToRestApiServer(WebSocketRestApiRequest request) {
        String errorMessage = requestValidator.validateRequest(request);
        if (errorMessage != null) {
            log.error(errorMessage);
            return new WebSocketRestApiResponse(request.getRequestId(), Response.Status.BAD_REQUEST.getStatusCode(), errorMessage);
        }

        String url = restApiAddress + request.getPath();
        String method = request.getMethod();
        String body = request.getBody();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (body != null && !body.isEmpty()) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        // Support both old authentication (authToken, authTs, authNonce) and new authentication (headers)
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            // New authentication format - forward all headers
            request.getHeaders().forEach(requestBuilder::header);
            log.debug("Using new authentication format with headers: {}", request.getHeaders().keySet());
        } else if (request.getAuthToken() != null && request.getAuthTs() != null && request.getAuthNonce() != null) {
            // Legacy authentication format
            requestBuilder.header(AuthUtils.AUTH_HEADER, request.getAuthToken());
            requestBuilder.header(AuthUtils.AUTH_TIMESTAMP_HEADER, request.getAuthTs());
            requestBuilder.header(AuthUtils.AUTH_NONCE_HEADER, request.getAuthNonce());
            log.debug("Using legacy authentication format");
        }
        try {
            HttpRequest httpRequest = requestBuilder.build();
            log.info("Forwarding {} request to {}", method, url);
            // Blocking send
            HttpResponse<String> httpResponse = httpClient.orElseThrow().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("httpResponse {}", httpResponse);
            return new WebSocketRestApiResponse(request.getRequestId(), httpResponse.statusCode(), httpResponse.body());
        } catch (Exception e) {
            errorMessage = String.format("Error at sending a '%s' request to '%s' with body: '%s'. Error: %s", method, url, body, e.getMessage());
            log.error(errorMessage, e);
            return new WebSocketRestApiResponse(request.getRequestId(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
        }
    }
}