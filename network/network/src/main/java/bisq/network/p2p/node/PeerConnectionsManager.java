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

import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.node.transport.ServerSocketResult;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peer_group.BanList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
public class PeerConnectionsManager {

    private final Node.Config config;
    private final NetworkId networkId;
    private final BanList banList;
    private final NetworkLoad myNetworkLoad;
    private final AuthorizationService authorizationService;
    private final TransportService transportService;

    private Optional<ServerChannel> server = Optional.empty();
    private Optional<OutboundConnectionMultiplexer> outboundConnectionMultiplexer;

    public PeerConnectionsManager(Node.Config config,
                                  NetworkId networkId,
                                  BanList banList,
                                  NetworkLoad myNetworkLoad,
                                  AuthorizationService authorizationService,
                                  TransportService transportService) {
        this.config = config;
        this.networkId = networkId;
        this.banList = banList;
        this.myNetworkLoad = myNetworkLoad;
        this.authorizationService = authorizationService;
        this.transportService = transportService;
    }

    public void start(Node node) {
        try {
            Capability myCapability = createServerAndListen(node);
            createAndStartOutboundConnectionMultiplexer(myCapability, node);
        } catch (IOException e) {
            log.error("Couldn't start PeerConnectionsManager", e);
        }
    }

    public void shutdown() {
        server.ifPresent(ServerChannel::shutdown);
        outboundConnectionMultiplexer.ifPresent(OutboundConnectionMultiplexer::shutdown);
    }

    public Optional<Address> findMyAddress() {
        return server.map(ServerChannel::getAddress);
    }

    public ConnectionChannel getConnection(Address address) {
        if (server.isPresent()) {
            Optional<InboundConnectionChannel> connectionOptional = server.get().getConnectionByAddress(address);
            if (connectionOptional.isPresent()) {
                return connectionOptional.get();
            }
        }

        if (outboundConnectionMultiplexer.isPresent()) {
            CompletableFuture<OutboundConnectionChannel> connection = outboundConnectionMultiplexer.get().getConnection(address);
            try {
                return connection.get(2, MINUTES);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("Couldn't connect to {}", address);
            }
        }

        return null;
    }

    public Stream<ConnectionChannel> getAllConnections() {
        return Stream.concat(getInboundConnections().stream(), getOutboundConnections().stream());
    }

    public int getNumConnections() {
        return getInboundConnections().size() + getOutboundConnections().size();
    }

    public Collection<InboundConnectionChannel> getInboundConnections() {
        return server.isPresent() ? server.get().getAllInboundConnections() : Collections.emptyList();
    }

    public Collection<OutboundConnectionChannel> getOutboundConnections() {
        return outboundConnectionMultiplexer.isPresent() ?
                outboundConnectionMultiplexer.get().getAllOutboundConnections() : Collections.emptyList();
    }

    private Capability createServerAndListen(Node node) throws IOException {
        ServerSocketResult serverSocketResult = transportService.getServerSocket(networkId, node.getKeyBundle());
        List<TransportType> supportedTransportTypes = new ArrayList<>(config.getSupportedTransportTypes());
        List<Feature> features = new ArrayList<>(config.getFeatures());
        Capability serverCapability = Capability.myCapability(serverSocketResult.getAddress(), supportedTransportTypes, features);
        ServerChannel serverChannel = new ServerChannel(
                serverCapability,
                myNetworkLoad,
                banList,
                authorizationService,
                node,
                ServerSocketChannel.open()
        );
        server = Optional.of(serverChannel);
        serverChannel.start();

        return serverCapability;
    }

    private void createAndStartOutboundConnectionMultiplexer(Capability serverCapability, Node node) {
        try {
            OutboundConnectionManager outboundConnectionManager = new OutboundConnectionManager(
                    authorizationService,
                    banList,
                    myNetworkLoad,
                    serverCapability,
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
}
