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

import bisq.network.p2p.ConnectionHandshakeInitiator;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.services.peergroup.BanList;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OutboundConnectionManager {
    public interface Listener {
        void onNewConnection(OutboundConnectionChannel outboundConnectionChannel);
    }

    private final AuthorizationService authorizationService;
    private final BanList banList;
    private final Load myLoad;
    private final Capability myCapability;
    @Getter
    private final Selector selector;

    private final List<SocketChannel> connectingChannels = new CopyOnWriteArrayList<>();
    private final Map<SocketChannel, Address> addressByChannel = new ConcurrentHashMap<>();
    private final Map<Address, SocketChannel> channelByAddress = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ConnectionHandshakeInitiator> handshakeInitiatorByChannel = new ConcurrentHashMap<>();

    private final List<SocketChannel> outboundHandshakeChannels = new CopyOnWriteArrayList<>();
    private final List<SocketChannel> verifiedConnections = new CopyOnWriteArrayList<>();

    private final Map<SocketChannel, OutboundConnectionChannel> connectionByChannel = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public OutboundConnectionManager(AuthorizationService authorizationService, BanList banList, Load myLoad, Capability myCapability, Selector selector) {
        this.authorizationService = authorizationService;
        this.banList = banList;
        this.myLoad = myLoad;
        this.myCapability = myCapability;
        this.selector = selector;
    }

    public boolean areConnectionsInProgress() {
        return !connectingChannels.isEmpty() || !outboundHandshakeChannels.isEmpty();
    }

    public void createNewConnection(Address address) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            connectingChannels.add(socketChannel);
            addressByChannel.put(socketChannel, address);
            channelByAddress.put(address, socketChannel);

            InetSocketAddress socketAddress = new InetSocketAddress(address.getHost(), address.getPort());
            boolean isConnectedImmediately = socketChannel.connect(socketAddress);
            if (isConnectedImmediately) {
                handleConnectedChannel(socketChannel);
            }

        } catch (IOException e) {
            log.warn("Couldn't create connection to " + address.getFullAddress(), e);
        }
    }

    public void handleConnectableChannel(SocketChannel socketChannel) throws IOException {
        try {
            socketChannel.finishConnect();
            handleConnectedChannel(socketChannel);

        } catch (ConnectException e) {
            // Couldn't connect to peer, nothing we can do.
            Address address = addressByChannel.get(socketChannel);
            channelByAddress.remove(address);

            connectingChannels.remove(socketChannel);
            addressByChannel.remove(socketChannel);
        }
    }

    public void handleWritableChannel(SocketChannel socketChannel) throws IOException {
        if (outboundHandshakeChannels.contains(socketChannel)) {
            var handshakeInitiator = new ConnectionHandshakeInitiator(
                    myCapability,
                    authorizationService,
                    banList,
                    myLoad,
                    addressByChannel.get(socketChannel)
            );
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
            OutboundConnectionChannel outboundConnectionChannel = new OutboundConnectionChannel(
                    peerCapability,
                    handshakeResponse.getLoad(),
                    networkEnvelopeSocketChannel,
                    new Metrics()
            );

            connectionByChannel.put(socketChannel, outboundConnectionChannel);
            listeners.forEach(l -> l.onNewConnection(outboundConnectionChannel));
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

            connectingChannels.remove(socketChannel);
            outboundHandshakeChannels.add(socketChannel);

            Address address = addressByChannel.get(socketChannel);
            log.info("Created outbound connection to {}", address.getFullAddress());
        } catch (ClosedChannelException e) {
            // Connection closed.
            Address address = addressByChannel.get(socketChannel);
            channelByAddress.remove(address);

            connectingChannels.remove(socketChannel);
            addressByChannel.remove(socketChannel);
        }
    }

    private ByteBuffer wrapPayloadInByteBuffer(NetworkEnvelope networkEnvelope) {
        bisq.network.protobuf.NetworkEnvelope poWRequest = networkEnvelope.toProto();
        byte[] requestInBytes = poWRequest.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ProtoBufMessageLengthWriter.writeToBuffer(requestInBytes.length, byteBuffer);
        byteBuffer.put(requestInBytes);

        byteBuffer.flip();
        return byteBuffer;
    }
}
