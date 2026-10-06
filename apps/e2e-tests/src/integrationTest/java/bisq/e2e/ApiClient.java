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

package bisq.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * A client of the REST and WebSocket API of an api-app node. It uses only the routes which v2.1.13 and this version
 * both have, with the same request and response formats (see the API comparison in tools/compat/README.md). The
 * nodes of a test network run without authentication.
 */
final class ApiClient {
    static final ObjectMapper JSON = new ObjectMapper();

    // Both versions limit the publications of an author to 3 per second and 30 per minute (BannedUserService). The
    // client keeps a distance between its publications, so that neither its own node nor a peer drops them.
    private static final long MIN_MILLIS_BETWEEN_PUBLICATIONS = 2_100;

    private final String baseUrl;
    private final URI webSocketUri;
    private long lastPublication;
    // HTTP/1.1, because the client of the JDK otherwise asks for an upgrade to HTTP/2, which makes the server of v2.1.13
    // fail to read the body of a POST request (EOFException)
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    ApiClient(int port) {
        baseUrl = "http://127.0.0.1:" + port + "/api/v1";
        webSocketUri = URI.create("ws://127.0.0.1:" + port + "/websocket");
    }

    static final class ApiException extends RuntimeException {
        final int status;

        ApiException(String request, int status, String body) {
            super(request + " returned HTTP " + status + ": " + body);
            this.status = status;
        }
    }

