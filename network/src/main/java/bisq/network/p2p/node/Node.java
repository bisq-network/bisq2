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


import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.transport.ClearNetTransport;
import bisq.network.p2p.node.transport.I2PTransport;
import bisq.network.p2p.node.transport.TorTransport;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.BanList;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static bisq.network.NetworkService.DISPATCHER;
import static bisq.network.p2p.node.Node.State.INITIALIZE_SERVER;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Responsibility:
 * - Creates Transport based on TransportType
 * - Creates 1 Server associated with that server
 * - Creates inbound and outbound connections.
 * - Checks if a connection has been created when sending a proto and creates one otherwise.
 * - Performs initial connection handshake for exchanging capability and performing authorization
 * - Performs authorization protocol at sending and receiving messages
 * - Notifies ConnectionListeners when a new connection has been created or closed.
 * - Notifies MessageListeners when a new proto has been received.
 */
@Slf4j
public class Node implements Connection.Handler {
    public static final String DEFAULT = "default";

    public enum State {
        CREATED,
        INITIALIZE_SERVER,
        SERVER_INITIALIZED,
        SHUTDOWN_STARTED,
        SHUTDOWN_COMPLETE
    }

    public interface Listener {
        void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId);

        void onConnection(Connection connection);

        void onDisconnect(Connection connection, CloseReason closeReason);

