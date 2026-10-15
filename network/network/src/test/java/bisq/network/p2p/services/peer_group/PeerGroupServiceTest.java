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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.TorKeyGeneration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// docs/specifications/network/peer-exchange.md
class PeerGroupServiceTest {
    private static final int MAX_REPORTED = 5;
    private static final int MAX_PERSISTED = 3;
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);

    @SuppressWarnings("rawtypes")
    private final Persistence persistence = mock(Persistence.class);

    @Test
    void peersOfAnotherTransportAreNotKept() {
        PeerGroupService service = createService(TransportType.TOR);
        Peer torPeer = torPeer(now() - MINUTE);
        Peer clearnetPeer = clearnetPeer(1000, now() - MINUTE);

        service.addReportedPeers(Set.of(torPeer, clearnetPeer));
        service.addPersistedPeers(Set.of(torPeer, clearnetPeer));

        assertThat(service.getReportedPeers()).containsExactly(torPeer);
        assertThat(service.getPersistedPeers()).containsExactly(torPeer);
    }

    @Test
    void limitsKeepTheNewestPeersWhenPeersAreAdded() {
        PeerGroupService service = createService(TransportType.CLEAR);
        // Port 1000 is the newest peer, port 1007 the oldest
        List<Peer> peers = IntStream.range(0, 8)
                .mapToObj(i -> clearnetPeer(1000 + i, now() - (i + 1) * MINUTE))
                .toList();

        service.addReportedPeers(Set.copyOf(peers));
        service.addPersistedPeers(Set.copyOf(peers));

        assertThat(service.getReportedPeers()).containsExactlyInAnyOrderElementsOf(peers.subList(0, MAX_REPORTED));
        assertThat(service.getPersistedPeers()).containsExactlyInAnyOrderElementsOf(peers.subList(0, MAX_PERSISTED));
    }

    @Test
    void peerDatedInTheFutureDoesNotPushOutAnHonestPeer() {
        PeerGroupService service = createService(TransportType.CLEAR);
        Set<Peer> honestPeers = Set.of(clearnetPeer(1000, now() - MINUTE),
                clearnetPeer(1001, now() - 2 * MINUTE),
                clearnetPeer(1002, now() - 3 * MINUTE));
        service.addPersistedPeers(honestPeers);

        Peer futureDatedPeer = clearnetPeer(2000, now() + TimeUnit.HOURS.toMillis(1));
        service.addReportedPeers(Set.of(futureDatedPeer));
        service.addPersistedPeers(Set.of(futureDatedPeer));

        assertThat(service.getPersistedPeers()).containsExactlyInAnyOrderElementsOf(honestPeers);
        assertThat(service.getReportedPeers()).isEmpty();
    }

    @Test
    void peerDatedALittleInTheFutureIsKept() {
        PeerGroupService service = createService(TransportType.CLEAR);
        Peer peer = clearnetPeer(1000, now() + PeerGroupService.MAX_DATE_IN_FUTURE / 2);

        service.addReportedPeers(Set.of(peer));

        assertThat(service.getReportedPeers()).containsExactly(peer);
    }

    // A store which an earlier version wrote can contain peers of all transports and more peers than the limit
    @SuppressWarnings("unchecked")
    @Test
    void loadedStoreKeepsOnlyTheNewestPeersOfOurTransportWithinTheLimit() throws UnresolvableProtobufException {
        PeerGroupService service = createService(TransportType.TOR);
        List<Peer> torPeers = IntStream.range(0, 5)
                .mapToObj(i -> torPeer(now() - (i + 1) * MINUTE))
                .toList();
        List<Peer> storedPeers = new ArrayList<>(torPeers);
        storedPeers.add(clearnetPeer(1000, now()));
        storedPeers.add(clearnetPeer(1001, now()));
        storedPeers.add(torPeer(now() + TimeUnit.HOURS.toMillis(1)));
        PeerGroupStore storedStore = PeerGroupStore.fromProto(bisq.network.protobuf.PeerGroupStore.newBuilder()
                .addAllPersistedPeers(storedPeers.stream().map(peer -> peer.toProto(false)).collect(Collectors.toList()))
                .build());
        when(persistence.read()).thenReturn(Optional.of(storedStore));

        service.readPersisted();

        assertThat(service.getPersistedPeers()).containsExactlyInAnyOrderElementsOf(torPeers.subList(0, MAX_PERSISTED));
        // The pruned store is written, so the file shrinks at once
        verify(persistence).persistAsync(any());
    }

    @SuppressWarnings("unchecked")
    private PeerGroupService createService(TransportType transportType) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        return new PeerGroupService(persistenceService,
                transportType,
                new PeerGroupService.Config(),
                Set.of(),
                new BanList(),
                MAX_REPORTED,
                MAX_PERSISTED);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static Peer torPeer(long created) {
        return peer(Address.from(TorKeyGeneration.generateKeyPair().getOnionAddress(), 1000), TransportType.TOR, created);
    }

    private static Peer clearnetPeer(int port, long created) {
        return peer(Address.from("127.0.0.1", port), TransportType.CLEAR, created);
    }

    private static Peer peer(Address address, TransportType transportType, long created) {
        Capability capability = new Capability(Capability.VERSION,
                address,
                new ArrayList<>(List.of(transportType)),
                new ArrayList<>(),
                "2.1.14");
        return new Peer(capability, new NetworkLoad(), true, created);
    }
}
