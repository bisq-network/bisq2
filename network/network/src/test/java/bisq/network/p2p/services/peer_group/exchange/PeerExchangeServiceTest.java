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

package bisq.network.p2p.services.peer_group.exchange;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.TorKeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// docs/specifications/network/peer-exchange.md
class PeerExchangeServiceTest {
    private PeerExchangeService peerExchangeService;

    @AfterEach
    void tearDown() {
        if (peerExchangeService != null) {
            peerExchangeService.shutdown();
        }
    }

    @Test
    void initialPeerExchangeWhichTimesOutCompletesWithoutSuccessAndRetries() throws Exception {
        Address seedAddress = Address.from(TorKeyGeneration.generateKeyPair().getOnionAddress(), 1000);
        Node node = mock(Node.class);
        when(node.notMyself(any(Address.class))).thenReturn(true);
        when(node.getAllActiveConnections()).thenAnswer(invocation -> Stream.empty());
        // The connection to the seed node does not complete within the batch timeout, as with slow bridges
        when(node.getOrCreateConnectionAsync(seedAddress)).thenAnswer(invocation -> new CompletableFuture<Connection>());
        peerExchangeService = new PeerExchangeService(node,
                peerGroupService(seedAddress),
                new PeerExchangeService.Config(2, 10, 10, true),
                new PeerGroupService.Config(),
                100);
        peerExchangeService.initialize();

        CompletableFuture<Boolean> result = peerExchangeService.startInitialPeerExchange();

        // A timeout must not fail the start of the node, which would end the application
        assertThat(result.get(5, SECONDS)).isFalse();
        // The retry continues in the background after the result completed
        verify(node, timeout(5_000).atLeast(2)).getOrCreateConnectionAsync(seedAddress);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static PeerGroupService peerGroupService(Address seedAddress) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        return new PeerGroupService(persistenceService,
                TransportType.TOR,
                new PeerGroupService.Config(),
                Set.of(seedAddress),
                new BanList(),
                500,
                100);
    }
}
