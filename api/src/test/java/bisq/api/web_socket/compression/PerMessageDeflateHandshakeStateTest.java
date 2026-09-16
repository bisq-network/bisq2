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

package bisq.api.web_socket.compression;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.IndexedAttributeHolder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The offer belongs to the upgrade that carried it. Drives the handshake through the filter directly,
 * because Grizzly closes a connection whose upgrade failed and an end-to-end test therefore cannot
 * observe what the state does afterwards.
 */
class PerMessageDeflateHandshakeStateTest {
    private static final String EXTENSIONS = "Sec-WebSocket-Extensions";

    private final PerMessageDeflateFilter filter = new PerMessageDeflateFilter();
    private Connection<?> connection;

    @BeforeEach
    void setUp() {
        connection = mock(Connection.class);
        when(connection.getAttributes()).thenReturn(new IndexedAttributeHolder(Grizzly.DEFAULT_ATTRIBUTE_BUILDER));
    }

    @Test
    void confirmsTheExtensionForAnUpgradeThatOfferedIt() {
        offerExtension();

        assertThat(respondWith(101).getHeader(EXTENSIONS))
                .isEqualTo("permessage-deflate; client_no_context_takeover; server_no_context_takeover");
    }

    @Test
    void doesNotConfirmTheExtensionForAnUpgradeThatDidNotOfferIt() {
        handshakeRequestWithout();

        assertThat(respondWith(101).getHeader(EXTENSIONS)).isNull();
    }

    @Test
    void forgetsTheOfferOfAnUpgradeThatFailed() {
        offerExtension();
        respondWith(400);

        // The retry does not offer it, so it must not be confirmed either
        handshakeRequestWithout();

        assertThat(respondWith(101).getHeader(EXTENSIONS)).isNull();
    }

    private void offerExtension() {
        HttpRequestPacket request = handshakeRequest();
        request.addHeader(EXTENSIONS, "permessage-deflate");
        filter.handleRead(contextFor(HttpContent.builder(request).build()));
    }

    private void handshakeRequestWithout() {
        filter.handleRead(contextFor(HttpContent.builder(handshakeRequest()).build()));
    }

    private HttpResponsePacket respondWith(int status) {
        HttpResponsePacket response = HttpResponsePacket.builder(handshakeRequest())
                .protocol(Protocol.HTTP_1_1)
                .status(status)
                .build();
        filter.handleWrite(contextFor(HttpContent.builder(response).build()));
        return response;
    }

    private static HttpRequestPacket handshakeRequest() {
        return HttpRequestPacket.builder()
                .method("GET")
                .uri("/websocket")
                .protocol(Protocol.HTTP_1_1)
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                .build();
    }

    private FilterChainContext contextFor(HttpContent message) {
        FilterChainContext ctx = mock(FilterChainContext.class);
        when(ctx.getConnection()).thenReturn(connection);
        when(ctx.getMessage()).thenReturn(message);
        return ctx;
    }
}
