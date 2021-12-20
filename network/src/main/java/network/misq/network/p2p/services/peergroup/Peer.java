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

package network.misq.network.p2p.services.peergroup;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Capability;
import network.misq.network.p2p.node.Load;

import java.io.Serializable;
import java.util.Date;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Peer implements Serializable {
    @EqualsAndHashCode.Include
    private final Capability capability;
    private final Load load;
    private final boolean isOutboundConnection;
    private final long created;

    public Peer(Capability capability, Load load, boolean isOutboundConnection) {
        this.capability = capability;
        this.load = load;
        this.isOutboundConnection = isOutboundConnection;
        this.created = System.currentTimeMillis();
    }

    public Date getDate() {
        return new Date(created);
    }

    public Address getAddress() {
        return capability.address();
    }

    public long getAge() {
        return new Date().getTime() - created;
    }
}
