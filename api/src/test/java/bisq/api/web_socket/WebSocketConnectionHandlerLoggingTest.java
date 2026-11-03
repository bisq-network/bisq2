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

package bisq.api.web_socket;

import bisq.api.access.filter.Attributes;
import bisq.api.access.permissions.PermissionService;
import bisq.api.offerbook.RecordedOfferItems;
import bisq.api.util.LogCapture;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiService;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.common.json.JsonMapperProvider;
import bisq.network.NetworkService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Isolated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every message which a client sends passes the connection handler first. A message can contain the body of a REST
 * request, for example the private key of a new user identity, and clients released before the node took the
 * identity from the handshake put their session ID into it. Users share their log files, so the handler and the
 * services which it calls must not write a message to the log, at any log level.
 */
@Isolated("Captures the log of every logger")
class WebSocketConnectionHandlerLoggingTest {
    private static final String PRIVATE_VALUE = "privateSentinelValue";
    private static final String SESSION_ID = "sessionSentinelValue";

    @RegisterExtension
    final LogCapture logs = new LogCapture();
    private WebSocketRestApiService webSocketRestApiService;
    private WebSocketConnectionHandler handler;
    private DefaultWebSocket webSocket;

    @BeforeEach
    void setUp() {
        webSocketRestApiService = mock(WebSocketRestApiService.class);
        handler = new WebSocketConnectionHandler(subscriptionService(), webSocketRestApiService, clientId -> true);
        webSocket = connect();
    }

    @AfterEach
    void tearDown() {
        handler.shutdown();
    }

    @Test
    void receivedMessageIsLoggedWithoutItsContent() {
        String json = restRequest("WebSocketRestApiRequest");
        when(webSocketRestApiService.canHandle(json)).thenReturn(true);

        handler.onMessage(webSocket, json);

        verify(webSocketRestApiService, timeout(10_000)).onMessage(json, webSocket);
        logs.assertNoLineContains(PRIVATE_VALUE, SESSION_ID);
        assertThat(logs.lines()).anyMatch(line -> line.contains("Received a message of length " + json.length()));
    }

    // A client of a newer version can send a message type which this node does not know
    @Test
    void messageWithoutAServiceIsLoggedWithoutItsContent() throws Exception {
        String json = restRequest("AMessageTypeThisNodeDoesNotKnow");

        handler.onMessage(webSocket, json);

        logs.awaitLineContaining("No service found");
        logs.assertNoLineContains(PRIVATE_VALUE, SESSION_ID);
        assertThat(logs.lines()).anyMatch(line -> line.contains("No service found")
                && line.contains("length " + json.length()));
    }

    // A client of a newer version can send a property which this node does not know
    @Test
    void subscriptionRequestWhichCannotBeParsedIsLoggedWithoutItsContent() throws Exception {
        String json = JsonMapperProvider.get().createObjectNode()
                .put("type", "SubscriptionRequest")
                .put("requestId", "request-id")
                .put("topic", "MARKET_PRICE")
                .put("parameter", PRIVATE_VALUE)
                .put("aPropertyThisNodeDoesNotKnow", 1)
                .toString();

        handler.onMessage(webSocket, json);

        logs.awaitLineContaining("Json deserialization");
        logs.assertNoLineContains(PRIVATE_VALUE);
        assertThat(logs.lines()).anyMatch(line -> line.contains("deserialization of a SubscriptionRequest failed")
                && line.contains("Message length=" + json.length()));
    }

    private DefaultWebSocket connect() {
        DefaultWebSocket webSocket = mock(DefaultWebSocket.class, RETURNS_DEEP_STUBS);
        HttpServletRequest upgradeRequest = mock(HttpServletRequest.class);
        when(upgradeRequest.getAttribute(Attributes.CLIENT_ID)).thenReturn("client-id");
        when(webSocket.getUpgradeRequest()).thenReturn(upgradeRequest);
        handler.onConnect(webSocket);
        return webSocket;
    }

    // The request which creates a user identity carries the private key of the identity in its body
    private static String restRequest(String type) {
        ObjectNode body = JsonMapperProvider.get().createObjectNode();
        body.putObject("keyMaterialResponse").putObject("keyPair").putObject("privateKey")
                .put("encoded", PRIVATE_VALUE);
        ObjectNode request = JsonMapperProvider.get().createObjectNode()
                .put("type", type)
                .put("requestId", "request-id")
                .put("method", "POST")
                .put("path", "/api/v1/user-identities")
                .put("body", body.toString());
        request.putObject("headers").put("Bisq-Session-Id", SESSION_ID);
        return request.toString();
    }

    private static SubscriptionService subscriptionService() {
        return new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                false,
                new RecordedOfferItems(mock(UserService.class, RETURNS_DEEP_STUBS), mock(BondedRolesService.class, RETURNS_DEEP_STUBS)));
    }
}
