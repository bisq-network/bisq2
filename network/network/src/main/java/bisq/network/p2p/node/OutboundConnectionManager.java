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

import bisq.common.network.Address;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.envelope.parser.nio.ProtoBufMessageLengthWriter;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.handshake.ConnectionHandshakeInitiator;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.tor.TorAddressOwnershipProofGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OutboundConnectionManager {
    public interface Listener {
        void onNewConnection(OutboundConnectionChannel outboundConnectionChannel);
    }

    private final AuthorizationService authorizationService;
    private final BanList banList;
    private final NetworkLoad myNetworkLoad;
    private final Capability myCapability;
    private final Node node;
    @Getter
    private final Selector selector;

    private final Map<SocketChannel, Address> addressByChannel = new ConcurrentHashMap<>();
    private final Map<Address, SocketChannel> channelByAddress = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ConnectionHandshakeInitiator> handshakeInitiatorByChannel = new ConcurrentHashMap<>();

    private final List<SocketChannel> outboundHandshakeChannels = new CopyOnWriteArrayList<>();
    private final List<SocketChannel> verifiedConnections = new CopyOnWriteArrayList<>();

    private final Map<SocketChannel, OutboundConnectionChannel> connectionByChannel = new ConcurrentHashMap<>();
    private final Map<Address, CompletableFuture<OutboundConnectionChannel>> completableFutureByPeerAddress = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public OutboundConnectionManager(AuthorizationService authorizationService,
                                     BanList banList,
                                     NetworkLoad myNetworkLoad,
                                     Capability myCapability,
                                     Node node,
                                     Selector selector) {
        this.authorizationService = authorizationService;
        this.banList = banList;
        this.myNetworkLoad = myNetworkLoad;
        this.myCapability = myCapability;
        this.node = node;
        this.selector = selector;
    }

    public CompletableFuture<OutboundConnectionChannel> createNewConnection(Address address) {
        if (completableFutureByPeerAddress.containsKey(address)) {
            return completableFutureByPeerAddress.get(address);
        }

        var completableFuture = new CompletableFuture<OutboundConnectionChannel>();
        completableFutureByPeerAddress.put(address, completableFuture);

        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            addressByChannel.put(socketChannel, address);
            channelByAddress.put(address, socketChannel);

            InetSocketAddress socketAddress = new InetSocketAddress(address.getHost(), address.getPort());
            boolean isConnectedImmediately = socketChannel.connect(socketAddress);
            if (isConnectedImmediately) {
                handleConnectedChannel(socketChannel);
            }

        } catch (IOException e) {
            log.warn("Couldn't create connection to {}", address.getFullAddress(), e);
        }

        return completableFuture;
    }

    public void handleConnectableChannel(SocketChannel socketChannel) throws IOException {
        try {
            socketChannel.finishConnect();
            handleConnectedChannel(socketChannel);

        } catch (ConnectException e) {
            // Couldn't connect to peer, nothing we can do.
            Address address = addressByChannel.get(socketChannel);
            //noinspection resource
            channelByAddress.remove(address);

            addressByChannel.remove(socketChannel);
        }
    }

    public void handleWritableChannel(SocketChannel socketChannel) throws IOException {
        if (outboundHandshakeChannels.contains(socketChannel)) {
            var handshakeInitiator = new ConnectionHandshakeInitiator(
                    myCapability,
                    authorizationService,
                    banList,
                    myNetworkLoad,
                    addressByChannel.get(socketChannel),
                    new TorAddressOwnershipProofGenerator(null));
            handshakeInitiatorByChannel.put(socketChannel, handshakeInitiator);

            NetworkEnvelope handshakeRequest = handshakeInitiator.initiate();
            ByteBuffer byteBuffer = wrapPayloadInByteBuffer(handshakeRequest);

            socketChannel.register(selector, SelectionKey.OP_READ);

            log.info("Sending PoW request to peer.");
            socketChannel.write(byteBuffer);
        }
    }

    public void handleReadableChannel(SocketChannel socketChannel) throws IOException {
        if (outboundHandshakeChannels.contains(socketChannel)) {
            NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(socketChannel);
            List<NetworkEnvelope> networkEnvelopeList = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

            ConnectionHandshakeInitiator handshakeInitiator = handshakeInitiatorByChannel.get(socketChannel);
            ConnectionHandshake.Response handshakeResponse = handshakeInitiator.finish(networkEnvelopeList);

            verifiedConnections.add(socketChannel);
            outboundHandshakeChannels.remove(socketChannel);

            Capability peerCapability = handshakeResponse.getCapability();
            // We got the peers network load passed in the response message.
            NetworkLoadSnapshot peersNetworkLoadSnapshot = new NetworkLoadSnapshot(handshakeResponse.getNetworkLoad());
            OutboundConnectionChannel outboundConnectionChannel = new OutboundConnectionChannel(
                    peerCapability,
                    peersNetworkLoadSnapshot,
                    networkEnvelopeSocketChannel,
                    new ConnectionMetrics()
            );

            connectionByChannel.put(socketChannel, outboundConnectionChannel);
            listeners.forEach(listener -> {
                try {
                    listener.onNewConnection(outboundConnectionChannel);
                } catch (Exception e) {
                    log.error("Calling onNewConnection at listener {} failed", listener, e);
                }
            });

            CompletableFuture<OutboundConnectionChannel> completableFuture =
                    completableFutureByPeerAddress.get(peerCapability.getAddress());
            completableFuture.complete(outboundConnectionChannel);

        } else if (verifiedConnections.contains(socketChannel)) {
            OutboundConnectionChannel connectionChannel = connectionByChannel.get(socketChannel);

            NetworkEnvelopeSocketChannel envelopeSocketChannel = connectionChannel.getNetworkEnvelopeSocketChannel();
            List<NetworkEnvelope> networkEnvelopes = envelopeSocketChannel.receiveNetworkEnvelopes();
            log.debug("Received {} messages from peer {}.",
                    networkEnvelopes.size(), connectionChannel.getPeerAddress().getFullAddress());

            networkEnvelopes.forEach(networkEnvelope -> node.handleNetworkMessage(
                    networkEnvelope.getEnvelopePayloadMessage(),
                    networkEnvelope.getAuthorizationToken(),
                    connectionChannel
            ));
        }
    }

    public Optional<OutboundConnectionChannel> getConnection(Address address) {
        if (channelByAddress.containsKey(address)) {
            SocketChannel socketChannel = channelByAddress.get(address);
            OutboundConnectionChannel connectionChannel = connectionByChannel.get(socketChannel);
            return Optional.ofNullable(connectionChannel);
        }

        return Optional.empty();
    }

    public Collection<OutboundConnectionChannel> getAllOutboundConnections() {
        return connectionByChannel.values();
    }

    public void registerListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void handleConnectedChannel(SocketChannel socketChannel) {
        try {
            socketChannel.register(selector, SelectionKey.OP_WRITE);
            outboundHandshakeChannels.add(socketChannel);

            Address address = addressByChannel.get(socketChannel);
            log.info("Created outbound connection to {}", address.getFullAddress());
        } catch (ClosedChannelException e) {
            log.error("Couldn't connect to channel.", e);

            // Connection closed.
            Address address = addressByChannel.get(socketChannel);
            //noinspection resource
            channelByAddress.remove(address);

            addressByChannel.remove(socketChannel);
        }
    }

    private ByteBuffer wrapPayloadInByteBuffer(NetworkEnvelope networkEnvelope) {
        bisq.network.protobuf.NetworkEnvelope poWRequest = networkEnvelope.completeProto();
        byte[] requestInBytes = poWRequest.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ProtoBufMessageLengthWriter.writeToBuffer(requestInBytes.length, byteBuffer);
        byteBuffer.put(requestInBytes);

        byteBuffer.flip();
        return byteBuffer;
    }
}
