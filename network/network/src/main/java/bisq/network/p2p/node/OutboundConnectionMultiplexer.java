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

import bisq.common.threading.ThreadName;
import bisq.network.common.Address;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OutboundConnectionMultiplexer implements OutboundConnectionManager.Listener {

    private final Selector selector;
    private final OutboundConnectionManager outboundConnectionManager;

    private Optional<Thread> workerThread = Optional.empty();


    public OutboundConnectionMultiplexer(OutboundConnectionManager outboundConnectionManager) {
        this.selector = outboundConnectionManager.getSelector();
        this.outboundConnectionManager = outboundConnectionManager;
    }

    public void start() {
        outboundConnectionManager.registerListener(this);

        var thread = new Thread(() -> {
            ThreadName.set(this, "workerLoop");
            workerLoop();
        });
        workerThread = Optional.of(thread);
        thread.start();
    }

    public void shutdown() {
        workerThread.ifPresent(Thread::interrupt);
    }

    public CompletableFuture<OutboundConnectionChannel> getConnection(Address address) {
        Optional<OutboundConnectionChannel> optionalConnectionChannel =
                outboundConnectionManager.getConnection(address);

        if (optionalConnectionChannel.isPresent()) {
            return CompletableFuture.completedFuture(
                    optionalConnectionChannel.get()
            );
        }

        CompletableFuture<OutboundConnectionChannel> completableFuture = outboundConnectionManager.createNewConnection(address);
        selector.wakeup();

        return completableFuture;
    }

    @Override
    public void onNewConnection(OutboundConnectionChannel outboundConnectionChannel) {
    }

    public Collection<OutboundConnectionChannel> getAllOutboundConnections() {
        return outboundConnectionManager.getAllOutboundConnections();
    }

    private void workerLoop() {
        while (true) {
            selectorLoop();
        }
    }

    private void selectorLoop() {
        try {
            while (selector.select() > 0) {
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = readyKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();

                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                    if (selectionKey.isConnectable()) {
                        outboundConnectionManager.handleConnectableChannel(socketChannel);
                    }

                    if (selectionKey.isReadable()) {
                        outboundConnectionManager.handleReadableChannel(socketChannel);
                    }

                    if (selectionKey.isWritable()) {
                        outboundConnectionManager.handleWritableChannel(socketChannel);
                    }

                }
            }
        } catch (IOException e) {
            log.warn("IOException in OutboundConnectionMultiplexer selector.", e);
        } catch (CancelledKeyException e) {
            // Connection attempt failed. Nothing we can do here.
        }
    }
}
