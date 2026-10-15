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

package bisq.api.web_socket.util;

import bisq.api.access.filter.Attributes;
import bisq.api.access.filter.Headers;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The identity of a WebSocket is what the filter chain recorded on its upgrade request, never a header which the
 * client chose.
 */
class WebSocketIdentityTest {
    private static final String CLIENT_OF_THE_SESSION = "client-a";
    private static final String CLIENT_IN_THE_HEADER = "client-v";
    private static final String SESSION_ID = "session-of-a";

    @Test
    void theIdentityIsTheRecordedClientAndNotTheHeader() {
        HttpServletRequest upgradeRequest = upgradeRequestWithHeaders();
        when(upgradeRequest.getAttribute(Attributes.CLIENT_ID)).thenReturn(CLIENT_OF_THE_SESSION);
        when(upgradeRequest.getAttribute(Attributes.SESSION_ID)).thenReturn(SESSION_ID);
        WebSocket webSocket = webSocketWith(upgradeRequest);

        assertThat(WebSocketIdentity.findClientId(webSocket)).contains(CLIENT_OF_THE_SESSION);
        assertThat(WebSocketIdentity.findSessionId(webSocket)).contains(SESSION_ID);
    }

    @Test
    void anUpgradeRequestWithoutARecordedIdentityHasNone() {
        WebSocket webSocket = webSocketWith(upgradeRequestWithHeaders());

        assertThat(WebSocketIdentity.findClientId(webSocket)).isEmpty();
        assertThat(WebSocketIdentity.findSessionId(webSocket)).isEmpty();
    }

    @Test
    void anEmptyRecordedIdentityIsNone() {
        HttpServletRequest upgradeRequest = upgradeRequestWithHeaders();
        when(upgradeRequest.getAttribute(Attributes.CLIENT_ID)).thenReturn("");
        when(upgradeRequest.getAttribute(Attributes.SESSION_ID)).thenReturn("");
        WebSocket webSocket = webSocketWith(upgradeRequest);

        assertThat(WebSocketIdentity.findClientId(webSocket)).isEmpty();
        assertThat(WebSocketIdentity.findSessionId(webSocket)).isEmpty();
    }

    @Test
    void aWebSocketWithoutAnUpgradeRequestHasNoIdentity() {
        WebSocket webSocket = mock(WebSocket.class);

        assertThat(WebSocketIdentity.findClientId(webSocket)).isEmpty();
        assertThat(WebSocketIdentity.findSessionId(webSocket)).isEmpty();
    }

    // The client chose these headers. They must not count as the identity.
    private static HttpServletRequest upgradeRequestWithHeaders() {
        HttpServletRequest upgradeRequest = mock(HttpServletRequest.class);
        when(upgradeRequest.getHeader(Headers.CLIENT_ID)).thenReturn(CLIENT_IN_THE_HEADER);
        when(upgradeRequest.getHeader(Headers.SESSION_ID)).thenReturn("session-in-the-header");
        return upgradeRequest;
    }

    private static WebSocket webSocketWith(HttpServletRequest upgradeRequest) {
        DefaultWebSocket webSocket = mock(DefaultWebSocket.class);
        when(webSocket.getUpgradeRequest()).thenReturn(upgradeRequest);
        return webSocket;
    }
}
