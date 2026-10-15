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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Rule 4 of docs/specifications/network/validation-at-parsing.md: a store skips a persisted entry which it cannot
// parse and keeps the other entries.
class PeerGroupStoreTest {
    @Test
    void skipsAPersistedPeerWhichCannotBeParsed() throws UnresolvableProtobufException {
        Peer peer = createPeer(1000);
        bisq.network.protobuf.Peer validProto = peer.toProto(false);
        bisq.network.protobuf.Peer invalidProto = createPeer(1001).toProto(false).toBuilder()
                .setCapability(createPeer(1001).getCapability().toProto(false).toBuilder()
                        .setAddress(bisq.common.protobuf.Address.newBuilder().setHost("").setPort(1001)))
                .build();
        bisq.network.protobuf.PeerGroupStore proto = bisq.network.protobuf.PeerGroupStore.newBuilder()
                .addPersistedPeers(validProto)
                .addPersistedPeers(invalidProto)
                .build();

        PeerGroupStore store = PeerGroupStore.fromProto(proto);

        assertThat(store.getPersistedPeersByAddress()).containsOnlyKeys(peer.getAddress());
    }

    private static Peer createPeer(int port) {
        Capability capability = new Capability(Capability.VERSION,
                Address.from("127.0.0.1", port),
                new ArrayList<>(List.of(TransportType.CLEAR)),
                new ArrayList<>(),
                "2.1.14");
        return new Peer(capability, new NetworkLoad(), true, System.currentTimeMillis());
    }
}
