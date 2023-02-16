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

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
public class InboundConnectionsManagerTests {
    private final Path tmpDir = FileUtils.createTempDir();
    private final AuthorizationService authorizationService = createAuthorizationService();
    private final List<Transport.Type> supportedTransportTypes = new ArrayList<>(1);

    public InboundConnectionsManagerTests() throws IOException {
        supportedTransportTypes.add(Transport.Type.CLEAR);
    }

    @Test
    void validConnections() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        Address myAddress = Address.localHost(NetworkUtils.findFreeSystemPort());
        InetSocketAddress socketAddress = new InetSocketAddress(
                InetAddress.getLocalHost(),
                myAddress.getPort()
        );
        serverSocketChannel.socket().bind(socketAddress);

        Capability myCapability = new Capability(myAddress, supportedTransportTypes);

        Selector selector = SelectorProvider.provider().openSelector();
        InboundConnectionsManager inboundConnectionsManager = new InboundConnectionsManager(
                mock(BanList.class), myCapability, authorizationService, serverSocketChannel,
                selector,
                mock(Node.class)
        );

        inboundConnectionsManager.registerOpAccept();

        Thread serverThread = new Thread(() -> {
            try {
                while (selector.select() > 0) {
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
                                inboundConnectionsManager.handleInboundConnection(socketChannel, Collections.emptyList());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error: ", e);
            }
        });
        serverThread.start();

        List<SocketChannel> clientConnections = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(socketAddress);

            InetSocketAddress localSocketAddress = (InetSocketAddress) socketChannel.getLocalAddress();
            Address peerAddress = Address.localHost(localSocketAddress.getPort());

            bisq.network.protobuf.NetworkEnvelope poWRequest = createPoWRequest(myAddress, peerAddress);
            byte[] requestInBytes = poWRequest.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            ProtoBufMessageLengthWriter.writeToBuffer(requestInBytes.length, byteBuffer);
            byteBuffer.put(requestInBytes);

            byteBuffer.flip();
            socketChannel.write(byteBuffer);

            clientConnections.add(socketChannel);
        }

        int receivedReplies = 0;
        for (SocketChannel socketChannel : clientConnections) {
            NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(socketChannel);
            List<NetworkEnvelope> initialMessages = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

            assertThat(initialMessages).isNotEmpty();
            receivedReplies++;
        }

        assertThat(receivedReplies).isEqualTo(5);
    }

    @Test
    void invalidConnection() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        Address myAddress = Address.localHost(NetworkUtils.findFreeSystemPort());
        InetSocketAddress socketAddress = new InetSocketAddress(
                InetAddress.getLocalHost(),
                myAddress.getPort()
        );
        serverSocketChannel.socket().bind(socketAddress);

        Capability myCapability = new Capability(myAddress, supportedTransportTypes);

        Selector selector = SelectorProvider.provider().openSelector();
        InboundConnectionsManager inboundConnectionsManager = new InboundConnectionsManager(
                mock(BanList.class), myCapability, authorizationService, serverSocketChannel,
                selector,
                mock(Node.class)
        );

        inboundConnectionsManager.registerOpAccept();

        Thread serverThread = new Thread(() -> {
            try {
                while (selector.select() > 0) {
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

                            NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(socketChannel);
                            List<NetworkEnvelope> networkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
                            if (networkEnvelopes.isEmpty()) {
                                continue;
                            }

                            log.info("Received message from {}", socketChannel.getRemoteAddress());

                            if (inboundConnectionsManager.isInboundConnection(socketChannel)) {
                                inboundConnectionsManager.handleInboundConnection(socketChannel, networkEnvelopes);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error: ", e);
            }
        });
        serverThread.start();

        List<SocketChannel> clientConnections = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(socketAddress);

            InetSocketAddress localSocketAddress = (InetSocketAddress) socketChannel.getLocalAddress();
            Address peerAddress = Address.localHost(localSocketAddress.getPort());

            bisq.network.protobuf.NetworkEnvelope invalidPoWRequest = createPoWRequest(peerAddress, myAddress);

            byte[] requestInBytes = invalidPoWRequest.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            ProtoBufMessageLengthWriter.writeToBuffer(requestInBytes.length, byteBuffer);
            byteBuffer.put(requestInBytes);

            byteBuffer.flip();
            socketChannel.write(byteBuffer);

            clientConnections.add(socketChannel);
        }

        int receivedReplies = 0;
        for (SocketChannel socketChannel : clientConnections) {
            NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(socketChannel);
            List<NetworkEnvelope> initialMessages = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

            assertThat(initialMessages).isEmpty();
            receivedReplies++;
        }

        assertThat(receivedReplies).isEqualTo(5);
    }

    private bisq.network.protobuf.NetworkEnvelope createPoWRequest(Address myAddress, Address peerAddress) {
        List<Transport.Type> supportedTransportTypes = new ArrayList<>(1);
        supportedTransportTypes.add(Transport.Type.CLEAR);
        Capability peerCapability = new Capability(peerAddress, supportedTransportTypes);

        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Load.INITIAL_LOAD);
        AuthorizationService authorizationService = createAuthorizationService();
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                myAddress.getFullAddress(),
                0);
        return new NetworkEnvelope(NetworkEnvelope.VERSION, token, request).toProto();
    }

    private AuthorizationService createAuthorizationService() {
        String baseDir = tmpDir.toAbsolutePath().toString();
        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService);
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }
}
