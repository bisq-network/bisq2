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

import bisq.common.data.ByteArray;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.peergroup.Peer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class PeerExchangeResponse implements NetworkMessage {
    private final int nonce;
    private final List<Peer> peers;

    public PeerExchangeResponse(int nonce, List<Peer> peers) {
        this.nonce = nonce;
        this.peers = peers;
        // We need to sort deterministically as the data is used in the proof of work check
        this.peers.sort(Comparator.comparing((Peer e) -> new ByteArray(e.serialize())));
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder().setPeerExchangeResponse(
                        bisq.network.protobuf.PeerExchangeResponse.newBuilder()
                                .setNonce(nonce)
                                .addAllPeers(peers.stream()
                                        .map(Peer::toProto)
                                        .collect(Collectors.toList())))
                .build();
    }

    public static PeerExchangeResponse fromProto(bisq.network.protobuf.PeerExchangeResponse proto) {
        return new PeerExchangeResponse(proto.getNonce(),
                proto.getPeersList().stream().map(Peer::fromProto).collect(Collectors.toList()));
    }
}