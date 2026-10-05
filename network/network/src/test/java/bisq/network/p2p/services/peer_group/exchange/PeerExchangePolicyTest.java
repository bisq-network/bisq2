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
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.network.p2p.services.peer_group.Peer;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.TorKeyGeneration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// docs/specifications/network/peer-exchange.md
class PeerExchangePolicyTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void clearnetPeersReportedToATorNodeAreNotStored() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        PeerGroupService peerGroupService = new PeerGroupService(persistenceService,
                TransportType.TOR,
                new PeerGroupService.Config(),
                Set.of(),
                new BanList(),
                500,
                100);
        Node node = mock(Node.class);
        when(node.notMyself(any(Address.class))).thenReturn(true);
        PeerExchangePolicy policy = new PeerExchangePolicy(peerGroupService,
                node,
                new PeerExchangeService.Config(3, 10, 10, true),
                new PeerGroupService.Config());
        Address reporterAddress = Address.from(TorKeyGeneration.generateKeyPair().getOnionAddress(), 9999);
        Peer torPeer = peer(Address.from(TorKeyGeneration.generateKeyPair().getOnionAddress(), 1000), TransportType.TOR);
        // Every IP literal with a valid port becomes a clearnet address, also in a message to a Tor node
        Peer clearnetPeer = peer(Address.from("203.0.113.7", 1000), TransportType.CLEAR);

        policy.addReportedPeers(List.of(torPeer, clearnetPeer), reporterAddress);

        assertThat(peerGroupService.getReportedPeers()).containsExactly(torPeer);
        assertThat(peerGroupService.getPersistedPeers()).containsExactly(torPeer);
    }

    private static Peer peer(Address address, TransportType transportType) {
        Capability capability = new Capability(Capability.VERSION,
                address,
                new ArrayList<>(List.of(transportType)),
                new ArrayList<>(),
                "2.1.14");
        return new Peer(capability, new NetworkLoad(), true, System.currentTimeMillis() - 60_000);
    }
}
