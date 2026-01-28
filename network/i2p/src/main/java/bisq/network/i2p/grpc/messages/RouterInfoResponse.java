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

package bisq.network.i2p.grpc.messages;

import bisq.common.proto.Proto;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterState;
import bisq.network.i2p.router.state.TunnelInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class RouterInfoResponse implements Proto {

    private final ProcessState processState;
    private final NetworkState networkState;
    private final RouterState routerState;
    private final TunnelInfo tunnelInfo;

    public RouterInfoResponse(ProcessState processState,
                              NetworkState networkState,
                              RouterState routerState,
                              TunnelInfo tunnelInfo) {
        this.processState = processState;
        this.networkState = networkState;
        this.routerState = routerState;
        this.tunnelInfo = tunnelInfo;
    }

    @Override
    public bisq.bi2p.protobuf.RouterInfoResponse completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.bi2p.protobuf.RouterInfoResponse toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.bi2p.protobuf.RouterInfoResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.bi2p.protobuf.RouterInfoResponse.newBuilder()
                .setProcessState(processState.toProtoEnum())
                .setNetworkState(networkState.toProtoEnum())
                .setRouterState(routerState.toProtoEnum())
                .setTunnelInfo(tunnelInfo.toProto(serializeForHash));
    }

    public static RouterInfoResponse fromProto(bisq.bi2p.protobuf.RouterInfoResponse proto) {
        return new RouterInfoResponse(
                ProcessState.fromProto(proto.getProcessState()),
                NetworkState.fromProto(proto.getNetworkState()),
                RouterState.fromProto(proto.getRouterState()),
                TunnelInfo.fromProto(proto.getTunnelInfo())
        );
    }
}
