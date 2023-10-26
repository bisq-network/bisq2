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

package bisq.network.p2p.services.peergroup;

import bisq.common.proto.Proto;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.common.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.util.Date;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Peer implements Proto, Comparable<Peer> {
    @EqualsAndHashCode.Include
    private final Capability capability;
    private final NetworkLoad networkLoad;
    private final boolean isOutboundConnection;
    private final long created;

    public Peer(Capability capability, NetworkLoad networkLoad, boolean isOutboundConnection) {
        this(capability, networkLoad, isOutboundConnection, System.currentTimeMillis());
    }

    public Peer(Capability capability, NetworkLoad networkLoad, boolean isOutboundConnection, long created) {
        this.capability = capability;
        this.networkLoad = networkLoad;
        this.isOutboundConnection = isOutboundConnection;
        this.created = created;

        NetworkDataValidation.validateDate(created);
    }

    public bisq.network.protobuf.Peer toProto() {
        return bisq.network.protobuf.Peer.newBuilder()
                .setCapability(capability.toProto())
                .setNetworkLoad(networkLoad.toProto())
                .setIsOutboundConnection(isOutboundConnection)
                .setCreated(created)
                .build();
    }

    public static Peer fromProto(bisq.network.protobuf.Peer proto) {
        return new Peer(Capability.fromProto(proto.getCapability()),
                NetworkLoad.fromProto(proto.getNetworkLoad()),
                proto.getIsOutboundConnection(),
                proto.getCreated());
    }

    public Date getDate() {
        return new Date(created);
    }

    public Address getAddress() {
        return capability.getAddress();
    }

    public long getAge() {
        return new Date().getTime() - created;
    }

    // Descending order
    @Override
    public int compareTo(@Nonnull Peer o) {
        return Long.compare(o.getCreated(), created);
    }
}
