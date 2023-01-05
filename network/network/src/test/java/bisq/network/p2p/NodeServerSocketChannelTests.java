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

package bisq.network.p2p;

import bisq.common.util.NetworkUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class NodeServerSocketChannelTests {
    @Test
    void basicTest() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), NetworkUtils.findFreeSystemPort());
        serverSocketChannel.socket().bind(socketAddress);

        NodeServerSocketChannel nodeServerSocketChannel = new NodeServerSocketChannel(serverSocketChannel);
        Thread serverThread = new Thread(() -> nodeServerSocketChannel.start(Throwable::printStackTrace));
        serverThread.start();

        List<SocketChannel> clientConnections = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(socketAddress);
            clientConnections.add(socketChannel);
        }


        int receivedHellos = 0;
        for (SocketChannel socketChannel : clientConnections) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            socketChannel.read(byteBuffer);
            byteBuffer.flip();

            byte b = byteBuffer.get();
            if ((int) b == 55) {
                receivedHellos++;
            } else {
                fail("Received " + b + " instead of " + 55);
            }
        }

        assertThat(receivedHellos).isEqualTo(5);

        nodeServerSocketChannel.shutdown();
        for (SocketChannel clientConnection : clientConnections) {
            clientConnection.close();
        }
    }
}
