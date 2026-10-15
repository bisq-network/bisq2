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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated("Changes the static peer limit of the peer exchange messages")
class PeerExchangeMessageTest {
    private static final int MAX_NUM_PEERS = 500;

    private long previousMaxNumPeersOfRequest;
    private long previousMaxNumPeersOfResponse;

    @BeforeEach
    void setUp() {
        previousMaxNumPeersOfRequest = PeerExchangeRequest.maxNumPeers;
        previousMaxNumPeersOfResponse = PeerExchangeResponse.maxNumPeers;
        PeerExchangeRequest.setMaxNumPeers(MAX_NUM_PEERS);
        PeerExchangeResponse.setMaxNumPeers(MAX_NUM_PEERS);
    }

    @AfterEach
    void tearDown() {
        PeerExchangeRequest.setMaxNumPeers(previousMaxNumPeersOfRequest);
        PeerExchangeResponse.setMaxNumPeers(previousMaxNumPeersOfResponse);
    }

    // Nodes up to 2.1.13 accept and relay a peer with a network load which is not a number. A node of this version
    // rejects such a peer. Before, it dropped every peer exchange message which contained one, so peer exchange between
    // the two versions failed in both directions.
    @Test
    void peerWhichCannotBeParsedIsSkippedAndTheOtherPeersAreKept() throws UnresolvableProtobufException {
        Peer valid = peer("a");
        bisq.network.protobuf.Peer invalid = peer("b").toProto(false).toBuilder()
                .setNetworkLoad(bisq.network.protobuf.NetworkLoad.newBuilder().setLoad(Double.NaN))
                .build();

        PeerExchangeResponse response = PeerExchangeResponse.fromProto(bisq.network.protobuf.PeerExchangeResponse.newBuilder()
                .setNonce(1)
                .addPeers(valid.toProto(false))
                .addPeers(invalid)
                .build());
        PeerExchangeRequest request = PeerExchangeRequest.fromProto(bisq.network.protobuf.PeerExchangeRequest.newBuilder()
                .setNonce(1)
                .addPeers(invalid)
                .addPeers(valid.toProto(false))
                .build());

        assertEquals(List.of(valid.getAddress()), response.getPeers().stream().map(Peer::getAddress).toList());
        assertEquals(List.of(valid.getAddress()), request.getPeers().stream().map(Peer::getAddress).toList());
    }

    // The peers are not part of the hash, so a skipped peer does not change the proof of work payload
    @Test
    void skippedPeerDoesNotChangeTheHash() throws UnresolvableProtobufException {
        bisq.network.protobuf.Peer invalid = peer("b").toProto(false).toBuilder()
                .setNetworkLoad(bisq.network.protobuf.NetworkLoad.newBuilder().setLoad(Double.NaN))
                .build();
        PeerExchangeResponse withoutInvalidPeer = new PeerExchangeResponse(1, new ArrayList<>(List.of(peer("a"))));
        PeerExchangeResponse withInvalidPeer = PeerExchangeResponse.fromProto(withoutInvalidPeer.toValueProto(false)
                .toBuilder()
                .addPeers(invalid)
                .build());

        assertArrayEquals(withoutInvalidPeer.serializeForHash(), withInvalidPeer.serializeForHash());
    }

    @Test
    void messageWithMorePeersThanTheLimitFailsAtParsing() {
        bisq.network.protobuf.PeerExchangeResponse.Builder builder = bisq.network.protobuf.PeerExchangeResponse
                .newBuilder()
                .setNonce(1);
        bisq.network.protobuf.Peer peer = peer("a").toProto(false);
        for (int i = 0; i <= MAX_NUM_PEERS; i++) {
            builder.addPeers(peer);
        }

        assertThrows(UnresolvableProtobufException.class, () -> PeerExchangeResponse.fromProto(builder.build()));
    }

    private static Peer peer(String hostCharacter) {
        Address address = Address.from(hostCharacter.repeat(56) + ".onion", 9999);
        Capability capability = new Capability(Capability.VERSION,
                address,
                new ArrayList<>(List.of(TransportType.TOR)),
                new ArrayList<>(List.of(Feature.AUTHORIZATION_HASH_CASH_V2)),
                "2.1.14");
        return new Peer(capability, new NetworkLoad(), true, System.currentTimeMillis());
    }
}
