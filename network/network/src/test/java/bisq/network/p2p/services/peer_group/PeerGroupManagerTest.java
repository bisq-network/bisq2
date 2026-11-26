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

package bisq.network.p2p.services.peer_group;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeService;
import bisq.network.p2p.services.peer_group.keep_alive.KeepAliveService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.TorKeyGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeerGroupManagerTest {
    // doInitialize cannot run twice: a second call finds the state INITIALIZING and does nothing. So a failure of a step
    // must fail the initialization at once instead of leaving the peer group in INITIALIZING.
    @Test
    @Timeout(10)
    void aFailedStepOfTheInitializationFailsItAtOnce() {
        Node node = mock(Node.class);
        IllegalStateException failure = new IllegalStateException("The peer exchange could not register at the node");
        doThrow(failure).when(node).addListener(argThat(listener -> listener instanceof PeerExchangeService));
        PeerGroupManager peerGroupManager = new PeerGroupManager(node, peerGroupService(Set.of()), new BanList(),
                config());

        assertThatThrownBy(peerGroupManager::initialize).isSameAs(failure);

        verify(node, times(1)).addListener(argThat(listener -> listener instanceof PeerExchangeService));
        assertThat(peerGroupManager.getState().get()).isEqualTo(PeerGroupManager.State.INITIALIZING);
    }

    // The shutdown of the node ends the open connection attempts, so the initial peer exchange completes after the
    // shutdown of the peer group. The periodic tasks must not start then, and the state must stay TERMINATED.
    @Test
    @Timeout(10)
    void aShutdownDuringTheInitialPeerExchangeStartsNoPeriodicTasks() {
        Address seedAddress = Address.from(TorKeyGeneration.generateKeyPair().getOnionAddress(), 1000);
        Node node = mock(Node.class);
        when(node.notMyself(any(Address.class))).thenReturn(true);
        when(node.getAllActiveConnections()).thenAnswer(invocation -> Stream.empty());
        CompletableFuture<Connection> connectionToSeed = new CompletableFuture<>();
        when(node.getOrCreateConnectionAsync(seedAddress)).thenReturn(connectionToSeed);
        PeerGroupManager peerGroupManager = new PeerGroupManager(node, peerGroupService(Set.of(seedAddress)),
                new BanList(), config());

        CompletableFuture<Void> initialization = CompletableFuture.runAsync(peerGroupManager::initialize);
        verify(node, timeout(5_000)).getOrCreateConnectionAsync(seedAddress);
        peerGroupManager.shutdown();
        connectionToSeed.completeExceptionally(new IOException("The node was shut down"));

        assertThat(initialization).succeedsWithin(Duration.ofSeconds(5));
        assertThat(peerGroupManager.getState().get()).isEqualTo(PeerGroupManager.State.TERMINATED);
        verify(node, never()).addListener(argThat(listener -> listener instanceof KeepAliveService));
    }

    private static PeerGroupManager.Config config() {
        return new PeerGroupManager.Config(new PeerGroupService.Config(),
                new PeerExchangeService.Config(2, 10, 10, true),
                new KeepAliveService.Config(180_000, 90_000, 30_000),
                1_000,
                60_000,
                60_000,
                3_600_000,
                500,
                100,
                10);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static PeerGroupService peerGroupService(Set<Address> seedAddresses) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        return new PeerGroupService(persistenceService,
                TransportType.TOR,
                new PeerGroupService.Config(),
                seedAddresses,
                new BanList(),
                500,
                100);
    }
}
