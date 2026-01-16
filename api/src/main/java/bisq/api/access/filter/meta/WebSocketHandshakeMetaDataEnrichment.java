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
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * Enriches the connection context with metadata extracted from a WebSocket
 * handshake request.
 *
 * <p>
 * This filter inspects incoming messages and applies metadata only when the
 * message represents a valid WebSocket HTTP handshake request. The metadata
 * is stored on the connection attributes and is intended to be reused for
 * the lifetime of the WebSocket connection.
 * </p>
 *
 * <p>
 * The following attributes may be populated:
 * </p>
 * <ul>
 *     <li>Remote address of the peer</li>
 *     <li>User-Agent header (or a fallback value if absent)</li>
 * </ul>
 *
 * <p>
 * Attribute enrichment is idempotent and will only occur once per connection.
 * Subsequent handshake messages (if any) will not override existing values.
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
                .filter(HttpRequestFilterUtils::isWebsocketHandshakeRequest)
                .ifPresent(request -> {
                    // We apply the attributes only at the Websocket handshake request
                    Connection<?> connection = context.getConnection();
                    AttributeHolder connectionAttributes = connection.getAttributes();
                    if (connectionAttributes != null && connectionAttributes.getAttribute(Attributes.USER_AGENT) == null) {
                        String userAgent = request.getHeader(Headers.USER_AGENT);
                        connectionAttributes.setAttribute(Attributes.USER_AGENT, Objects.requireNonNullElse(userAgent, "-"));

                        Object peer = connection.getPeerAddress();
                        if (peer instanceof SocketAddress) {
                            connectionAttributes.setAttribute(Attributes.REMOTE_ADDRESS, peer.toString());
                        }

                    }
                });


        return context.getInvokeAction();
    }
}
