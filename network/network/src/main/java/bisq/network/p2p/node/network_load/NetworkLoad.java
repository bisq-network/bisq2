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

package bisq.network.p2p.node.network_load;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class NetworkLoad implements Proto {
    public static final NetworkLoad INITIAL_NETWORK_LOAD = new NetworkLoad();
    private final int numConnections;

    public NetworkLoad() {
        this.numConnections = 1;
    }

    public NetworkLoad(int numConnections) {
        this.numConnections = numConnections;
    }

    public bisq.network.protobuf.NetworkLoad toProto() {
        return bisq.network.protobuf.NetworkLoad.newBuilder().setNumConnections(numConnections).build();
    }

    public static NetworkLoad fromProto(bisq.network.protobuf.NetworkLoad proto) {
        return new NetworkLoad(proto.getNumConnections());
    }

    public int getFactor() {
        //todo
        return 10;
    }
}