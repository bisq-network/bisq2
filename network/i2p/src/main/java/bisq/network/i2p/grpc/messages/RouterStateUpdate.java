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
import bisq.network.i2p.router.state.RouterState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode
@ToString
public final class RouterStateUpdate implements Proto {
    @Getter
    private final RouterState value;

    public RouterStateUpdate(RouterState value) {
        this.value = value;
    }

    @Override
    public bisq.bi2p.protobuf.RouterStateUpdate completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.bi2p.protobuf.RouterStateUpdate toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.bi2p.protobuf.RouterStateUpdate.Builder getBuilder(boolean serializeForHash) {
        return bisq.bi2p.protobuf.RouterStateUpdate.newBuilder()
                .setValue(value.toProtoEnum());
    }

    public static RouterStateUpdate fromProto(bisq.bi2p.protobuf.RouterStateUpdate proto) {
        return new RouterStateUpdate(RouterState.fromProto(proto.getValue()));
    }
}
