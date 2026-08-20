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

package bisq.api.access;

import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.web_socket.WebSocketService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApiAccessServiceTest {
    private final PairingService pairingService = mock(PairingService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final WebSocketService webSocketService = mock(WebSocketService.class);
    private final ApiAccessService apiAccessService =
            new ApiAccessService(pairingService, sessionService, Optional.of(webSocketService));

    /**
     * Revoking has to end the connection, not only the credentials. A subscription is authorised
     * once, at subscribe time, and a {@code Subscriber} holds a socket rather than a clientId, so
     * nothing re-checks the grant afterwards and nothing can drop one client's subscriptions by
     * name. An open socket therefore keeps receiving every topic it had already taken out, with no
     * reason to ever fail on its own — closing it is the only thing that stops the push.
     * <p>
     * The order between the two is load-bearing, not tidiness. Closing only moves the socket to
     * CLOSING, and it keeps accepting sends until the close frame is flushed.
     * {@code SubscriptionService#subscribe} re-reads the grant right after adding a subscriber so a
     * subscription racing the revocation is refused instead of being served its snapshot on that
     * socket, and that read only sees the grant gone because it went first. Close first and the
     * re-read still finds the grant, so the snapshot goes out.
     */
    @Test
    void revokingAClientClosesItsConnectionAfterRemovingItsGrant() {
        when(pairingService.revokeClientProfile("client-1")).thenReturn(true);

        assertThat(apiAccessService.revokeClient("client-1")).isTrue();

        verify(sessionService).removeSessionByClientId("client-1");
        InOrder grantBeforeSocket = inOrder(pairingService, webSocketService);
        grantBeforeSocket.verify(pairingService).revokeClientProfile("client-1");
        grantBeforeSocket.verify(webSocketService).disconnectClient("client-1");
    }

    /**
     * The profile being gone says nothing about the connection: it may have been removed by another
     * path while the socket it was paired with is still open and still being pushed to. Both
     * cleanups run on their own, which is what the session removal already did.
     */
    @Test
    void revokingAClientWithNoProfileStillClosesItsConnection() {
        when(pairingService.revokeClientProfile("client-1")).thenReturn(false);

        assertThat(apiAccessService.revokeClient("client-1")).isFalse();

        verify(sessionService).removeSessionByClientId("client-1");
        verify(webSocketService).disconnectClient("client-1");
    }

    /** Node-monitor runs no WebSocket server, so there is no connection to end. */
    @Test
    void revokingAClientOnANodeWithoutAWebSocketServerJustRemovesTheProfile() {
        ApiAccessService withoutWebSocket =
                new ApiAccessService(pairingService, sessionService, Optional.empty());
        when(pairingService.revokeClientProfile("client-1")).thenReturn(true);

        assertThat(withoutWebSocket.revokeClient("client-1")).isTrue();

        verify(sessionService).removeSessionByClientId("client-1");
        verifyNoInteractions(webSocketService);
    }
}
