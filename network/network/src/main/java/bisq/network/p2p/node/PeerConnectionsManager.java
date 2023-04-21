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

import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.TorTransport;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.node.transport.socketchannel.ClearNetSocketChannelFactory;
import bisq.network.p2p.node.transport.socketchannel.SocketChannelFactory;
import bisq.network.p2p.node.transport.socketchannel.TorSocketChannelFactory;
import bisq.network.p2p.services.peergroup.BanList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class PeerConnectionsManager {

    private final Node.Config config;
    private final String nodeId;
    private final BanList banList;
    private final AuthorizationService authorizationService;
    private final Transport.Type transportType;
    private final Transport transport;

    private Optional<ServerChannel> server = Optional.empty();
    private Optional<OutboundConnectionMultiplexer> outboundConnectionMultiplexer;

    public PeerConnectionsManager(Node.Config config,
                                  String nodeId,
                                  BanList banList,
                                  AuthorizationService authorizationService,
                                  Transport.Type transportType,
                                  Transport transport) {
        this.config = config;
        this.nodeId = nodeId;
        this.banList = banList;
        this.authorizationService = authorizationService;
        this.transportType = transportType;
        this.transport = transport;
    }

    public CompletableFuture<Void> start(Node node, int port) {
        return createServerAndListenAsync(node, port)
                .thenAccept(myCapability -> createAndStartOutboundConnectionMultiplexer(myCapability, node));
    }

    public void shutdown() {
        server.ifPresent(ServerChannel::shutdown);
        outboundConnectionMultiplexer.ifPresent(OutboundConnectionMultiplexer::shutdown);
    }

    public Optional<Address> findMyAddress() {
        return server.map(ServerChannel::getAddress);
    }

    public CompletableFuture<? extends Connection> getConnection(Address address) {
        if (server.isPresent()) {
            Optional<InboundConnection> connectionOptional = server.get().getConnectionByAddress(address);
            if (connectionOptional.isPresent()) {
                InboundConnection connection = connectionOptional.get();
                return CompletableFuture.completedFuture(connection);
            }
        }

        return outboundConnectionMultiplexer.get().getConnection(address);
    }

    public Stream<Connection> getAllConnections() {
        return Stream.concat(getInboundConnections().stream(), getOutboundConnections().stream());
    }

    public int getNumConnections() {
        return getInboundConnections().size() + getOutboundConnections().size();
    }

    public Collection<InboundConnection> getInboundConnections() {
        return server.isPresent() ? server.get().getAllInboundConnections() : Collections.emptyList();
    }

    public Collection<OutboundConnection> getOutboundConnections() {
        return outboundConnectionMultiplexer.isPresent() ?
                outboundConnectionMultiplexer.get().getAllOutboundConnections() : Collections.emptyList();
    }

    private CompletableFuture<Capability> createServerAndListenAsync(Node node, int port) {
        return transport.getServerSocketChannel(port, nodeId)
                .thenApply(serverSocketChannelResult -> {
                    Capability serverCapability = new Capability(
                            serverSocketChannelResult.getAddress(),
                            new ArrayList<>(config.getSupportedTransportTypes())
                    );
                    ServerChannel serverChannel = new ServerChannel(
                            serverCapability,
                            banList,
                            authorizationService,
                            node,
                            serverSocketChannelResult.getServerSocketChannel()
                    );
                    server = Optional.of(serverChannel);
                    serverChannel.start();

                    return serverCapability;
                });
    }

    private void createAndStartOutboundConnectionMultiplexer(Capability serverCapability, Node node) {
        try {
            OutboundConnectionManager outboundConnectionManager = new OutboundConnectionManager(
                    authorizationService,
                    banList,
                    Load.INITIAL_LOAD,
                    serverCapability,
                    getSocketChannelFactory(),
                    node,
                    SelectorProvider.provider().openSelector()
            );
            OutboundConnectionMultiplexer connectionMultiplexer =
                    new OutboundConnectionMultiplexer(outboundConnectionManager);
            outboundConnectionMultiplexer = Optional.of(connectionMultiplexer);
            connectionMultiplexer.start();
        } catch (IOException e) {
            log.error("Couldn't create OutboundConnectionManager", e);
        }
    }

    private SocketChannelFactory getSocketChannelFactory() {
        switch (transportType) {
            case TOR:
                TorTransport torTransport = (TorTransport) transport;
                int socksProxyPort = torTransport.getSocksPort();
                return new TorSocketChannelFactory(socksProxyPort);
            case CLEAR:
                return new ClearNetSocketChannelFactory();
            default:
                throw new UnsupportedOperationException("Unsupported transportType");
        }
    }
}
