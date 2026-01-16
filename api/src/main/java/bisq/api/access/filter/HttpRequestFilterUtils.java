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

import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;

import java.util.Optional;

/**
 * Utility methods for resolving and inspecting HTTP requests within
 * filter chain messages.
 */
public class HttpRequestFilterUtils {
    /**
     * Attempts to resolve the given message as an HTTP request.
     *
     * @param message the message from the filter chain
     * @return an {@link Optional} containing the {@link HttpRequestPacket}
     * if the message represents an HTTP request, otherwise empty
     */
    public static Optional<HttpRequestPacket> resolveAsHttpRequest(Object message) {
        if (message instanceof HttpContent httpContent
                && httpContent.getHttpHeader() instanceof HttpRequestPacket request) {
            return Optional.of(request);
        }
        return Optional.empty();
    }

    /**
     * Determines whether the given HTTP request represents a valid
     * WebSocket handshake request.
     *
     * @param request the HTTP request packet
     * @return true if the request matches WebSocket handshake requirements
     */
    public static boolean isWebsocketHandshakeRequest(HttpRequestPacket request) {
        return request.getMethod() == Method.GET
                && HeaderValues.WEBSOCKET.equalsIgnoreCase(request.getHeader(Headers.UPGRADE))
                && request.getHeader(Headers.SEC_WEBSOCKET_KEY) != null;
    }

    public static boolean hasConnectionAttribute(FilterChainContext ctx, String attribute) {
        AttributeHolder holder = ctx.getConnection().getAttributes();
        return holder != null && holder.getAttribute(attribute) != null;
    }

    public static void setConnectionAttribute(FilterChainContext ctx, String attribute, Object value) {
        AttributeHolder holder = ctx.getConnection().getAttributes();
        if (holder != null) {
            holder.setAttribute(attribute, value);
        }
    }
}
