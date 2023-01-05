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

package bisq.socks5_socket_channel;

import bisq.socks5_socket_channel.messages.Socks5ConnectMessage;
import bisq.socks5_socket_channel.messages.Socks5RequestMessage;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class Socks5SocketChannel implements Closeable, ReadableByteChannel, WritableByteChannel {
    private final SocketChannel socketChannel;

    private Socks5SocketChannel() throws IOException {
        socketChannel = SocketChannel.open();
    }

    public static Socks5SocketChannel open() throws IOException {
        return new Socks5SocketChannel();
    }

    public SocketChannel connect(Socks5ConnectionData connectionData, ByteBuffer byteBuffer) throws IOException {
        socketChannel.connect(connectionData.getSocks5ProxySocketAddress());

        negotiateProtocol(byteBuffer);
        connectToDestination(
                byteBuffer,
                connectionData.getDestinationHostName(),
                connectionData.getDestinationPort()
        );

        byteBuffer.clear();
        return socketChannel;
    }

    private void negotiateProtocol(ByteBuffer byteBuffer) throws IOException {
        byteBuffer.clear();
        Socks5ConnectMessage.writeToByteBuffer(byteBuffer);

        byteBuffer.flip();
        write(byteBuffer);

        byteBuffer.clear();
        read(byteBuffer);

        byteBuffer.flip();
        Socks5ConnectMessage.processServerReply(byteBuffer);
    }

    private void connectToDestination(ByteBuffer byteBuffer, String hostName, int port) throws IOException {
        byteBuffer.clear();
        Socks5RequestMessage connectionRequestMessage = new Socks5RequestMessage(hostName, port);
        connectionRequestMessage.writeToByteBuffer(byteBuffer);

        byteBuffer.flip();
        socketChannel.write(byteBuffer);

        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();

        connectionRequestMessage.processServerReply(byteBuffer);
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    @Override
    public boolean isOpen() {
        return socketChannel.isOpen();
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        return socketChannel.read(byteBuffer);
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        return socketChannel.write(byteBuffer);
    }
}
