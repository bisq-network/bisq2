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

package bisq.network.i2p.router.state;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum NetworkState implements ProtoEnum {
    NEW,
    UNKNOWN_SIGNAL,     // no reliable signal yet
    TESTING,     // bootstrapping / mixed "Testing"
    OK,          // at least one family (v4 or v6) confirmed OK
    FIREWALLED,  // reachable only in limited ways (no confirmed OK)
    DISCONNECTED; // no connectivity


    @Override
    public bisq.bi2p.protobuf.NetworkState toProtoEnum() {
        return bisq.bi2p.protobuf.NetworkState.valueOf(getProtobufEnumPrefix() + name());
    }

    public static NetworkState fromProto(bisq.bi2p.protobuf.NetworkState proto) {
        return ProtobufUtils.enumFromProto(NetworkState.class, proto.name(), NEW);
    }
}
