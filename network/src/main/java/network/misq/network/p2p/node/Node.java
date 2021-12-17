/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.node;


import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.NetworkUtils;
import network.misq.common.util.StringUtils;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.authorization.AuthorizationService;
import network.misq.network.p2p.node.authorization.AuthorizedMessage;
import network.misq.network.p2p.node.transport.ClearNetTransport;
import network.misq.network.p2p.node.transport.I2PTransport;
import network.misq.network.p2p.node.transport.TorTransport;
import network.misq.network.p2p.node.transport.Transport;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Responsibility:
 * - Creates Transport based on TransportType
 * - Creates 1 Server associated with that server
 * - Creates inbound and outbound connections.
 * - Checks if a connection has been created when sending a message and creates one otherwise.
 * - Performs initial connection handshake for exchanging capability and performing authorization
 * - Performs authorization protocol at sending and receiving messages
 * - Notifies ConnectionListeners when a new connection has been created or closed.
 * - Notifies MessageListeners when a new message has been received.
 */
@Slf4j
public class Node implements Connection.Handler {
    public static final String DEFAULT_NODE_ID = "default";

    public interface Listener {
        void onMessage(Message message, Connection connection, String nodeId);

        default void onConnection(Connection connection) {
        }

        default void onDisconnect(Connection connection) {
        }
    }

    public static record Config(Transport.Type transportType,
                                Set<Transport.Type> supportedTransportTypes,
                                AuthorizationService authorizationService,
                                Transport.Config transportConfig,
                                int socketTimeout) {
    }

    private final Transport transport;
    private final AuthorizationService authorizationService;
    private final Config config;
    private final String nodeId;
    @Getter
    private final Map<Address, OutboundConnection> outboundConnections = new ConcurrentHashMap<>();
    @Getter
    private final Map<Address, InboundConnection> inboundConnections = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, Listener> connectionListenersByConnectionId = new ConcurrentHashMap<>();

    private Optional<Server> server = Optional.empty();
    private Optional<Capability> myCapability;
    private volatile boolean isStopped;

    public Node(Config config, String nodeId) {
        transport = getTransport(config.transportType(), config.transportConfig());
        authorizationService = config.authorizationService();
        this.config = config;
        this.nodeId = nodeId;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public CompletableFuture<Transport.ServerSocketResult> initializeServer(int port) {
        return transport.initialize()
                .thenCompose(e -> createServerAndListen(port));
    }

    private CompletableFuture<Transport.ServerSocketResult> createServerAndListen(int port) {
        return transport.getServerSocket(port, nodeId)
                .thenCompose(serverSocketResult -> {
                    myCapability = Optional.of(new Capability(serverSocketResult.address(), config.supportedTransportTypes()));
                    server = Optional.of(new Server(serverSocketResult,
                            socket -> onClientSocket(socket, serverSocketResult, myCapability.get()),
                            this::handleException));
                    return CompletableFuture.completedFuture(serverSocketResult);
                });
    }

    private void onClientSocket(Socket socket, Transport.ServerSocketResult serverSocketResult, Capability myCapability) {
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, config.socketTimeout(), myCapability, authorizationService);
        log.debug("Inbound handshake request at: {}", myCapability.address());
        connectionHandshake.onSocket(getMyLoad())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        handleException(throwable);
                        return;
                    }

                    Address address = result.capability().address();
                    log.debug("Inbound handshake completed: Initiated by {} to {}", address, myCapability.address());
                    if (inboundConnections.containsKey(address)) {
                        log.warn("Node {} have already an InboundConnection from {}. This can happen when a " +
                                        "handshake was in progress while we received a new connection from that address. " +
                                        "We will close the socket of that new connection and use the existing instead.",
                                this, address);
                        try {
                            socket.close();
                        } catch (IOException ignore) {
                        }
                        return;
                    }

