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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    // The credentials of a client which paired with the node, like the mobile app of Bisq Connect, and its current
    // session. A node with the default security of its version requires them. The nodes of a test network run
    // without authorization, and then the client sends no access headers.
    private volatile String clientId;
    private volatile String clientSecret;
    private volatile String sessionId;
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

    // Whether the API server answers. The route of sessions needs no authorization, and it rejects an empty request.
    boolean isServing() {
        try {
            post("/access/session", JSON.createObjectNode());
            return true;
        } catch (ApiException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Pairs this client with the node, with the pairing code which the node shows as QR code. */
    void pair(String pairingCodeId, String clientName) {
        JsonNode response = post("/access/pairing", JSON.createObjectNode()
                .put("version", 1)
                .put("pairingCodeId", pairingCodeId)
                .put("clientName", clientName));
        clientId = response.path("clientId").asText();
        clientSecret = response.path("clientSecret").asText();
        sessionId = response.path("sessionId").asText();
    }

    /** Requests a new session with the credentials of the pairing, as the app does when its session is not valid. */
    void renewSession() {
        sessionId = requestSession(clientId, clientSecret).path("sessionId").asText();
    }

    JsonNode requestSession(String clientId, String clientSecret) {
        return post("/access/session", JSON.createObjectNode()
                .put("clientId", clientId)
                .put("clientSecret", clientSecret));
    }

    String clientId() {
        return clientId;
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

    JsonNode settings() {
        return get("/settings");
    }

    // The API of v2.1.13 applies only one field of a settings change request, so each change is a request of its own
    void changeSetting(String field, JsonNode value) {
        ObjectNode request = JSON.createObjectNode();
        request.set(field, value);
        patch("/settings", request);
    }

    // The ID of the selected user profile: the ID of the public key of its network ID
    String userProfileId() {
        return get("/user-identities/selected/user-profile").path("networkId").path("pubKey").path("id").asText();
    }

    // The user profiles of the identities of the node
    List<JsonNode> ownedUserProfiles() {
        List<JsonNode> profiles = new ArrayList<>();
        get("/user-identities/owned-profiles").forEach(profiles::add);
        return profiles;
    }

    // Selects the identity of the user profile. New offers and chat messages use the selected identity.
    void selectUserProfile(String userProfileId) {
        post("/user-identities/select", JSON.createObjectNode().put("userProfileId", userProfileId));
    }

    // The node must know the user profile, else the API returns HTTP 404
    void ignoreUserProfile(String userProfileId) {
        post("/user-profiles/ignore/" + userProfileId, JSON.createObjectNode());
    }

    void undoIgnoreUserProfile(String userProfileId) {
        delete("/user-profiles/ignore/" + userProfileId);
    }

    List<String> ignoredUserProfileIds() {
        List<String> ids = new ArrayList<>();
        get("/user-profiles/ignored").forEach(id -> ids.add(id.asText()));
        return ids;
    }

    // The closed trades of the node, at most 100. Only this version has this route.
    List<JsonNode> closedTrades() {
        List<JsonNode> trades = new ArrayList<>();
        get("/trades/closed?page=1&pageSize=100").path("items").forEach(trades::add);
        return trades;
    }

    // The market in the format of a create offer request (MarketDto). The list of markets has more fields, which the
    // request does not accept.
    /**
     * Adds a user-defined payment account with the route of v2.1.13, the only kind of account which its API creates.
     * The API of this checkout takes another request for this route.
     */
    void addUserDefinedAccountWithOldApi(String accountName, String accountData) {
        post("/payment-accounts", JSON.createObjectNode()
                .put("accountName", accountName)
                .put("accountData", accountData));
    }

    /** Selects a user-defined payment account with the route of v2.1.13. */
    void selectUserDefinedAccountWithOldApi(String accountName, String accountData) {
        ObjectNode account = JSON.createObjectNode().put("accountName", accountName);
        account.putObject("accountPayload").put("accountData", accountData);
        ObjectNode request = JSON.createObjectNode();
        request.set("selectedAccount", account);
        patch("/payment-accounts/selected", request);
    }

    /**
     * The data of each user-defined payment account by its name. Both versions list them with this route. v2.1.13 lists
     * only user-defined accounts, without a payment rail. This checkout lists all accounts with their payment rail.
     */
    Map<String, String> userDefinedAccounts() {
        Map<String, String> dataByName = new LinkedHashMap<>();
        for (JsonNode account : get("/payment-accounts")) {
            JsonNode paymentRail = account.path("paymentRail");
            if (paymentRail.isMissingNode() || paymentRail.asText().equals("CUSTOM")) {
                dataByName.put(account.path("accountName").asText(),
                        account.path("accountPayload").path("accountData").asText());
            }
        }
        return dataByName;
    }

    /** The name of the selected user-defined payment account, which both versions show with this route. */
    Optional<String> selectedUserDefinedAccountName() {
        JsonNode account = get("/payment-accounts/fiat/selected");
        return account.isNull() ? Optional.empty() : Optional.of(account.path("accountName").asText());
    }

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

    /** Sends a text message in the chat of a trade, optionally as a reply to a cited message. */
    void sendTradeChatMessage(String channelId, String text, ObjectNode citation) {
        ObjectNode request = JSON.createObjectNode();
        request.put("text", text);
        if (citation == null) {
            request.putNull("citation");
        } else {
            request.set("citation", citation);
        }
        post("/trade-chat-channels/" + channelId + "/messages", request);
    }

    /**
     * Adds a reaction to a message of a trade chat. To remove an own reaction again, isRemoved is true, and the API needs
     * the profile id of the sender of the reaction.
     */
    void sendTradeChatReaction(String channelId, String messageId, int reactionId, boolean isRemoved,
                               String senderUserProfileId) {
        ObjectNode request = JSON.createObjectNode();
        request.put("reactionId", reactionId);
        request.put("isRemoved", isRemoved);
        if (senderUserProfileId == null) {
            request.putNull("senderUserProfileId");
        } else {
            request.put("senderUserProfileId", senderUserProfileId);
        }
        post("/trade-chat-channels/" + channelId + "/" + messageId + "/reactions", request);
    }

    /** The messages of all trade chats of the node, each with its reactions. */
    List<JsonNode> tradeChatMessages() {
        List<JsonNode> messages = new ArrayList<>();
        subscriptionSnapshot("TRADE_CHAT_MESSAGES").forEach(messages::add);
        return messages;
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
     * The alert which restricts trading of the desktop app on the node, or a JSON null without such an alert. The
     * api-app uses the app type of the desktop app. Only nodes of this checkout have this topic.
     */
    JsonNode tradeRestrictingAlert() {
        return subscriptionSnapshot("TRADE_RESTRICTING_ALERT", Optional.of("DESKTOP"));
    }

    private JsonNode subscriptionSnapshot(String topic) {
        return subscriptionSnapshot(topic, Optional.empty());
    }

    /**
     * Subscribes to a topic on a new connection, reads the first answer, which contains the whole current data of the
     * topic, and closes the connection. Closing removes the subscription again.
     */
    private JsonNode subscriptionSnapshot(String topic, Optional<String> parameter) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> response = new CompletableFuture<>();
        WebSocket.Builder webSocketBuilder = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5));
        accessHeaders().forEach(webSocketBuilder::header);
        WebSocket webSocket = webSocketBuilder
                .buildAsync(webSocketUri, new SubscriptionResponseListener(requestId, response))
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
        try {
            ObjectNode request = JSON.createObjectNode();
            request.put("type", "SubscriptionRequest");
            request.put("requestId", requestId);
            request.put("topic", topic);
            parameter.ifPresentOrElse(value -> request.put("parameter", value), () -> request.putNull("parameter"));
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

    // The headers with which a paired client authenticates its requests (Headers of the API)
    private Map<String, String> accessHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (clientId != null) {
            headers.put("Bisq-Client-Id", clientId);
        }
        if (sessionId != null) {
            headers.put("Bisq-Session-Id", sessionId);
        }
        return headers;
    }

    private JsonNode send(String description, HttpRequest.Builder builder) {
        try {
            accessHeaders().forEach(builder::header);
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
