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

package network.misq.network.p2p.node;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.node.transport.Transport;

import java.net.Socket;

@Slf4j
public class InboundConnection extends Connection {
    @Getter
    private final Transport.ServerSocketResult serverSocketResult;

    @Setter
    private boolean isPeerAddressVerified;

    InboundConnection(Socket socket,
                      Transport.ServerSocketResult serverSocketResult,
                      Capability peersCapability,
                      Load peersLoad,
                      Handler handler) {
        super(socket, peersCapability, peersLoad, handler);
        this.serverSocketResult = serverSocketResult;
        log.debug("Create inboundConnection from server: {}", serverSocketResult);
    }

    @Override
    public boolean isPeerAddressVerified() {
        return isPeerAddressVerified;
    }
}
