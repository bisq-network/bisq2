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

package bisq.api.access.filter.authn;

import bisq.api.access.filter.Attributes;
import bisq.api.access.filter.Headers;
import bisq.api.access.filter.HttpRequestFilterUtils;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.util.Optional;

@Slf4j
public class WebSocketSessionAuthenticationFilter extends BaseFilter {
    private final SessionAuthenticationService sessionAuthenticationService;

    public WebSocketSessionAuthenticationFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public NextAction handleRead(FilterChainContext context) {
        Object message = context.getMessage();

        // Every request which Grizzly upgrades to a WebSocket must be authenticated, whatever its method and
        // its other headers are. So the filter uses the same condition as Grizzly.
        Optional<HttpRequestPacket> requestOpt =
                HttpRequestFilterUtils.resolveAsHttpRequest(message)
                        .filter(HttpRequestFilterUtils::isWebSocketUpgradeRequest);

        if (requestOpt.isEmpty()) {
            // Ignore filter if not a WebSocket upgrade request
            return context.getInvokeAction();
        }

        HttpRequestPacket request = requestOpt.get();
        // After the headers of the upgrade request, Grizzly passes every later byte of the connection as content of
        // the same request: the WebSocket frames, and the bytes after a handshake which Grizzly refused or after the
        // WebSocket closed. They reach this filter with the request which was already authenticated. Content never
        // starts a second handshake with this request, because SingleHandshakeWebSocketFilter starts at most one
        // handshake on a connection and closes the connection instead of a second one. The mark is on the request and
        // not on the connection: another request on the same connection is authenticated again. Only the value which
        // this filter writes counts as the mark.
        if (Boolean.TRUE.equals(request.getAttribute(Attributes.IS_AUTHENTICATED))) {
            return context.getInvokeAction();
        }

        try {
            AuthenticatedSession session = sessionAuthenticationService.authenticate(
                    request.getHeader(Headers.CLIENT_ID),
                    request.getHeader(Headers.SESSION_ID)
            );

            // The identity of the WebSocket (see WebSocketIdentity). A client cannot set request attributes. The REST
            // filter records its properties, which Jersey keeps as attributes of the same request objects, under
            // other names (see Attributes).
            request.setAttribute(Attributes.IS_AUTHENTICATED, true);
            request.setAttribute(Attributes.SESSION_ID, session.getSessionId());
            request.setAttribute(Attributes.CLIENT_ID, session.getClientId());

            return context.getInvokeAction();
        } catch (AuthenticationException e) {
            log.warn("WebSocket auth rejected: {}", e.getMessage());
            return HttpRequestFilterUtils.rejectAndClose(context, request, HttpStatus.UNAUTHORIZED_401);
        }
    }
}

