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
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.NetworkUtils;
import network.misq.common.util.StringUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.authorization.AuthorizationService;
import network.misq.network.p2p.node.authorization.AuthorizationToken;
import network.misq.network.p2p.node.authorization.AuthorizedMessage;
import network.misq.network.p2p.node.transport.ClearNetTransport;
import network.misq.network.p2p.node.transport.I2PTransport;
import network.misq.network.p2p.node.transport.TorTransport;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.BanList;

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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static network.misq.network.p2p.node.CloseReason.*;

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

        default void onDisconnect(Connection connection, CloseReason closeReason) {
        }
    }

    public static record Config(Transport.Type transportType,
                                Set<Transport.Type> supportedTransportTypes,
                                AuthorizationService authorizationService,
                                Transport.Config transportConfig,
                                int socketTimeout) {
    }

    private final BanList banList;
    private final Transport transport;
    private final AuthorizationService authorizationService;
    private final Config config;
    private final String nodeId;
    @Getter
    private final Map<Address, OutboundConnection> outboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Map<Address, InboundConnection> inboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Transport.Type transportType;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, ConnectionHandshake> connectionHandshakes = new ConcurrentHashMap<>();
    private Optional<Server> server = Optional.empty();
    private Optional<Capability> myCapability = Optional.empty();

    private volatile boolean isStopped;

    public Node(BanList banList, Config config, String nodeId) {
        this.banList = banList;
        transportType = config.transportType();
        transport = getTransport(transportType, config.transportConfig());
        authorizationService = config.authorizationService();
        this.config = config;
        this.nodeId = nodeId;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void initializeServer(int port) {
        transport.initializeAsync();
        createServerAndListen(port);
    }

    private void createServerAndListen(int port) {
        Transport.ServerSocketResult serverSocketResult = transport.getServerSocket(port, nodeId);
        myCapability = Optional.of(new Capability(serverSocketResult.address(), config.supportedTransportTypes()));
        server = Optional.of(new Server(serverSocketResult,
                socket -> onClientSocket(socket, serverSocketResult, myCapability.get()),
                this::handleException));
    }

    public CompletableFuture<Transport.ServerSocketResult> initializeServerAsync(int port) {
        return transport.initializeAsync()
                .thenCompose(e -> createServerAndListenAsync(port));
    }

    private CompletableFuture<Transport.ServerSocketResult> createServerAndListenAsync(int port) {
        return transport.getServerSocketAsync(port, nodeId)
                .thenCompose(serverSocketResult -> {
                    myCapability = Optional.of(new Capability(serverSocketResult.address(), config.supportedTransportTypes()));
                    server = Optional.of(new Server(serverSocketResult,
                            socket -> onClientSocket(socket, serverSocketResult, myCapability.get()),
                            this::handleException));
                    return CompletableFuture.completedFuture(serverSocketResult);
                });
    }

    private void onClientSocket(Socket socket, Transport.ServerSocketResult serverSocketResult, Capability myCapability) {
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, banList, config.socketTimeout(), myCapability, authorizationService);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Inbound handshake request at: {}", myCapability.address());
        try {
            ConnectionHandshake.Result result = connectionHandshake.onSocket(getMyLoad()); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());

            Address address = result.capability().address();
            log.debug("Inbound handshake completed: Initiated by {} to {}", address, myCapability.address());

            // As time passed we check again if connection is still not available
            if (inboundConnectionsByAddress.containsKey(address)) {
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

            InboundConnection connection = new InboundConnection(socket,
                    serverSocketResult,
                    result.capability(),
                    result.load(),
                    this,
                    this::handleException);
            inboundConnectionsByAddress.put(connection.getPeerAddress(), connection);
            runAsync(() -> listeners.forEach(listener -> listener.onConnection(connection)), NetworkService.DISPATCHER);
        } catch (Throwable throwable) {
            connectionHandshake.shutdown();
            connectionHandshakes.remove(connectionHandshake.getId());
            try {
                socket.close();
            } catch (IOException ignore) {
            }

            handleException(throwable);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Connection send(Message message, Address address) {
        Connection connection = getConnection(address);
        return send(message, connection);
    }

    public Connection send(Message message, Connection connection) {
        if (connection.isStopped()) {
            throw new ConnectionClosedException(connection);
        }
        try {
            AuthorizationToken token = authorizationService.createToken(message.getClass()).join(); //todo
            return connection.send(new AuthorizedMessage(message, token));
        } catch (Throwable throwable) {
            if (connection.isRunning()) {
                handleException(connection, throwable);
                closeConnection(connection, CloseReason.EXCEPTION.exception(throwable));
            }
            throw new ConnectionClosedException(connection);
        }
    }


    public CompletableFuture<Connection> sendAsync(Message message, Address address) {
        return getConnectionAsync(address)
                .thenCompose(connection -> sendAsync(message, connection));
    }

    public CompletableFuture<Connection> sendAsync(Message message, Connection connection) {
        if (connection.isStopped()) {
            return CompletableFuture.failedFuture(new ConnectionClosedException(connection));
        }
        return authorizationService.createToken(message.getClass())
                .thenCompose(token -> connection.sendAsync(new AuthorizedMessage(message, token)))
                .whenComplete((con, throwable) -> {
                    if (throwable != null) {
                        if (connection.isRunning()) {
                            handleException(connection, throwable);
                            closeConnection(connection, CloseReason.EXCEPTION.exception(throwable));
                        }
                    }
                });
    }

    public CompletableFuture<Connection> getConnectionAsync(Address address) {
        return getConnectionAsync(address, true);
    }

    public CompletableFuture<Connection> getConnectionAsync(Address address, boolean allowUnverifiedAddress) {
        if (outboundConnectionsByAddress.containsKey(address)) {
            return CompletableFuture.completedFuture(outboundConnectionsByAddress.get(address));
        } else if (inboundConnectionsByAddress.containsKey(address) &&
                (allowUnverifiedAddress || inboundConnectionsByAddress.get(address).isPeerAddressVerified())) {
            return CompletableFuture.completedFuture(inboundConnectionsByAddress.get(address));
        } else {
            return createOutboundConnectionAsync(address);
        }
    }

    public Connection getConnection(Address address) {
        return getConnection(address, true);
    }

    public Connection getConnection(Address address, boolean allowUnverifiedAddress) {
        if (outboundConnectionsByAddress.containsKey(address)) {
            return outboundConnectionsByAddress.get(address);
        } else if (inboundConnectionsByAddress.containsKey(address) &&
                (allowUnverifiedAddress || inboundConnectionsByAddress.get(address).isPeerAddressVerified())) {
            return inboundConnectionsByAddress.get(address);
        } else {
            return createOutboundConnection(address);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OutboundConnection
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Connection> createOutboundConnectionAsync(Address address) {
        return myCapability.map(capability -> createOutboundConnectionAsync(address, capability))
                .orElseGet(() -> initializeServerAsync(NetworkUtils.findFreeSystemPort())
                        .thenCompose(serverSocketResult -> createOutboundConnectionAsync(address, myCapability.get())));
    }

    private CompletableFuture<Connection> createOutboundConnectionAsync(Address address, Capability myCapability) {
        return CompletableFuture.supplyAsync(() -> createOutboundConnection(address, myCapability),
                ExecutorFactory.newSingleThreadExecutor("Node.createOutboundConnection"));
    }

    private Connection createOutboundConnection(Address address) {
        return myCapability.map(capability -> createOutboundConnection(address, capability))
                .orElseGet(() -> {
                    int port = NetworkUtils.findFreeSystemPort();
                    log.warn("We create an outbound connection but we have not initialized our server. " +
                            "We create a server on port {} now but clients better control node " +
                            "life cycle themselves.", port);
                    initializeServer(port);
                    checkArgument(myCapability.isPresent(),
                            "myCapability must be present after initializeServer got called");
                    return createOutboundConnection(address, myCapability.get());
                });
    }

    private Connection createOutboundConnection(Address address, Capability myCapability) {
        if (banList.isBanned(address)) {
            throw new ConnectionException("Create outbound connection failed. PeerAddress is banned. address=" + address);
        }
        Socket socket;
        try {
            socket = transport.getSocket(address); // Blocking call
        } catch (IOException e) {
            handleException(e);
            throw new ConnectionException(e);
        }

        // As time passed we check again if connection is still not available
        if (outboundConnectionsByAddress.containsKey(address)) {
            log.warn("Node {} has already an OutboundConnection to {}. This can happen while we " +
                            "we waited for the socket creation at the createOutboundConnection method. " +
                            "We will close the socket and use the existing connection instead.",
                    this, address);
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            return outboundConnectionsByAddress.get(address);
        }

        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, banList, config.socketTimeout(), myCapability, authorizationService);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.address(), address);
        try {
            ConnectionHandshake.Result result = connectionHandshake.start(getMyLoad()); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());
            log.debug("Outbound handshake completed: Initiated by {} to {}", myCapability.address(), address);
            log.debug("Create new outbound connection to {}", address);
            checkArgument(address.equals(result.capability().address()),
                    "Peers reported address must match address we used to connect");

            // As time passed we check again if connection is still not available
            if (outboundConnectionsByAddress.containsKey(address)) {
                log.warn("Node {} has already an OutboundConnection to {}. This can happen when a " +
                                "handshake was in progress while we started a new connection to that address and as the " +
                                "handshake was not completed we did not consider that as an available connection. " +
                                "We will close the socket of that new connection and use the existing instead.",
                        this, address);
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                return outboundConnectionsByAddress.get(address);
            }

            OutboundConnection connection = new OutboundConnection(socket,
                    address,
                    result.capability(),
                    result.load(),
                    this,
                    this::handleException);
            outboundConnectionsByAddress.put(address, connection);
            runAsync(() -> listeners.forEach(listener -> listener.onConnection(connection)), NetworkService.DISPATCHER);
            return connection;
        } catch (Throwable throwable) {
            connectionHandshake.shutdown();
            connectionHandshakes.remove(connectionHandshake.getId());
            try {
                socket.close();
            } catch (IOException ignore) {
            }

            handleException(throwable);
            throw new ConnectionException(throwable);
        }
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
                    log.debug("Node {} received CloseConnectionMessage from {} with reason: {}", this, connection.getPeerAddress(), closeConnectionMessage.closeReason());
                    closeConnection(connection, CLOSE_MSG_RECEIVED.details(closeConnectionMessage.closeReason().name()));
                } else {
                    connection.notifyListeners(payLoadMessage);
                    listeners.forEach(listener -> listener.onMessage(payLoadMessage, connection, nodeId));
                }
            } else {
                //todo handle
                log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(authorizedMessage.toString()));
            }
        }
    }

    @Override
    public void onConnectionClosed(Connection connection, CloseReason closeReason) {
        Address peerAddress = connection.getPeerAddress();
        log.debug("Node {} got called onConnectionClosed. connection={}, peerAddress={}", this, connection, peerAddress);
        if (connection instanceof InboundConnection) {
            if (inboundConnectionsByAddress.remove(peerAddress) == null) {
                log.warn("Node {} did not had entry in inboundConnections. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        } else if (connection instanceof OutboundConnection) {
            if (outboundConnectionsByAddress.remove(peerAddress) == null) {
                log.warn("Node {} did not had entry in outboundConnections. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        }
        listeners.forEach(listener -> listener.onDisconnect(connection, closeReason));
    }

    public CompletableFuture<Connection> closeConnection(Connection connection, CloseReason closeReason) {
        log.debug("Node {} got called closeConnection for {}", this, connection);
        return connection.close(closeReason);
    }

    public CompletableFuture<Connection> closeConnectionGracefully(Connection connection, CloseReason closeReason) {
        return sendAsync(new CloseConnectionMessage(closeReason), connection)
                .orTimeout(200, MILLISECONDS)
                .whenComplete((c, t) -> connection.close(CLOSE_MSG_SENT.details(closeReason.name())));
    }

    public CompletableFuture<Void> shutdown() {
        log.info("Node {} shutdown", this);
        if (isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        isStopped = true;

        return runAsync(() -> {
            CountDownLatch latch = new CountDownLatch(outboundConnectionsByAddress.values().size() +
                    inboundConnectionsByAddress.size() +
                    connectionHandshakes.size() +
                    +1); // For transport
            outboundConnectionsByAddress.values()
                    .forEach(connection -> closeConnectionGracefully(connection, SHUTDOWN)
                            .whenComplete((c, t) -> latch.countDown()));
            inboundConnectionsByAddress.values()
                    .forEach(connection -> closeConnectionGracefully(connection, SHUTDOWN)
                            .whenComplete((c, t) -> latch.countDown()));
            connectionHandshakes.values()
                    .forEach(handshake -> handshake.shutdown()
                            .whenComplete((c, t) -> latch.countDown()));
            server.ifPresent(Server::shutdown);
            transport.shutdown().whenComplete((__, t) -> latch.countDown());
            try {
                if (!latch.await(1, TimeUnit.SECONDS)) {
                    log.warn("Shutdown interrupted by timeout");
                }
            } catch (InterruptedException e) {
                log.warn("Shutdown interrupted", e);
            }
            outboundConnectionsByAddress.clear();
            inboundConnectionsByAddress.clear();
            listeners.clear();
        }).orTimeout(1000, MILLISECONDS);
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

    public Optional<Address> findMyAddress() {
        return server.map(Server::getAddress);
    }

    public int getNumConnections() {
        return inboundConnectionsByAddress.size() + outboundConnectionsByAddress.size();
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
            case I2P -> new I2PTransport(config);
            case CLEAR -> new ClearNetTransport(config);
        };
    }

    private Load getMyLoad() {
        return new Load(getNumConnections());
    }
}
