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

package bisq.api.access.filter;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.util.Optional;

/**
 * Rejects a WebSocket upgrade request which is not a valid opening handshake.
 *
 * <p>
 * Grizzly starts the handshake for every request whose {@code Upgrade} value is {@code websocket}
 * (see {@link HttpRequestFilterUtils#isWebSocketUpgradeRequest}) and does not check the HTTP method.
 * RFC 6455 requires the method {@code GET} and the header {@code Sec-WebSocket-Key} for the opening
 * handshake. This filter answers every other upgrade request with status 400 and closes the connection.
 * Such a request then reaches neither the filters behind this one nor the {@code WebSocketFilter} of
 * Grizzly.
 * </p>
 *
 * <p>
 * The rule does not depend on session handling, so {@link WebSocketFilterAddOn} installs this filter for
 * every configuration. A request without the upgrade value {@code websocket} passes unchanged.
 * </p>
 */
@Slf4j
public class WebSocketHandshakeValidationFilter extends BaseFilter {
    @Override
    public NextAction handleRead(FilterChainContext context) {
        Optional<HttpRequestPacket> upgradeRequest = HttpRequestFilterUtils.resolveAsHttpRequest(context.getMessage())
                .filter(HttpRequestFilterUtils::isWebSocketUpgradeRequest);
        if (upgradeRequest.isEmpty()) {
            return context.getInvokeAction();
        }

        HttpRequestPacket request = upgradeRequest.get();
        // The reasons are fixed texts. The method and the headers come from the caller and are not logged.
        if (request.getMethod() != Method.GET) {
            log.warn("WebSocket upgrade rejected: the method is not GET");
            return HttpRequestFilterUtils.rejectAndClose(context, request, HttpStatus.BAD_REQUEST_400);
        }
        if (request.getHeader(Headers.SEC_WEBSOCKET_KEY) == null) {
            log.warn("WebSocket upgrade rejected: the header Sec-WebSocket-Key is missing");
            return HttpRequestFilterUtils.rejectAndClose(context, request, HttpStatus.BAD_REQUEST_400);
        }
        return context.getInvokeAction();
    }
}
