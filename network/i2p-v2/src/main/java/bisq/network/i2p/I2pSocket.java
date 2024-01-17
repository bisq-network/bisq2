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

package bisq.network.i2p;

import bisq.network.common.PeerSocket;
import lombok.Getter;
import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class I2pSocket implements PeerSocket {
    private final I2PSocket socket;
    @Getter
    private final InputStream inputStream;
    @Getter
    private final OutputStream outputStream;

    public I2pSocket(I2PSocket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
