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

import bisq.common.data.Pair;
import bisq.common.network.Address;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.handshake.ConnectionHandshakeResponder;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.services.peer_group.BanList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class InboundConnectionsManager {

    private final BanList banList;
    private final Capability myCapability;
    private final NetworkLoad myNetworkLoad;
    private final AuthorizationService authorizationService;

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Node node;
    private final List<SocketChannel> inboundHandshakeChannels = new CopyOnWriteArrayList<>();
    private final List<SocketChannel> verifiedConnections = new CopyOnWriteArrayList<>();
    private final Map<SocketChannel, InboundConnectionChannel> connectionByChannel = new ConcurrentHashMap<>();
    private final Map<Address, InboundConnectionChannel> connectionByAddress = new ConcurrentHashMap<>();
    private final Map<SocketChannel, NetworkEnvelopeSocketChannel> networkEnvelopeChannelBySocketChannel = new ConcurrentHashMap<>();

    public InboundConnectionsManager(BanList banList,
                                     Capability myCapability,
                                     NetworkLoad myNetworkLoad,
                                     AuthorizationService authorizationService,
                                     ServerSocketChannel serverSocketChannel,
                                     Selector selector,
                                     Node node) {
        this.banList = banList;
        this.myCapability = myCapability;
        this.myNetworkLoad = myNetworkLoad;
        this.authorizationService = authorizationService;
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
        this.node = node;
    }

    public void registerOpAccept() {
        try {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            log.error("Couldn't register OP_ACCEPT for serverSocketChannel.", e);
        } catch (IOException e) {
            log.error("Couldn't set serverSocketChannel to non-blocking mode.", e);
        }
    }

    public void acceptNewConnection(SelectionKey selectionKey) {
        SocketChannel newConnectionSocketChannel = null;
        try (ServerSocketChannel nextReadySocketChannel = (ServerSocketChannel) selectionKey.channel()) {
            newConnectionSocketChannel = nextReadySocketChannel.accept();
            log.info("Accepted new inbound connection with peer: {}", newConnectionSocketChannel.getRemoteAddress());

            newConnectionSocketChannel.configureBlocking(false);
            newConnectionSocketChannel.register(selector, SelectionKey.OP_READ);

            inboundHandshakeChannels.add(newConnectionSocketChannel);
        } catch (IOException e) {
            log.warn("Cannot accept new connection", e);
            try {
                if (newConnectionSocketChannel != null) {
                    newConnectionSocketChannel.close();
                }
            } catch (IOException exception) {
                // ignored
            }
        }
    }

    public void handleInboundConnection(SocketChannel socketChannel) {
        if (inboundHandshakeChannels.contains(socketChannel)) {
            NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(socketChannel);
            networkEnvelopeChannelBySocketChannel.put(socketChannel, networkEnvelopeSocketChannel);

            log.debug("Inbound handshake request at: {}", myCapability.getAddress());
            Optional<InboundConnectionChannel> inboundConnectionOptional = performHandshake(networkEnvelopeSocketChannel);

            if (inboundConnectionOptional.isPresent()) {
                InboundConnectionChannel inboundConnection = inboundConnectionOptional.get();
                Address peerAddress = inboundConnection.getPeerAddress();
                log.debug("Inbound handshake completed: Initiated by {} to {}",
                        peerAddress, myCapability.getAddress());

                if (isAlreadyConnectedToPeer(peerAddress)) {
                    log.warn("{} have already an InboundConnection from {}. This can happen when a " + "handshake was in progress while we received a new connection from that address. " + "We will close the socket of that new connection and use the existing instead.", this, peerAddress);
                    closeChannel(networkEnvelopeSocketChannel);
                } else {
                    connectionByChannel.put(socketChannel, inboundConnection);
                    connectionByAddress.put(peerAddress, inboundConnection);
                    verifiedConnections.add(socketChannel);

                    log.info("Calling node.onNewIncomingConnection for peer {}", peerAddress.getFullAddress());
                    node.onNewIncomingConnection(inboundConnection);
                }

            } else {
                closeChannel(networkEnvelopeSocketChannel);
            }

            inboundHandshakeChannels.remove(socketChannel);

        } else if (verifiedConnections.contains(socketChannel)) {
            InboundConnectionChannel inboundConnection = connectionByChannel.get(socketChannel);
            Address peerAddress = inboundConnection.getPeerAddress();

            NetworkEnvelopeSocketChannel networkEnvelopeChannel = networkEnvelopeChannelBySocketChannel.get(socketChannel);
            try {
                List<NetworkEnvelope> networkEnvelopes = networkEnvelopeChannel.receiveNetworkEnvelopes();
                log.debug("Received {} messages from peer {}.", networkEnvelopes.size(), peerAddress.getFullAddress());

                networkEnvelopes.forEach(networkEnvelope -> node.handleNetworkMessage(
                        networkEnvelope.getEnvelopePayloadMessage(),
                        networkEnvelope.getAuthorizationToken(),
                        inboundConnection
                ));
            } catch (IOException e) {
                log.error("Couldn't receive messages from socketChannel", e);
            }
        }
    }

    public boolean isInboundConnection(SocketChannel socketChannel) {
        return inboundHandshakeChannels.contains(socketChannel) || verifiedConnections.contains(socketChannel);
    }

    public Optional<InboundConnectionChannel> getConnectionByAddress(Address address) {
        InboundConnectionChannel inboundConnection = connectionByAddress.get(address);
        return Optional.ofNullable(inboundConnection);
    }

    public Collection<InboundConnectionChannel> getAllInboundConnections() {
        return connectionByAddress.values();
    }

    private Optional<InboundConnectionChannel> performHandshake(NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel) {
        try {
            var handshakeResponder = new ConnectionHandshakeResponder(
                    banList,
                    myCapability,
                    myNetworkLoad,
                    authorizationService,
                    networkEnvelopeSocketChannel
            );
            Pair<ConnectionHandshake.Request, NetworkEnvelope>
                    requestAndResponseNetworkEnvelopes = handshakeResponder.verifyAndBuildRespond();

            ConnectionHandshake.Request handshakeRequest = requestAndResponseNetworkEnvelopes.getFirst();
            Address peerAddress = handshakeRequest.getCapability().getAddress();

            log.debug("Sending PoW response to peer {}", peerAddress.getFullAddress());
            bisq.network.p2p.message.NetworkEnvelope responseEnvelope = requestAndResponseNetworkEnvelopes.getSecond();
            try {
                networkEnvelopeSocketChannel.send(responseEnvelope);
            } catch (IOException e) {
                log.warn("Couldn't send PoW response to peer {}", peerAddress.getFullAddress(), e);
                throw e;
            }

            // We got the peers network load passed in the request message.
            NetworkLoadSnapshot peersNetworkLoadSnapshot = new NetworkLoadSnapshot(handshakeRequest.getNetworkLoad());
            return Optional.of(
                    new InboundConnectionChannel(
                            handshakeRequest.getCapability(),
                            peersNetworkLoadSnapshot,
                            networkEnvelopeSocketChannel,
                            new ConnectionMetrics()
                    )
            );
        } catch (ConnectionException e) {
            log.warn("Peer failed PoW challenge.", e);
        } catch (IOException e) {
            log.warn("Handshake failed with peer: ", e);
        }

        return Optional.empty();
    }

    private boolean isAlreadyConnectedToPeer(Address peerAddress) {
        return connectionByAddress.containsKey(peerAddress);
    }

    private void closeChannel(NetworkEnvelopeSocketChannel networkEnvelopeSocket) {
        try {
            networkEnvelopeSocket.close();
        } catch (IOException e) {
            // ignored
        }
    }
}
