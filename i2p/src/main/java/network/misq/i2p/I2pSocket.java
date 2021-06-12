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

package network.misq.i2p;

import lombok.Setter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class I2pSocket extends Socket {
    @Setter
    @Nullable
    private String peerDestination;

    public I2pSocket(String host, int port) throws IOException {
        super(host, port);
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        if (peerDestination == null) {
            throw new IllegalStateException("The remote peer address is only available after an inbound connection is established.");
        }
        return new I2PSocketAddress(peerDestination);
    }
}