        default void onStateChange(State state) {
        }
    }

    @Getter
    @ToString
    public static final class Config {
        private final Transport.Type transportType;
        private final Set<Transport.Type> supportedTransportTypes;
        private final AuthorizationService authorizationService;
        private final Transport.Config transportConfig;
        private final int socketTimeout;

        public Config(Transport.Type transportType,
                      Set<Transport.Type> supportedTransportTypes,
                      AuthorizationService authorizationService,
                      Transport.Config transportConfig,
                      int socketTimeout) {
            this.transportType = transportType;
            this.supportedTransportTypes = supportedTransportTypes;
            this.authorizationService = authorizationService;
            this.transportConfig = transportConfig;
            this.socketTimeout = socketTimeout;
        }
    }

    private final BanList banList;
    private final Transport transport;
    private final AuthorizationService authorizationService;
    private final Config config;
    @Getter
    private final String nodeId;
    @Getter
    private final Map<Address, OutboundConnection> outboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Map<Address, InboundConnection> inboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Transport.Type transportType;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, ConnectionHandshake> connectionHandshakes = new ConcurrentHashMap<>();
    private final RetryPolicy<Boolean> initializeServerRetryPolicy;
    private Optional<Server> server = Optional.empty();
    private Optional<Capability> myCapability = Optional.empty();

    private volatile boolean isStopped;
    @Getter
    public AtomicReference<State> state = new AtomicReference<>(State.CREATED);

    public Node(BanList banList, Config config, String nodeId) {
        this.banList = banList;
        transportType = config.getTransportType();
        transport = getTransport(transportType, config.getTransportConfig());
        authorizationService = config.getAuthorizationService();
        this.config = config;
        this.nodeId = nodeId;

        initializeServerRetryPolicy = RetryPolicy.<Boolean>builder()
                .handle(IllegalStateException.class)
                .handleResultIf(result -> state.get() == INITIALIZE_SERVER)
                .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(20))
                .withJitter(0.25)
                .withMaxDuration(Duration.ofMinutes(5))
                .withMaxRetries(10)
                .onRetry(e -> log.debug("Retry to call initializeServer. AttemptCount={}.", e.getAttemptCount()))
                .onRetriesExceeded(e -> log.warn("InitializeServer failed. Max retries exceeded."))
                .onSuccess(e -> log.debug("InitializeServer succeeded."))
                .build();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean maybeInitializeServer(int port) {
        return Failsafe.with(initializeServerRetryPolicy).get(() -> maybeDoInitializeServer(port));
    }

    private boolean maybeDoInitializeServer(int port) {
        switch (state.get()) {
            case CREATED: {
                setState(INITIALIZE_SERVER);
                transport.initialize();
                createServerAndListen(port);
                setState(State.SERVER_INITIALIZED);
                return true;
            }
            case INITIALIZE_SERVER: {
                log.debug("Initializing node has already started. NodeId={}", nodeId);
                throw new IllegalStateException("Initializing node has already started. NodeId=" + nodeId);
            }
            case SERVER_INITIALIZED: {
                log.debug("Node is already initialized.");
                return true;
            }
            case SHUTDOWN_STARTED: {
                log.warn("Node shutdown has been started.");
                return false;
            }
            case SHUTDOWN_COMPLETE: {
                log.warn("Node is already shutdown.");
                return false;
            }
            default: {
                throw new RuntimeException("Unhandled state " + state.get());
            }
        }
    }

    private void createServerAndListen(int port) {
        Transport.ServerSocketResult serverSocketResult = transport.getServerSocket(port, nodeId);
        myCapability = Optional.of(new Capability(serverSocketResult.getAddress(), config.getSupportedTransportTypes()));
        server = Optional.of(new Server(serverSocketResult,
                socket -> onClientSocket(socket, serverSocketResult, myCapability.get()),
                exception -> {
                    handleException(exception);
                    // If server fails we shut down the node
                    shutdown();
                }));
    }

    private void onClientSocket(Socket socket, Transport.ServerSocketResult serverSocketResult, Capability myCapability) {
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, banList, config.getSocketTimeout(), myCapability, authorizationService);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Inbound handshake request at: {}", myCapability.getAddress());
        try {
            ConnectionHandshake.Result result = connectionHandshake.onSocket(getMyLoad()); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());

            Address address = result.getCapability().getAddress();
            log.debug("Inbound handshake completed: Initiated by {} to {}", address, myCapability.getAddress());

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
                    result.getCapability(),
                    result.getLoad(),
                    result.getMetrics(),
                    this,
                    this::handleException);
            inboundConnectionsByAddress.put(connection.getPeerAddress(), connection);
            DISPATCHER.submit(() -> listeners.forEach(listener -> listener.onConnection(connection)));
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

    public Connection send(NetworkMessage networkMessage, Address address) {
        Connection connection = getConnection(address);
        return send(networkMessage, connection);
    }

    public Connection send(NetworkMessage networkMessage, Connection connection) {
        if (connection.isStopped()) {
            throw new ConnectionClosedException(connection);
        }
        try {
            AuthorizationToken token = authorizationService.createToken(networkMessage.getClass());
            return connection.send(networkMessage, token);
        } catch (Throwable throwable) {
            if (connection.isRunning()) {
                handleException(connection, throwable);
                closeConnection(connection, CloseReason.EXCEPTION.exception(throwable));
            }
            throw new ConnectionClosedException(connection);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    private Connection createOutboundConnection(Address address) {
        return myCapability.map(capability -> createOutboundConnection(address, capability))
                .orElseGet(() -> {
                    int port = NetworkUtils.findFreeSystemPort();
                    log.warn("We create an outbound connection but we have not initialized our server. " +
                            "We create a server on port {} now but clients better control node " +
                            "life cycle themselves.", port);
                    maybeInitializeServer(port);
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

        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, banList, config.getSocketTimeout(), myCapability, authorizationService);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.getAddress(), address);
        try {
            ConnectionHandshake.Result result = connectionHandshake.start(getMyLoad()); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());
            log.debug("Outbound handshake completed: Initiated by {} to {}", myCapability.getAddress(), address);
            log.debug("Create new outbound connection to {}", address);
            if (!address.isClearNetAddress()) {
                // For clearnet this check doesn't make sense because:
                // - the peer binds to 127.0.0.1, therefore reports 127.0.0.1 in the handshake
                // - we use the peer's public IP to connect to him
                checkArgument(address.equals(result.getCapability().getAddress()),
                        "Peers reported address must match address we used to connect");
            }

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
                    result.getCapability(),
                    result.getLoad(),
                    result.getMetrics(),
                    this,
                    this::handleException);
            outboundConnectionsByAddress.put(address, connection);
            DISPATCHER.submit(() -> listeners.forEach(listener -> listener.onConnection(connection)));
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

    public Stream<Connection> getAllConnections() {
        return Stream.concat(inboundConnectionsByAddress.values().stream(), outboundConnectionsByAddress.values().stream());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection.Handler
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleNetworkMessage(NetworkMessage networkMessage, AuthorizationToken authorizationToken, Connection connection) {
        if (isStopped) {
            return;
        }
        if (authorizationService.isAuthorized(networkMessage, authorizationToken)) {
            if (networkMessage instanceof CloseConnectionMessage) {
                CloseConnectionMessage closeConnectionMessage = (CloseConnectionMessage) networkMessage;
                log.debug("Node {} received CloseConnectionMessage from {} with reason: {}", this,
                        connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
                closeConnection(connection, CloseReason.CLOSE_MSG_RECEIVED.details(closeConnectionMessage.getCloseReason().name()));
            } else {
                // We got called from Connection on the dispatcher thread, so no mapping needed here.
                connection.notifyListeners(networkMessage);
                listeners.forEach(listener -> listener.onMessage(networkMessage, connection, nodeId));
            }
        } else {
            //todo handle
            log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(networkMessage.toString()));
        }
    }

    @Override
    public void handleConnectionClosed(Connection connection, CloseReason closeReason) {
        Address peerAddress = connection.getPeerAddress();
        log.debug("Node {} got called onConnectionClosed. connection={}, peerAddress={}", this, connection, peerAddress);
        boolean wasRemoved = false;
        if (connection instanceof InboundConnection) {
            wasRemoved = inboundConnectionsByAddress.remove(peerAddress) != null;
            if (!wasRemoved) {
                log.debug("Node {} did not had entry in inboundConnections at onConnectionClosed. " +
                        "This can happen if different threads triggered a close. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        } else if (connection instanceof OutboundConnection) {
            wasRemoved = outboundConnectionsByAddress.remove(peerAddress) != null;
            if (!wasRemoved) {
                log.debug("Node {} did not had entry in outboundConnections at onConnectionClosed. " +
                        "This can happen if different threads triggered a close. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        }
        if (wasRemoved) {
            listeners.forEach(listener -> listener.onDisconnect(connection, closeReason));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Close
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void closeConnection(Connection connection, CloseReason closeReason) {
        log.debug("Node {} got called closeConnection for {}", this, connection);
        connection.close(closeReason);
    }

    public void closeConnectionGracefully(Connection connection, CloseReason closeReason) {
        try {
            connection.stopListening();
            send(new CloseConnectionMessage(closeReason), connection);
        } catch (Throwable ignore) {
        }
        connection.close(CloseReason.CLOSE_MSG_SENT.details(closeReason.name()));
    }

    public CompletableFuture<Void> closeConnectionGracefullyAsync(Connection connection, CloseReason closeReason) {
        return runAsync(() -> closeConnectionGracefully(connection, closeReason), NetworkService.NETWORK_IO_POOL);
    }

    public CompletableFuture<List<Void>> shutdown() {
        log.info("Node {} shutdown", this);
        if (isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        setState(State.SHUTDOWN_STARTED);
        isStopped = true;

        server.ifPresent(Server::shutdown);
        connectionHandshakes.values().forEach(ConnectionHandshake::shutdown);

        Stream<CompletableFuture<Void>> connections = Stream.concat(inboundConnectionsByAddress.values().stream(),
                        outboundConnectionsByAddress.values().stream())
                .map(connection -> closeConnectionGracefullyAsync(connection, CloseReason.SHUTDOWN));
        return CompletableFutureUtils.allOf(connections)
                .orTimeout(1, SECONDS)
                .whenComplete((r, throwable) -> {
                    transport.shutdown();
                    outboundConnectionsByAddress.clear();
                    inboundConnectionsByAddress.clear();
                    listeners.clear();
                    if (throwable != null) {
                        log.warn("Exception at node shutdown", throwable);
                    }
                    setState(State.SHUTDOWN_COMPLETE);
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
            log.debug(exception.toString(), exception);
        } else if (exception instanceof SocketException) {
            log.debug(exception.toString(), exception);
        } else if (exception instanceof UnknownHostException) {
            log.warn("UnknownHostException. Might happen if we try to connect to wrong network type");
            log.warn(exception.toString(), exception);
        } else if (exception instanceof SocketTimeoutException) {
            log.warn(exception.toString(), exception);
        } else {
            log.error(exception.toString(), exception);
        }
    }

    private Transport getTransport(Transport.Type transportType, Transport.Config config) {
        switch (transportType) {
            case TOR:
                return new TorTransport(config);
            case I2P:
                return new I2PTransport(config);
            case CLEAR:
                return new ClearNetTransport(config);
            default:
                throw new RuntimeException("Unhandled transportType");
        }
    }

    private Load getMyLoad() {
        return new Load(getNumConnections());
    }

    private void setState(State newState) {
        log.info("Set new state {} for nodeId {}", newState, nodeId);
        checkArgument(newState.ordinal() > state.get().ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        listeners.forEach(listener -> listener.onStateChange(newState));
    }
}
