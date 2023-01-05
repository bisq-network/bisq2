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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class NodeServerSocketChannel {
    private final ServerSocketChannel serverSocketChannel;
    private final AtomicBoolean isStopped = new AtomicBoolean();

    private final List<SocketChannel> activeConnections = new ArrayList<>();

    private Selector acceptSelector;

    NodeServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    void start(Consumer<Exception> exceptionHandler) {
        log.debug("Create server: {}", serverSocketChannel);
        try {
            acceptSelector = SelectorProvider.provider().openSelector();
            serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

            while (acceptSelector.select() > 0) {
                Set<SelectionKey> readyKeys = acceptSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = readyKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();

                    ServerSocketChannel nextReadySocketChannel = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel newConnectionSocketChannel = nextReadySocketChannel.accept();
                    log.debug("Accepted new connection on server: {}", serverSocketChannel);

                    activeConnections.add(newConnectionSocketChannel);

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    byteBuffer.put((byte) 55);

                    byteBuffer.flip();
                    newConnectionSocketChannel.write(byteBuffer);

                    // TODO: Initiate handshake
                    // TODO: Process Reads
                }
            }

        } catch (IOException e) {
            if (!isStopped.get()) {
                exceptionHandler.accept(e);
                shutdown();
            }
        } catch (ClosedSelectorException e) {
            shutdown();
        } finally {
            closeAllConnections();
        }
    }

    void shutdown() {
        log.info("shutdown {}", serverSocketChannel);
        if (isStopped.get()) {
            return;
        }
        isStopped.set(true);

        try {
            acceptSelector.close();
            closeActiveConnections();
        } catch (IOException ignore) {
        }
    }

    private void closeAllConnections() {
        try {
            closeActiveConnections();
            serverSocketChannel.close();
        } catch (IOException e) {
            log.error("Couldn't close server socket " + serverSocketChannel);
        }
    }

    private void closeActiveConnections() throws IOException {
        for (SocketChannel activeConnection : activeConnections) {
            try {
                activeConnection.close();
            } catch (IOException e) {
                log.error("Couldn't close client socket " + activeConnection);
            }
        }
    }
}
