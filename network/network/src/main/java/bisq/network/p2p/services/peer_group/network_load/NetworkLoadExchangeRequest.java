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

package bisq.network.p2p.services.peer_group.network_load;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.node.network_load.NetworkLoad;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class NetworkLoadExchangeRequest implements EnvelopePayloadMessage, Request {
    private final int nonce;
    private final NetworkLoad networkLoad;

    public NetworkLoadExchangeRequest(int nonce, NetworkLoad networkLoad) {
        this.nonce = nonce;
        this.networkLoad = networkLoad;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setNetworkLoadExchangeRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.NetworkLoadExchangeRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.NetworkLoadExchangeRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.NetworkLoadExchangeRequest.newBuilder()
                .setNonce(nonce)
                .setNetworkLoad(networkLoad.toProto(serializeForHash));
    }

    public static NetworkLoadExchangeRequest fromProto(bisq.network.protobuf.NetworkLoadExchangeRequest proto) {
        return new NetworkLoadExchangeRequest(proto.getNonce(), NetworkLoad.fromProto(proto.getNetworkLoad()));
    }

    @Override
    public double getCostFactor() {
        return 0.05;
    }

    @Override
    public String getRequestId() {
        return String.valueOf(nonce);
    }
}