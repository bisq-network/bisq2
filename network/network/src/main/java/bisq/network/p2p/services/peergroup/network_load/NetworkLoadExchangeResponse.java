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

package bisq.network.p2p.services.peergroup.network_load;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.network_load.NetworkLoad;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class NetworkLoadExchangeResponse implements EnvelopePayloadMessage {
    private final int requestNonce;
    private final NetworkLoad networkLoad;

    public NetworkLoadExchangeResponse(int requestNonce, NetworkLoad networkLoad) {
        this.requestNonce = requestNonce;
        this.networkLoad = networkLoad;
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder().setNetworkLoadExchangeResponse(
                        bisq.network.protobuf.NetworkLoadExchangeResponse.newBuilder()
                                .setRequestNonce(requestNonce)
                                .setNetworkLoad(networkLoad.toProto()))
                .build();
    }

    public static NetworkLoadExchangeResponse fromProto(bisq.network.protobuf.NetworkLoadExchangeResponse proto) {
        return new NetworkLoadExchangeResponse(proto.getRequestNonce(), NetworkLoad.fromProto(proto.getNetworkLoad()));
    }
}