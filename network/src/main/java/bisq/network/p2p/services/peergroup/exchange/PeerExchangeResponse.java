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

package bisq.network.p2p.services.peergroup.exchange;

import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.peergroup.Peer;

import java.util.Set;
import java.util.stream.Collectors;

record PeerExchangeResponse(int nonce, Set<Peer> peers) implements NetworkMessage {
    @Override
    public bisq.network.protobuf.NetworkMessage toNetworkMessageProto() {
        return getNetworkMessageBuilder().setPeerExchangeResponse(
                        bisq.network.protobuf.PeerExchangeResponse.newBuilder()
                                .setNonce(nonce)
                                .addAllPeers(peers.stream().map(Peer::toProto).collect(Collectors.toSet())))
                .build();
    }

    public static PeerExchangeResponse fromProto(bisq.network.protobuf.PeerExchangeResponse proto) {
        return new PeerExchangeResponse(proto.getNonce(),
                proto.getPeersList().stream().map(Peer::fromProto).collect(Collectors.toSet()));
    }
}