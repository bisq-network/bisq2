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

package bisq.network.p2p.node;

import bisq.network.common.Address;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.Socket;
import java.util.function.BiConsumer;

@Slf4j
public class OutboundConnection extends Connection {

    @Getter
    private final Address address;

    OutboundConnection(Socket socket,
                       Address address,
                       Capability peersCapability,
                       NetworkLoadSnapshot peersNetworkLoadSnapshot,
                       ConnectionMetrics connectionMetrics,
                       ConnectionThrottle connectionThrottle,
                       Handler handler,
                       BiConsumer<Connection, Exception> errorHandler) {
        super(socket,
                peersCapability,
                peersNetworkLoadSnapshot,
                connectionMetrics,
                connectionThrottle,
                handler,
                errorHandler);

        this.address = address;
        log.debug("Create outboundConnection to {}", address);
    }

    /**
     * @return Peer address used when connecting to the peer, NOT the address reported by the peer. This matters when
     * connecting to a clearnet seed, because the reported seed address will always be 127.0.0.1.
     */
    @Override
    public Address getPeerAddress() {
        return address;
    }
}
