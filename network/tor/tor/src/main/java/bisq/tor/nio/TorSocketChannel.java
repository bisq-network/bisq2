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

package bisq.tor.nio;

import bisq.socks5_socket_channel.Socks5ConnectionData;
import bisq.socks5_socket_channel.Socks5SocketChannel;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class TorSocketChannel implements Closeable, ReadableByteChannel, WritableByteChannel {
    private final Socks5SocketChannel socks5SocketChannel;

    private TorSocketChannel() throws IOException {
        socks5SocketChannel = Socks5SocketChannel.open();
    }

    public static TorSocketChannel open() throws IOException {
        return new TorSocketChannel();
    }

    public SocketChannel connect(TorSocksConnectionData torSocksConnectionData, ByteBuffer byteBuffer) throws IOException {
        InetSocketAddress torSocketAddress = new InetSocketAddress(
                InetAddress.getLocalHost(),
                torSocksConnectionData.getTorSocksProxyPort()
        );
        Socks5ConnectionData connectionData = new Socks5ConnectionData(
                torSocketAddress,
                torSocksConnectionData.getDestinationHostName(),
                torSocksConnectionData.getDestinationPort()
        );

        SocketChannel socketChannel = socks5SocketChannel.connect(connectionData, byteBuffer);
        byteBuffer.clear();

        return socketChannel;
    }

    @Override
    public void close() throws IOException {
        socks5SocketChannel.close();
    }

    @Override
    public boolean isOpen() {
        return socks5SocketChannel.isOpen();
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        return socks5SocketChannel.read(byteBuffer);
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        return socks5SocketChannel.write(byteBuffer);
    }
}
