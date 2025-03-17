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
import bisq.http_api.web_socket.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiService implements Service {
    private final ObjectMapper objectMapper;
    private final String restApiAddress;
    private Optional<HttpClient> httpClient = Optional.empty();

    public WebSocketRestApiService(ObjectMapper objectMapper, String restApiAddress) {
        this.objectMapper = objectMapper;
        this.restApiAddress = restApiAddress;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        httpClient = Optional.of(HttpClient.newHttpClient());
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
        String errorMessage = RequestValidation.validateRequest(request);
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
                .header("Accept", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpRequest httpRequest = requestBuilder.build();
            log.info("Send {} httpRequest to {}. httpRequest={} ", method, url, httpRequest);
            // Blocking send
            HttpResponse<String> httpResponse = httpClient.orElseThrow().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("httpResponse {}", httpResponse);
            return new WebSocketRestApiResponse( request.getRequestId(), httpResponse.statusCode(), httpResponse.body());
        } catch (Exception e) {
            errorMessage = String.format("Error at sending a '%s' request to '%s' with body: '%s'. Error: %s", method, url, body, e.getMessage());
            log.error(errorMessage, e);
            return new WebSocketRestApiResponse(request.getRequestId(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
        }
    }
}