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

package bisq.api.web_socket.domain.network;

import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NetworkInfoWebSocketServiceTest {

    @Test
    void seedRoleChangeIsPushedToSubscribers() {
        SubscriberRepository subscriberRepository = new SubscriberRepository();
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        NetworkInfoWebSocketService service = new NetworkInfoWebSocketService(subscriberRepository,
                networkService(),
                authorizedBondedRolesService);

        service.initialize().join();

        ArgumentCaptor<AuthorizedBondedRolesService.Listener> listenerCaptor =
                ArgumentCaptor.forClass(AuthorizedBondedRolesService.Listener.class);
        verify(authorizedBondedRolesService).addListener(listenerCaptor.capture());
        AuthorizedBondedRolesService.Listener listener = listenerCaptor.getValue();

        WebSocket webSocket = subscribe(subscriberRepository);

        // Authorized data which is not a seed node role must not trigger an update
        listener.onAuthorizedDataAdded(authorizedBondedRoleData(BondedRoleType.MEDIATOR));
        verify(webSocket, never()).send(anyString());

        // Adding and removing a seed node role reclassifies existing connections, so both must trigger an update
        listener.onAuthorizedDataAdded(authorizedBondedRoleData(BondedRoleType.SEED_NODE));
        listener.onAuthorizedDataRemoved(authorizedBondedRoleData(BondedRoleType.SEED_NODE));
        verify(webSocket, times(2)).send(anyString());

        service.shutdown().join();
        verify(authorizedBondedRolesService).removeListener(listener);
    }

    private NetworkService networkService() {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of());
        when(networkService.findDefaultNode(any(TransportType.class))).thenReturn(Optional.empty());
        return networkService;
    }

    private WebSocket subscribe(SubscriberRepository subscriberRepository) {
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> ReadyFutureImpl.create(null)).when(webSocket).send(anyString());
        String requestJson = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"request-1\",\"topic\":\"NETWORK_INFO\"}";
        subscriberRepository.add(SubscriptionRequest.fromJson(requestJson).orElseThrow(), Optional.empty(), webSocket);
        return webSocket;
    }

    private AuthorizedData authorizedBondedRoleData(BondedRoleType bondedRoleType) {
        AuthorizedBondedRole authorizedBondedRole = mock(AuthorizedBondedRole.class);
        when(authorizedBondedRole.getBondedRoleType()).thenReturn(bondedRoleType);
        AuthorizedData authorizedData = mock(AuthorizedData.class);
        when(authorizedData.getAuthorizedDistributedData()).thenReturn(authorizedBondedRole);
        return authorizedData;
    }
}
