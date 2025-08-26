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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode
@ToString
public class NetworkStateUpdate implements Proto {
    @Getter
    private final NetworkState value;

    public NetworkStateUpdate(NetworkState value) {
        this.value = value;
    }

    @Override
    public bisq.i2p.protobuf.NetworkStateUpdate completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.i2p.protobuf.NetworkStateUpdate toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    @Override
    public bisq.i2p.protobuf.NetworkStateUpdate.Builder getBuilder(boolean serializeForHash) {
        return bisq.i2p.protobuf.NetworkStateUpdate.newBuilder()
                .setValue(value.toProtoEnum());
    }

    public static NetworkStateUpdate fromProto(bisq.i2p.protobuf.NetworkStateUpdate proto) {
        return new NetworkStateUpdate(NetworkState.fromProto(proto.getValue()));
    }
}
