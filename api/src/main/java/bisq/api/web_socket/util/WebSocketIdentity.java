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

import bisq.api.access.filter.Headers;
import bisq.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Optional;

/**
 * Reads the client identity a WebSocket connection claims.
 * <p>
 * It lives on the upgrade request and not on the messages, because the handshake is the one point
 * where the connection could have been authenticated, and a client can put anything in a frame
 * afterwards. Shared because two callers need it for opposite reasons: the REST proxy forwards it
 * to the REST server so its filters can authorise the call, and the subscription service authorises
 * the topic itself.
 * <p>
 * How much this is worth depends on the deployment, and today it is worth less than the name
 * suggests. {@code WebSocketFilterAddOn} registers the session authentication filter only when
 * {@code supportSessionHandling} is set, and every shipped app config runs with it off, so in
 * practice the handshake authenticates nothing and {@code Bisq-Client-Id} is an unvalidated header
 * that any caller can set. That is not a gap this class introduces — {@code RestApiAuthorizationFilter}
 * reads the same raw header, and reading the enriched request attribute instead would not help,
 * since it is filled from that header too. Treat what this returns as a claim that is exactly as
 * trustworthy as the handshake was, and do not build anything on it that assumes more.
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
        return findUpgradeRequest(webSocket)
                .map(upgradeRequest -> upgradeRequest.getHeader(Headers.CLIENT_ID))
                .filter(StringUtils::isNotEmpty);
    }
}
