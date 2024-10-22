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
import bisq.common.network.Address;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

@Slf4j
public class ServerChannel {

    public interface Handler {
        void onServerReady();
    }

    private final Capability myCapability;
    private final NetworkLoad myNetworkLoad;
    private final BanList banList;
    private final AuthorizationService authorizationService;
    private final Node node;

    private final ServerSocketChannel serverSocketChannel;

    private Thread serverThread;
    private Optional<InboundConnectionsManager> inboundConnectionsManager = Optional.empty();

    @Setter
    private Optional<Handler> onServerReadyListener = Optional.empty();

    public ServerChannel(Capability myCapability,
                         NetworkLoad myNetworkLoad,
                         BanList banList,
                         AuthorizationService authorizationService,
                         Node node,
                         ServerSocketChannel serverSocketChannel) {
        this.myCapability = myCapability;
        this.myNetworkLoad = myNetworkLoad;
        this.banList = banList;
        this.authorizationService = authorizationService;
        this.node = node;
        this.serverSocketChannel = serverSocketChannel;
    }

    public void start() {
        Address myAddress = myCapability.getAddress();
        log.debug("Create server: {}", myAddress);

        serverThread = new Thread(() -> {
            ThreadName.set(this, "start");
            try {
                InetSocketAddress socketAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(),
                        myAddress.getPort()
                );
                serverSocketChannel.socket().bind(socketAddress);

                Selector selector = SelectorProvider.provider().openSelector();
                InboundConnectionsManager inboundConnectionsManager =
                        new InboundConnectionsManager(
                                banList,
                                myCapability,
                                myNetworkLoad,
                                authorizationService,
                                serverSocketChannel,
                                selector,
                                node
                        );
                this.inboundConnectionsManager = Optional.of(inboundConnectionsManager);

                inboundConnectionsManager.registerOpAccept();
                onServerReadyListener.ifPresent(Handler::onServerReady);

                while (selector.select() > 0) {
                    if (isServerStopped()) {
                        return;
                    }

                    Set<SelectionKey> readyKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = readyKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();
                        keyIterator.remove();

                        if (selectionKey.isAcceptable()) {
                            inboundConnectionsManager.acceptNewConnection(selectionKey);
                        }

                        if (selectionKey.isReadable()) {
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            log.info("Received message from {}", socketChannel.getRemoteAddress());

                            if (inboundConnectionsManager.isInboundConnection(socketChannel)) {
                                inboundConnectionsManager.handleInboundConnection(socketChannel);
                            }
                        }
                    }
                }

            } catch (IOException e) {
                if (!isServerStopped()) {
                    log.error("Unhandled exception in ServerChannel: ", e);
                    shutdown();
                }
            }

        }, "Server.listen-" + myAddress);

        serverThread.start();
    }

    public void shutdown() {
        try {
            Address myAddress = myCapability.getAddress();
            log.info("shutdown {}", myAddress);

            if (!isServerStopped()) {
                serverThread.interrupt();
                serverSocketChannel.close();
            }

        } catch (IOException ignore) {
        }
    }

    private boolean isServerStopped() {
        return serverThread.isInterrupted();
    }

    public Address getAddress() {
        return myCapability.getAddress();
    }

    public Optional<InboundConnectionChannel> getConnectionByAddress(Address address) {
        if (inboundConnectionsManager.isPresent()) {
            return inboundConnectionsManager.get().getConnectionByAddress(address);
        }
        return Optional.empty();
    }

    public Collection<InboundConnectionChannel> getAllInboundConnections() {
        if (inboundConnectionsManager.isPresent()) {
            return inboundConnectionsManager.get().getAllInboundConnections();
        }
        return Collections.emptyList();
    }
}
