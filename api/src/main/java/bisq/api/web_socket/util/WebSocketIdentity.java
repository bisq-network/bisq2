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
import bisq.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Optional;

/**
 * Reads the identity of a WebSocket connection: the client and the session which the filter chain recorded on
 * its upgrade request.
 * <p>
 * The identity lives on the upgrade request and not on the messages, because the handshake is the one point
 * where the connection is authenticated, and a client can put anything in a frame afterwards. It is read from
 * request attributes, never from a header: a client cannot set an attribute, and the filters set them once,
 * before Grizzly opens the WebSocket. No code may write them later. Other threads read them without a lock, and they
 * see the values only because they reach the WebSocket after it opened.
 * <ul>
 *     <li>With session handling, {@code WebSocketSessionAuthenticationFilter} records the client and the session
 *     of the session which authenticated the upgrade request. The {@code Bisq-Client-Id} header must name the
 *     same client, or the upgrade is rejected.</li>
 *     <li>Without session handling, {@code WebSocketHandshakeMetaDataEnrichment} records the
 *     {@code Bisq-Client-Id} header as the client which the connection claims, and there is no session. That
 *     claim is exactly as trustworthy as the same header on the REST API, which is to say not at all.</li>
 * </ul>
 * Every decision about an open WebSocket reads it here: the authorization of a subscription, the revocation of a
 * client, the list of connected clients, the identity which the REST proxy forwards, and the check whether the
 * session of the WebSocket still holds.
 */
public final class WebSocketIdentity {
    private WebSocketIdentity() {
    }

    public static Optional<HttpServletRequest> findUpgradeRequest(WebSocket webSocket) {
        return webSocket instanceof DefaultWebSocket defaultWebSocket
                ? Optional.ofNullable(defaultWebSocket.getUpgradeRequest())
                : Optional.empty();
    }

    public static Optional<String> findClientId(WebSocket webSocket) {
        return findAttribute(webSocket, Attributes.CLIENT_ID);
    }

    public static Optional<String> findSessionId(WebSocket webSocket) {
        return findAttribute(webSocket, Attributes.SESSION_ID);
    }

    private static Optional<String> findAttribute(WebSocket webSocket, String name) {
        return findUpgradeRequest(webSocket)
                .map(upgradeRequest -> upgradeRequest.getAttribute(name))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StringUtils::isNotEmpty);
    }
}
