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
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.util.Optional;

@Slf4j
public class WebSocketHandshakeAuthenticationFilter extends BaseFilter {
    private final SessionAuthenticationService sessionAuthenticationService;

    public WebSocketHandshakeAuthenticationFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public NextAction handleRead(FilterChainContext context) {
        Object message = context.getMessage();

        Optional<HttpRequestPacket> requestOpt =
                HttpRequestFilterUtils.resolveAsHttpRequest(message)
                        .filter(HttpRequestFilterUtils::isWebsocketHandshakeRequest);

        if (requestOpt.isEmpty()) {
            // Ignore filter if not a WebSocket handshake request
            return context.getInvokeAction();
        }

        // Defensive guard: WebSocket authentication is connection-scoped and must be idempotent.
        // Although a WebSocket handshake is logically performed once per connection,
        // the Grizzly filter chain may deliver the HTTP upgrade request in multiple
        // chunks or re-enter this filter before the protocol upgrade is finalized.
        // If authentication has already been completed for this connection, we must
        // skip re-authentication to avoid duplicate validation, false rejection, or
        // rebinding of identity.
        if (HttpRequestFilterUtils.hasConnectionAttribute(context, Attributes.IS_AUTHENTICATED)) {
            return context.getInvokeAction();
        }

        if (context.getConnection().getAttributes() == null) {
            log.error("WebSocket auth rejected: missing connection attributes");
            HttpResponsePacket response = HttpResponsePacket.builder(requestOpt.get())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode())
                    .protocol(Protocol.HTTP_1_1)
                    .contentLength(0)
                    .build();
            context.write(response);
            context.getConnection().closeSilently();
            return context.getStopAction();
        }
        HttpRequestPacket request = requestOpt.get();
        try {
            AuthenticatedSession session = sessionAuthenticationService.authenticate(
                    request.getHeader(Headers.CLIENT_ID),
                    request.getHeader(Headers.SESSION_ID)
            );

            HttpRequestFilterUtils.setConnectionAttribute(context, Attributes.IS_AUTHENTICATED, true);
            HttpRequestFilterUtils.setConnectionAttribute(context, Attributes.SESSION_ID, session.getSessionId());
            HttpRequestFilterUtils.setConnectionAttribute(context, Attributes.CLIENT_ID, session.getClientId());

            return context.getInvokeAction();

        } catch (AuthenticationException e) {
            log.warn("WebSocket auth rejected: {}", e.getMessage());

            HttpResponsePacket response = HttpResponsePacket.builder(request)
                    .status(HttpStatus.UNAUTHORIZED_401.getStatusCode())
                    .protocol(Protocol.HTTP_1_1)
                    .contentLength(0)
                    .build();
            context.write(response);
            context.getConnection().closeSilently();

            return context.getStopAction();
        }
    }
}