    boolean isReady() {
        try {
            get("/settings/version");
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    String version() {
        JsonNode version = get("/settings/version");
        return version.isTextual() ? version.asText() : version.path("version").asText();
    }

    /** Creates a user identity with the key material which the node prepares, as the mobile app does. */
    void createUserIdentity(String nickName) {
        JsonNode keyMaterial = get("/user-identities/key-material");
        ObjectNode request = JSON.createObjectNode();
        request.put("nickName", nickName);
        request.put("terms", "");
        request.put("statement", "");
        request.set("keyMaterialResponse", keyMaterial);
        post("/user-identities", request);
    }

    void acceptTermsAndConditions() {
        patch("/settings", JSON.createObjectNode().put("isTacAccepted", true));
    }

    // The market in the format of a create offer request (MarketDto). The list of markets has more fields, which the
    // request does not accept.
    JsonNode market(String quoteCurrencyCode) {
        for (JsonNode market : get("/offerbook/markets")) {
            if (market.path("baseCurrencyCode").asText().equals("BTC") &&
                    market.path("quoteCurrencyCode").asText().equals(quoteCurrencyCode)) {
                ObjectNode marketDto = JSON.createObjectNode();
                for (String field : List.of("baseCurrencyCode", "quoteCurrencyCode", "baseCurrencyName", "quoteCurrencyName")) {
                    marketDto.set(field, market.path(field));
                }
                return marketDto;
            }
        }
        throw new IllegalStateException("No market BTC/" + quoteCurrencyCode);
    }

    String createOffer(ObjectNode createOfferRequest) {
        awaitPublicationSlot();
        return post("/offerbook/offers", createOfferRequest).path("offerId").asText();
    }

    void removeOffer(String offerId) {
        awaitPublicationSlot();
        delete("/offerbook/offers/" + offerId);
    }

    private synchronized void awaitPublicationSlot() {
        long waitMillis = lastPublication + MIN_MILLIS_BETWEEN_PUBLICATIONS - System.currentTimeMillis();
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        lastPublication = System.currentTimeMillis();
    }

    List<JsonNode> offers(String quoteCurrencyCode) {
        List<JsonNode> offers = new ArrayList<>();
        get("/offerbook/markets/" + quoteCurrencyCode + "/offers").forEach(offers::add);
        return offers;
    }

    String takeOffer(String offerId, long baseSideAmount, long quoteSideAmount, String bitcoinPaymentMethod,
                     String fiatPaymentMethod) {
        ObjectNode request = JSON.createObjectNode();
        request.put("offerId", offerId);
        request.put("baseSideAmount", baseSideAmount);
        request.put("quoteSideAmount", quoteSideAmount);
        request.put("bitcoinPaymentMethod", bitcoinPaymentMethod);
        request.put("fiatPaymentMethod", fiatPaymentMethod);
        return post("/trades", request).path("tradeId").asText();
    }

    void tradeEvent(String tradeId, String tradeEventType, String data) {
        ObjectNode request = JSON.createObjectNode();
        request.put("tradeEventType", tradeEventType);
        if (data == null) {
            request.putNull("data");
        } else {
            request.put("data", data);
        }
        patch("/trades/" + tradeId + "/event", request);
    }

    /**
     * The open trades of the node, with their contract. v2.1.13 provides them only over WebSocket. The trade state in
     * these items is the state at the time when the item was created. {@link #tradeProperties()} has the current state.
     */
    List<JsonNode> openTrades() {
        List<JsonNode> trades = new ArrayList<>();
        subscriptionSnapshot("TRADES").forEach(trades::add);
        return trades;
    }

    /** The current properties of each open trade, such as its state and error messages, by trade id. */
    Map<String, JsonNode> tradeProperties() {
        Map<String, JsonNode> propertiesByTradeId = new HashMap<>();
        // A list of maps with one entry each
        for (JsonNode map : subscriptionSnapshot("TRADE_PROPERTIES")) {
            map.properties().forEach(entry -> propertiesByTradeId.put(entry.getKey(), entry.getValue()));
        }
        return propertiesByTradeId;
    }

    /**
     * Subscribes to a topic on a new connection, reads the first answer, which contains the whole current data of the
     * topic, and closes the connection. Closing removes the subscription again.
     */
    private JsonNode subscriptionSnapshot(String topic) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> response = new CompletableFuture<>();
        WebSocket webSocket = http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(webSocketUri, new SubscriptionResponseListener(requestId, response))
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
        try {
            ObjectNode request = JSON.createObjectNode();
            request.put("type", "SubscriptionRequest");
            request.put("requestId", requestId);
            request.put("topic", topic);
            request.putNull("parameter");
            webSocket.sendText(request.toString(), true).join();
            JsonNode subscriptionResponse = JSON.readTree(response.orTimeout(10, TimeUnit.SECONDS).join());
            JsonNode errorMessage = subscriptionResponse.path("errorMessage");
            if (!errorMessage.isNull() && !errorMessage.isMissingNode()) {
                throw new IllegalStateException("Subscription to " + topic + " failed: " + errorMessage.asText());
            }
            return JSON.readTree(subscriptionResponse.path("payload").asText());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").orTimeout(5, TimeUnit.SECONDS).exceptionally(e -> null).join();
        }
    }

    JsonNode get(String path) {
        return send("GET " + path, HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    JsonNode post(String path, JsonNode body) {
        return send("POST " + path, HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
    }

    JsonNode patch(String path, JsonNode body) {
        return send("PATCH " + path, HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString())));
    }

    JsonNode delete(String path) {
        return send("DELETE " + path, HttpRequest.newBuilder(URI.create(baseUrl + path)).DELETE());
    }

    private JsonNode send(String description, HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(description, response.statusCode(), response.body());
            }
            String body = response.body();
            return body == null || body.isBlank() ? NullNode.getInstance() : JSON.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException(description + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(description + " was interrupted", e);
        }
    }

    // Completes the future with the SubscriptionResponse of the request. Text frames can arrive in parts.
    private static final class SubscriptionResponseListener implements WebSocket.Listener {
        private final String requestId;
        private final CompletableFuture<String> response;
        private final StringBuilder message = new StringBuilder();

        private SubscriptionResponseListener(String requestId, CompletableFuture<String> response) {
            this.requestId = requestId;
            this.response = response;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            message.append(data);
            if (last) {
                String text = message.toString();
                message.setLength(0);
                if (text.contains("\"SubscriptionResponse\"") && text.contains(requestId)) {
                    response.complete(text);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            response.completeExceptionally(error);
        }
    }
}