                    InboundConnection connection = new InboundConnection(socket, serverSocketResult, result.capability(), result.load(), this);
                    inboundConnections.put(connection.getPeerAddress(), connection);
                    connection.startListen(exception -> handleException(connection, exception));
                    runAsync(() -> {
                        listeners.forEach(listener -> listener.onConnection(connection));
                        connectionListenersByConnectionId.entrySet().stream()
                                .filter(entry -> entry.getKey().equals(connection.getId()))
                                .forEach(entry -> entry.getValue().onConnection(connection));
                    });
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Connection> send(Message message, Address address) {
        return getConnection(address)
                .thenCompose(connection -> send(message, connection));
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        if (connection.isStopped()) {
            return CompletableFuture.failedFuture(new ConnectionClosedException(connection));
        }
        return authorizationService.createToken(message.getClass())
                .thenCompose(token -> connection.send(new AuthorizedMessage(message, token)))
                .whenComplete((con, throwable) -> {
                    if (throwable != null) {
                        if (!connection.isStopped()) {
                            handleException(connection, throwable);
                        }
                    } else if (message instanceof CloseConnectionMessage) {
                        closeConnection(connection);
                        //Scheduler.run(() -> closeConnection(connection)).after(100);
                    }
                });
    }

    public CompletableFuture<Connection> getConnection(Address address) {
        if (outboundConnections.containsKey(address)) {
            return CompletableFuture.completedFuture(outboundConnections.get(address));
        } else if (inboundConnections.containsKey(address) &&
                inboundConnections.get(address).isPeerAddressVerified()) {
            return CompletableFuture.completedFuture(inboundConnections.get(address));
        } else {
            return createOutboundConnection(address);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OutboundConnection
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Connection> createOutboundConnection(Address address) {
        return myCapability.map(capability -> createOutboundConnection(address, capability))
                .orElseGet(() -> initializeServer(NetworkUtils.findFreeSystemPort())
                        .thenCompose(serverSocketResult -> createOutboundConnection(address, myCapability.get())));
    }

    private CompletableFuture<Connection> createOutboundConnection(Address address, Capability myCapability) {
        Socket socket;
        try {
            socket = transport.getSocket(address);
        } catch (IOException e) {
            handleException(e);
            return CompletableFuture.failedFuture(e);
        }

        CompletableFuture<Connection> future = new CompletableFuture<>();
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, config.socketTimeout(), myCapability, authorizationService);
        log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.address(), address);
        connectionHandshake.start(getMyLoad())
                .whenComplete((result, throwable) -> {
                    log.debug("Outbound handshake completed: Initiated by {} to {}", myCapability.address(), address);
                    log.debug("Create new outbound connection to {}", address);
                    checkArgument(address.equals(result.capability().address()),
                            "Peers reported address must match address we used to connect");
                    if (outboundConnections.containsKey(address)) {
                        log.warn("Node {} has already an OutboundConnection to {}. This can happen when a " +
                                        "handshake was in progress while we started a new connection to that address and as the " +
                                        "handshake was not completed we did not consider that as an available connection. " +
                                        "We will close the socket of that new connection and use the existing instead.",
                                this, address);
                        try {
                            socket.close();
                        } catch (IOException ignore) {
                        }
                        future.complete(outboundConnections.get(address));
                        return;
                    }
                    OutboundConnection connection = new OutboundConnection(socket, address, result.capability(), result.load(), this);

                    outboundConnections.put(address, connection);
                    connection.startListen(exception -> handleException(connection, exception));
                    runAsync(() -> {
                        listeners.forEach(listener -> listener.onConnection(connection));
                        connectionListenersByConnectionId.entrySet().stream()
                                .filter(e -> e.getKey().equals(connection.getId()))
                                .forEach(e -> e.getValue().onConnection(connection));
                    });
                    future.complete(connection);
                });
        return future;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection.Handler
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        if (isStopped) {
            return;
        }
        if (message instanceof AuthorizedMessage authorizedMessage) {
            if (authorizationService.isAuthorized(authorizedMessage)) {
                Message payLoadMessage = authorizedMessage.message();
                if (payLoadMessage instanceof CloseConnectionMessage closeConnectionMessage) {
                    log.debug("Node {} received CloseConnectionMessage with reason: {}", this, closeConnectionMessage.reason());
                    closeConnection(connection);
                } else {
                    runAsync(() -> {
                        connection.notifyListeners(payLoadMessage);
                        listeners.forEach(listener -> listener.onMessage(payLoadMessage, connection, nodeId));
                    });
                }
            } else {
                log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(authorizedMessage.toString()));
            }
        }
    }

    @Override
    public void onConnectionClosed(Connection connection) {
        Address peerAddress = connection.getPeerAddress();
        log.debug("Node {} got called onConnectionClosed. connection={}, peerAddress={}", this, connection, peerAddress);
        if (connection instanceof InboundConnection) {
            if (inboundConnections.remove(peerAddress) == null) {
                log.warn("Node {} did not had entry in inboundConnections. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        } else if (connection instanceof OutboundConnection) {
            if (outboundConnections.remove(peerAddress) == null) {
                log.warn("Node {} did not had entry in outboundConnections. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        }
        runAsync(() -> {
            listeners.forEach(listener -> listener.onDisconnect(connection));
            connectionListenersByConnectionId.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(connection.getId()))
                    .forEach(entry -> entry.getValue().onDisconnect(connection));
        });
    }

    public CompletableFuture<Void> closeConnection(Connection connection) {
        log.debug("Node {} got called closeConnection for {}", this, connection);
        return connection.shutdown();
    }

    public CompletableFuture<Void> shutdown() {
        log.info("Node {} shutdown", this);
        if (isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        isStopped = true;

        CountDownLatch latch = new CountDownLatch(outboundConnections.values().size() +
                inboundConnections.size() +
                ((int) server.stream().count())
                + 1); // For transport
        return CompletableFuture.runAsync(() -> {
            outboundConnections.values().forEach(connection -> connection.shutdown().whenComplete((v, t) -> latch.countDown()));
            inboundConnections.values().forEach(connection -> connection.shutdown().whenComplete((v, t) -> latch.countDown()));
            server.ifPresent(server -> server.shutdown().whenComplete((v, t) -> latch.countDown()));
            transport.shutdown().whenComplete((v, t) -> latch.countDown());

            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Shutdown interrupted by timeout");
            }
            outboundConnections.clear();
            inboundConnections.clear();
            listeners.clear();
        });
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return transport.getSocksProxy();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void addListener(Connection connection, Listener listener) {
        connectionListenersByConnectionId.put(connection.getId(), listener);
    }

    public void removeListener(Connection connection, Listener listener) {
        connectionListenersByConnectionId.remove(connection.getId());
    }


    public Optional<Address> findMyAddress() {
        return server.map(Server::getAddress);
    }

    public int getNumConnections() {
        return inboundConnections.size() + outboundConnections.size();
    }

    @Override
    public String toString() {
        return findMyAddress().map(Address::toString).orElse("null");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleException(Connection connection, Throwable exception) {
        log.warn("Node {} got called handleException. connection={}, exception={}", this, connection, exception.getMessage());
        if (isStopped) {
            return;
        }
        if (connection.isRunning()) {
            handleException(exception);
        }
    }

    private void handleException(Throwable exception) {
        log.debug("Node {} got called handleException. exception={}", this, exception.getMessage());

        if (isStopped) {
            return;
        }
        if (exception instanceof EOFException) {
            //log.debug(exception.toString(), exception);
        } else if (exception instanceof SocketException) {
            //log.debug(exception.toString(), exception);
        } else if (exception instanceof SocketTimeoutException) {
            log.warn(exception.toString(), exception);
        } else {
            log.error(exception.toString(), exception);
        }
    }

    private Transport getTransport(Transport.Type transportType, Transport.Config config) {
        return switch (transportType) {
            case TOR -> new TorTransport(config);
            case I2P -> I2PTransport.getInstance(config);
            case CLEAR_NET -> new ClearNetTransport(config);
        };
    }

    private Load getMyLoad() {
        return new Load(getNumConnections());
    }
}
