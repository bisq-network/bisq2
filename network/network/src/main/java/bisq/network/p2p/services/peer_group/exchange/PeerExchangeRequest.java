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

import bisq.common.annotation.ExcludeForHash;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.services.peer_group.Peer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class PeerExchangeRequest implements EnvelopePayloadMessage, Request {
    @Setter
    public static long maxNumPeers;
    private final int nonce;
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final List<Peer> peers;

    public PeerExchangeRequest(int nonce, List<Peer> peers) {
        this.nonce = nonce;
        this.peers = peers;
        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.peers);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(peers.size() <= maxNumPeers);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setPeerExchangeRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.PeerExchangeRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.PeerExchangeRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.PeerExchangeRequest.newBuilder()
                .setNonce(nonce)
                .addAllPeers(peers.stream()
                        .map(peer -> peer.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    public static PeerExchangeRequest fromProto(bisq.network.protobuf.PeerExchangeRequest proto) {
        return new PeerExchangeRequest(proto.getNonce(),
                proto.getPeersList().stream().map(Peer::fromProto).collect(Collectors.toList()));
    }

    @Override
    public double getCostFactor() {
        return 0.1;
    }

    @Override
    public String getRequestId() {
        return String.valueOf(nonce);
    }
}