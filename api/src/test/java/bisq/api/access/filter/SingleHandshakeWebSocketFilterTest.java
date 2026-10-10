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
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SingleHandshakeWebSocketFilterTest {
    // Grizzly starts a handshake for content of a connection whose WebSocket is not connected, also when another
    // thread closed the WebSocket during the read. The connection already had its handshake, so it must be closed.
    @Test
    void aConnectionWhichHadAHandshakeStartsNoSecondOne() throws Exception {
        Connection<?> connection = mock(Connection.class);
        when(connection.getAttributes()).thenReturn(Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createSafeAttributeHolder());
        WebSocketHolder.set(connection, mock(ProtocolHandler.class), mock(WebSocket.class));
        NextAction stopAction = mock(NextAction.class);
        FilterChainContext context = mock(FilterChainContext.class);
        when(context.getConnection()).thenReturn(connection);
        when(context.getStopAction()).thenReturn(stopAction);
        HttpContent content = mock(HttpContent.class);
        when(content.getHttpHeader()).thenReturn(HttpRequestPacket.builder()
                .method(Method.GET)
                .uri("/single-handshake-test")
                .header("Upgrade", "websocket")
                .build());

        NextAction action = new SingleHandshakeWebSocketFilter(60).handleHandshake(context, content);

        assertThat(action).isSameAs(stopAction);
        verify(connection).closeSilently();
    }
}
