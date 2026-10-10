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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.websockets.HandshakeException;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketFilter;
import org.glassfish.grizzly.websockets.WebSocketHolder;

import java.io.IOException;

/**
 * The {@code WebSocketFilter} of Grizzly, which runs at most one opening handshake on a connection.
 *
 * <p>
 * After the headers of an upgrade request, Grizzly passes every later byte of the connection as content of the same
 * request. Grizzly 5.0.1 ({@code BaseWebSocketFilter.handleRead}) starts the handshake again for such content while
 * the WebSocket of the connection is not connected:
 * </p>
 * <ul>
 *     <li>after Grizzly refused the handshake, for example for a {@code Connection} header without {@code Upgrade}.
 *     Grizzly answers 400 and keeps the connection open, and every later byte repeats the refused handshake;</li>
 *     <li>after the WebSocket closed. Grizzly marks the WebSocket closed before it closes the connection, so a frame
 *     which arrives in between opens a second WebSocket with the same upgrade request. The application then gets a
 *     second {@code onConnect} with the identity of the first one, also after the session of that identity ended,
 *     because the filters do not authenticate content of an authenticated upgrade request again.</li>
 * </ul>
 *
 * <p>
 * A server needs neither: the handshake of an upgrade request runs completely while Grizzly handles its first content.
 * So this filter closes the connection instead of a second handshake, and after a refused handshake once the answer
 * is written. The check does not read the state of the WebSocket, which another thread can change at any time.
 * </p>
 */
public class SingleHandshakeWebSocketFilter extends WebSocketFilter {
    /**
     * The add-on of Grizzly with this filter instead of its {@code WebSocketFilter}. The add-ons of the API find this
     * filter by the type {@code WebSocketFilter}.
     */
    public static AddOn addOn() {
        return new WebSocketAddOn() {
            @Override
            protected WebSocketFilter createWebSocketFilter() {
                return new SingleHandshakeWebSocketFilter(getTimeoutInSeconds());
            }
        };
    }

    SingleHandshakeWebSocketFilter(long webSocketTimeoutInSeconds) {
        super(webSocketTimeoutInSeconds);
    }

    @Override
    protected NextAction handleHandshake(FilterChainContext context, HttpContent content) throws IOException {
        // Grizzly creates the holder when the first handshake of a connection starts and never removes it
        if (WebSocketHolder.isWebSocketInProgress(context.getConnection())) {
            context.getConnection().closeSilently();
            return context.getStopAction();
        }
        return super.handleHandshake(context, content);
    }

    @Override
    protected void onHandshakeFailure(Connection connection, HandshakeException e) {
        // After the answer of the refusal, which Grizzly has queued for writing
        connection.closeSilently();
    }
}
