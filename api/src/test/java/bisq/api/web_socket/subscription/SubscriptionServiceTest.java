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

package bisq.api.web_socket.subscription;

import bisq.api.access.filter.Headers;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.PermissionSet;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.domain.market_price.MarketPriceWebSocketService;
import bisq.api.web_socket.domain.network.NetworkInfoWebSocketService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.json.JsonMapperProvider;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    @Test
    void subscribeRemovesSubscriberWhenSendingInitialPayloadFails() throws Exception {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        when(alertNotificationsService.getUnconsumedAlerts()).thenReturn(new ObservableSet<>());
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT)).thenReturn(Stream.empty());

        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                alertNotificationsService,
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                // Authorization off: these cover routing and subscriber bookkeeping, and the
                // permission path has its own test.
                false
        );

        WebSocket webSocket = mock(WebSocket.class);
        doThrow(new RuntimeException("send failed"))
                .doReturn(null)
                .when(webSocket)
                .send(anyString());

        String requestJson = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"request-1\",\"topic\":\"ALERT_NOTIFICATIONS\",\"parameter\":\"MOBILE_CLIENT\"}";

        service.onMessage(requestJson, webSocket);

        assertThat(getSubscriberRepository(service).findSubscribers(Topic.ALERT_NOTIFICATIONS)).isEmpty();
    }

    @Test
    void subscribeDeliversLiveEventEmittedDuringInitialSnapshot() throws Exception {
        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                // Authorization off: these cover routing and subscriber bookkeeping, and the
                // permission path has its own test.
                false
        );
        SubscriberRepository subscriberRepository = getSubscriberRepository(service);
        replaceWebSocketService(service, "marketPriceWebSocketService", new TestMarketPriceWebSocketService(subscriberRepository));

        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        }).when(webSocket).send(anyString());

        String requestJson = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"request-1\",\"topic\":\"MARKET_PRICE\"}";

        service.onMessage(requestJson, webSocket);

        verify(webSocket, timeout(1000).times(2)).send(anyString());
        assertThat(sentMessages)
                .anySatisfy(message -> assertThat(message)
                        .contains("\"type\":\"SubscriptionResponse\"")
                        .contains("snapshot"));
        assertThat(sentMessages)
                .anySatisfy(message -> assertThat(message)
                        .contains("\"type\":\"WebSocketEvent\"")
                        .contains("live-event"));
    }

    @Test
    void subscribeOffersWithVariantCaseCurrencyCodeLandsInSameBucket() throws Exception {
        BisqEasyOfferbookChannelService offerbookService = mock(BisqEasyOfferbookChannelService.class);
        when(offerbookService.getChannels()).thenReturn(new ObservableSet<>());
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getBisqEasyOfferbookChannelService()).thenReturn(offerbookService);

        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                chatService,
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                // Authorization off: these cover routing and subscriber bookkeeping, and the
                // permission path has its own test.
                false
        );

        service.onMessage(subscriptionJson("r1", "OFFERS", "eur"), mock(WebSocket.class, RETURNS_DEEP_STUBS));
        service.onMessage(subscriptionJson("r2", "OFFERS", "EUR"), mock(WebSocket.class, RETURNS_DEEP_STUBS));

        Map<SubscriptionSpecifier, Set<Subscriber>> groups =
                getSubscriberRepository(service).findSubscribers(Topic.OFFERS);
        assertThat(groups).hasSize(1);
        assertThat(groups.keySet().iterator().next().parameter()).isEqualTo(Optional.of("EUR"));
        assertThat(groups.values().iterator().next()).hasSize(2);
    }

    /**
     * Anti-drift guard for the {@code network-info} API capability: a dropped constructor assignment or a
     * missing routing branch would leave the topic unreachable, which a field-declaration check cannot see.
     */
    @Test
    void networkInfoSubscriptionResolvesToTheInitializedServiceAndIsAnswered() throws Exception {
        SubscriptionService service = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                // Authorization off: these cover routing and subscriber bookkeeping, and the
                // permission path has its own test.
                false
        );

        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        }).when(webSocket).send(anyString());

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        assertThat(sentMessages).hasSize(1);
        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.get("type").asText()).isEqualTo("SubscriptionResponse");
        assertThat(response.hasNonNull("errorMessage"))
                .as("a valid NETWORK_INFO subscription must not produce an error response")
                .isFalse();
        assertThat(response.hasNonNull("payload")).isTrue();
        assertThat(response.get("payload").asText())
                .as("payload must be a NetworkInfoDto")
                .contains("allDataReceived")
                .contains("connections");
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isNotEmpty();
    }

    /**
     * The gap this class of test exists for: the same connection carries REST calls and
     * subscriptions, but only the REST ones are proxied through JAX-RS, so
     * {@code RestApiAuthorizationFilter} never saw a subscription and every topic was served to any
     * paired client whatever it had been granted.
     */
    @Test
    void subscriptionIsRefusedWhenTheTopicsPermissionIsNotGranted() throws Exception {
        SubscriptionService service = authorizingService(EnumSet.of(Permission.TRADES));
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages);

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.get("errorMessage").asText())
                .as("the client must be able to tell a withheld permission from a transport failure")
                .isEqualTo("permission_not_granted: NETWORK_INFO");
        assertThat(response.hasNonNull("payload"))
                .as("a refused subscription must not carry the snapshot it was refused")
                .isFalse();
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO))
                .as("a refused subscription must not leave a subscriber behind to be pushed to")
                .isEmpty();
    }

    @Test
    void subscriptionProceedsWhenTheTopicsPermissionIsGranted() throws Exception {
        SubscriptionService service = authorizingService(EnumSet.of(Permission.NETWORK_INFO));
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages);

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.hasNonNull("errorMessage")).isFalse();
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isNotEmpty();
    }

    /**
     * The authorization check and the repository add are two steps. A revocation between them
     * removes the grant and closes the socket, but closing only moves a Grizzly socket to CLOSING:
     * until the close frame is flushed it still accepts sends and the repository has not been
     * swept. Without the re-read after the add, the snapshot would be served to a client whose
     * grant is already gone and the subscriber would stay. The revocation runs here from the topic
     * service's {@code validate}, which {@code subscribe} calls after the check and before the add,
     * and the socket keeps sending, which is the CLOSING window.
     */
    @Test
    void aSubscriptionWhoseClientIsRevokedWhileItIsBeingAddedIsRefusedAndLeavesNoSubscriber() throws Exception {
        AtomicReference<Map<String, PermissionSet>> store = new AtomicReference<>(
                Map.of("client-1", new PermissionSet(EnumSet.of(Permission.NETWORK_INFO))));
        ApiAccessStoreService apiAccessStoreService = mock(ApiAccessStoreService.class);
        when(apiAccessStoreService.getPermissionsByClientId()).thenAnswer(invocation -> store.get());
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages);
        SubscriptionService service = subscriptionService(new PermissionService(apiAccessStoreService), true);
        replaceWebSocketService(service, "networkInfoWebSocketService",
                new NetworkInfoWebSocketService(getSubscriberRepository(service),
                        mock(NetworkService.class, RETURNS_DEEP_STUBS),
                        mock(AuthorizedBondedRolesService.class, RETURNS_DEEP_STUBS)) {
                    @Override
                    public void validate(SubscriptionRequest request) {
                        store.set(Map.of());
                    }
                });

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO))
                .as("a subscriber whose grant went while it was being added must not stay behind")
                .isEmpty();
        assertThat(sentMessages).hasSize(1);
        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.get("errorMessage").asText())
                .as("the still-sending socket gets the refusal, never the snapshot")
                .isEqualTo("permission_not_granted");
        assertThat(response.hasNonNull("payload")).isFalse();
    }

    /**
     * The other place a refusal meets a closed socket: the subscribe frame was already queued when
     * the revocation ran, so the first check denies and the answer goes to a connection that no
     * longer exists. The send throws, and the executor running {@code onMessage} discards its
     * future, so anything escaping here would be neither handled nor logged.
     */
    @Test
    void aSubscriptionDeniedOnAConnectionAlreadyClosedByTheRevocationEscapesNothing() throws Exception {
        SubscriptionService service = authorizingService(EnumSet.of(Permission.TRADES));
        WebSocket webSocket = authenticatedWebSocket(new ArrayList<>());
        doThrow(new RuntimeException("Socket is not connected.")).when(webSocket).send(anyString());

        assertThatCode(() -> service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket))
                .doesNotThrowAnyException();
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isEmpty();
    }

    /**
     * The other half of the legacy-pairing path. Every client paired before this node existed holds
     * the full standard set of its release, which the store promotes to grantAll on load (see
     * {@code ApiAccessStoreTest}). That promotion is worth nothing unless the expansion then covers
     * this permission, which is what {@code Kind.STANDARD} on {@code NETWORK_INFO} buys: declare it
     * sensitive and grantAll stops covering it, leaving every existing pairing refused with
     * re-pairing as the only way back.
     */
    @Test
    void subscriptionProceedsForAClientWhoseLegacyGrantWasPromotedToGrantAll() throws Exception {
        ApiAccessStoreService apiAccessStoreService = mock(ApiAccessStoreService.class);
        when(apiAccessStoreService.getPermissionsByClientId())
                .thenReturn(Map.of("client-1", PermissionSet.grantAll()));
        SubscriptionService service = subscriptionService(new PermissionService(apiAccessStoreService), true);
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages);

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.hasNonNull("errorMessage")).isFalse();
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isNotEmpty();
    }

    /**
     * The revoked-client path: the pairing is gone from the store while the connection is still
     * open. Nothing else covers it — every other test here answers for a client the store knows, so
     * the lookup would keep passing if it stopped failing closed on an unknown one.
     */
    @Test
    void subscriptionIsRefusedWhenTheClientIsNotPaired() throws Exception {
        SubscriptionService service = authorizingService(EnumSet.allOf(Permission.class));
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages, "client-2");

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.get("errorMessage").asText())
                .as("a client the store does not know gets a bare denial, not the missing permission")
                .isEqualTo("permission_not_granted");
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isEmpty();
    }

    /**
     * Fails closed on the step before the permission: the identity is read from the upgrade request,
     * so a connection that carries none is refused rather than treated as an anonymous client with
     * nothing to check.
     * <p>
     * Both ways of carrying none are covered. A socket that exposes no upgrade request at all never
     * reaches the header, and a socket that exposes one without a usable {@code Bisq-Client-Id} is
     * what the emptiness filter in {@code WebSocketIdentity} is for — an absent header and a present
     * blank one have to land in the same place.
     */
    @Test
    void subscriptionIsRefusedWhenTheConnectionCarriesNoClientId() throws Exception {
        List<WebSocketFactory> connectionsWithoutAnIdentity = List.of(
                new WebSocketFactory("no upgrade request", SubscriptionServiceTest::socketWithoutUpgradeRequest),
                new WebSocketFactory("absent header", sent -> authenticatedWebSocket(sent, null)),
                new WebSocketFactory("blank header", sent -> authenticatedWebSocket(sent, "")));

        for (WebSocketFactory factory : connectionsWithoutAnIdentity) {
            SubscriptionService service = authorizingService(EnumSet.allOf(Permission.class));
            List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());

            service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), factory.create(sentMessages));

            JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
            assertThat(response.get("errorMessage").asText()).as(factory.name()).isEqualTo("permission_not_granted");
            assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO))
                    .as(factory.name())
                    .isEmpty();
        }
    }

    /**
     * Not a duplicate of the NETWORK_INFO refusal: that one is the topic with no REST route to
     * mirror, so it could pass while every mirrored row was misrouted. This pins that the mapping is
     * consulted for an ordinary topic too, and that the refusal names the permission the REST rule
     * for the same data asks for.
     */
    @Test
    void subscriptionIsRefusedWithThePermissionTheTopicMapsTo() throws Exception {
        SubscriptionService service = authorizingService(EnumSet.of(Permission.TRADES));
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = authenticatedWebSocket(sentMessages);

        service.onMessage(subscriptionJson("request-1", "OFFERS", "eur"), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.get("errorMessage").asText()).isEqualTo("permission_not_granted: OFFERBOOK");
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.OFFERS)).isEmpty();
    }

    /**
     * A node configured without authorization registers no REST filter either, so there are no
     * grants to check against and demanding them would break every such deployment.
     */
    @Test
    void subscriptionProceedsWhenTheNodeRunsWithoutAuthorization() throws Exception {
        SubscriptionService service = subscriptionService(mock(PermissionService.class), false);
        List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = socketWithoutUpgradeRequest(sentMessages);

        service.onMessage(subscriptionJson("request-1", "NETWORK_INFO", null), webSocket);

        JsonNode response = JsonMapperProvider.get().readTree(sentMessages.getFirst());
        assertThat(response.hasNonNull("errorMessage")).isFalse();
        assertThat(getSubscriberRepository(service).findSubscribers(Topic.NETWORK_INFO)).isNotEmpty();
    }

    /**
     * {@code findWebSocketService} switches over {@link Topic} in arrow form, which the compiler does
     * not require to be exhaustive. A topic added without its case therefore compiles, and only shows
     * up as a client whose subscribe never gets answered. This is the check that catches it.
     */
    @Test
    void everyTopicResolvesToAWebSocketService() throws Exception {
        SubscriptionService service = subscriptionService(mock(PermissionService.class), false);

        Method findWebSocketService = SubscriptionService.class
                .getDeclaredMethod("findWebSocketService", Topic.class);
        findWebSocketService.setAccessible(true);

        for (Topic topic : Topic.values()) {
            @SuppressWarnings("unchecked")
            Optional<BaseWebSocketService> webSocketService =
                    (Optional<BaseWebSocketService>) findWebSocketService.invoke(service, topic);
            assertThat(webSocketService)
                    .as("Topic %s has no WebSocketService wired in SubscriptionService", topic)
                    .isPresent();
        }
    }

    /** A real PermissionService over a stubbed store, so the grant is read the way production reads it. */
    private static SubscriptionService authorizingService(Set<Permission> granted) {
        ApiAccessStoreService apiAccessStoreService = mock(ApiAccessStoreService.class);
        when(apiAccessStoreService.getPermissionsByClientId())
                .thenReturn(Map.of("client-1", new PermissionSet(granted)));
        return subscriptionService(new PermissionService(apiAccessStoreService), true);
    }

    private static SubscriptionService subscriptionService(PermissionService permissionService,
                                                           boolean authorizationRequired) {
        return new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                permissionService,
                authorizationRequired);
    }

    /** The identity lives on the upgrade request, which only a DefaultWebSocket exposes. */
    private static WebSocket authenticatedWebSocket(List<String> sentMessages) {
        return authenticatedWebSocket(sentMessages, "client-1");
    }

    private static WebSocket authenticatedWebSocket(List<String> sentMessages, @Nullable String clientId) {
        HttpServletRequest upgradeRequest = mock(HttpServletRequest.class);
        when(upgradeRequest.getHeader(Headers.CLIENT_ID)).thenReturn(clientId);
        DefaultWebSocket webSocket = mock(DefaultWebSocket.class);
        when(webSocket.getUpgradeRequest()).thenReturn(upgradeRequest);
        recordSends(webSocket, sentMessages);
        return webSocket;
    }

    /** Not a DefaultWebSocket, so there is no upgrade request to read an identity from at all. */
    private static WebSocket socketWithoutUpgradeRequest(List<String> sentMessages) {
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        recordSends(webSocket, sentMessages);
        return webSocket;
    }

    private static void recordSends(WebSocket webSocket, List<String> sentMessages) {
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        }).when(webSocket).send(anyString());
    }

    /** A named way of building a connection, so a failing row in a loop says which one it was. */
    private record WebSocketFactory(String name, Function<List<String>, WebSocket> factory) {
        private WebSocket create(List<String> sentMessages) {
            return factory.apply(sentMessages);
        }
    }

    private static String subscriptionJson(String requestId, String topic, String parameter) {
        String paramPart = parameter != null ? ",\"parameter\":\"" + parameter + "\"" : "";
        return "{\"type\":\"SubscriptionRequest\",\"requestId\":\"" + requestId
                + "\",\"topic\":\"" + topic + "\"" + paramPart + "}";
    }

    private SubscriberRepository getSubscriberRepository(SubscriptionService service) throws NoSuchFieldException, IllegalAccessException {
        Field subscriberRepositoryField = SubscriptionService.class.getDeclaredField("subscriberRepository");
        subscriberRepositoryField.setAccessible(true);
        return (SubscriberRepository) subscriberRepositoryField.get(service);
    }

    private void replaceWebSocketService(SubscriptionService service,
                                         String fieldName,
                                         Object replacement) throws Exception {
        Field field = SubscriptionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, replacement);
    }

    private static final class TestMarketPriceWebSocketService extends MarketPriceWebSocketService {
        private TestMarketPriceWebSocketService(SubscriberRepository subscriberRepository) {
            super(subscriberRepository, mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
        }

        @Override
        public Optional<String> getJsonPayload() {
            return Optional.of("\"snapshot\"");
        }

        @Override
        public Optional<String> getJsonPayload(Optional<String> parameter) {
            send(Optional.of("\"live-event\""), topic, ModificationType.REPLACE);
            return getJsonPayload();
        }
    }
}