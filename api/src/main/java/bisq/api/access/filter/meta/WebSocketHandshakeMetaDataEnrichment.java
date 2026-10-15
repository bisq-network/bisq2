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

package bisq.api.access.filter.meta;

import bisq.api.access.filter.Attributes;
import bisq.api.access.filter.Headers;
import bisq.api.access.filter.HttpRequestFilterUtils;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Records the client which a WebSocket upgrade request claims, if no filter before this one recorded the identity of
 * the client on the upgrade request.
 *
 * <p>
 * This filter inspects incoming messages and acts only when the message is a WebSocket upgrade request, which is
 * every request for which Grizzly starts the handshake. It records the {@code Bisq-Client-Id} header on the upgrade
 * request as the identity which the client claims. With session handling, the authentication filter has always
 * recorded the client of the session, so the header is never used. See {@code WebSocketIdentity}.
 * </p>
 *
 * <p>
 * This filter does not perform authentication or authorization and always
 * delegates request handling to the next filter in the chain.
 * </p>
 */
@Slf4j
public class WebSocketHandshakeMetaDataEnrichment extends BaseFilter {
    @Override
    public NextAction handleRead(FilterChainContext context) {
        Object message = context.getMessage();

        HttpRequestFilterUtils.resolveAsHttpRequest(message)
                .filter(HttpRequestFilterUtils::isWebSocketUpgradeRequest)
                .ifPresent(request -> {
                    // With session handling, the authentication filter has recorded the client of the session.
                    // Without it, the header is the only identity, an unverified claim as on the REST API.
                    if (request.getAttribute(Attributes.CLIENT_ID) == null) {
                        String clientIdHeader = request.getHeader(Headers.CLIENT_ID);
                        if (clientIdHeader != null) {
                            request.setAttribute(Attributes.CLIENT_ID, clientIdHeader);
                        }
                    }
                });

        return context.getInvokeAction();
    }
}
