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
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.HttpStatus;

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
     * Determines whether Grizzly starts the WebSocket handshake for the given HTTP request.
     *
     * <p>
     * {@code BaseWebSocketFilter.handleRead} of Grizzly 5.0.1 starts the handshake for every request whose
     * {@code Upgrade} value equals {@code websocket}, ignoring case. It does not check the HTTP method or the
     * {@code Sec-WebSocket-Key} header before that. This method reads the same value and compares it in the
     * same way.
     * </p>
     *
     * <p>
     * Every filter which must see each upgrade uses this method, so that the filters and Grizzly have one
     * definition of an upgrade request. A filter with a stricter definition does not see a request which
     * Grizzly still upgrades.
     * </p>
     *
     * @param request the HTTP request packet
     * @return true if Grizzly treats the request as a WebSocket upgrade request
     */
    public static boolean isWebSocketUpgradeRequest(HttpRequestPacket request) {
        return HeaderValues.WEBSOCKET.equalsIgnoreCase(request.getUpgrade());
    }

    /**
     * Answers the request with the given status and an empty body, and closes the connection.
     *
     * @return the stop action, so that no later filter processes the request
     */
    public static NextAction rejectAndClose(FilterChainContext ctx, HttpRequestPacket request, HttpStatus status) {
        HttpResponsePacket response = HttpResponsePacket.builder(request)
                .status(status.getStatusCode())
                .protocol(Protocol.HTTP_1_1)
                .contentLength(0)
                .build();
        ctx.write(response);
        ctx.getConnection().closeSilently();
        return ctx.getStopAction();
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
