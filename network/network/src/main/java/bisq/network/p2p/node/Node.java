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
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.threading.AbortPolicyWithLogging;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.MaxSizeAwareQueue;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.NetworkExecutors;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.CloseConnectionMessage;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.node.transport.ServerSocketResult;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static bisq.network.p2p.node.ConnectionException.Reason.ADDRESS_BANNED;
import static bisq.network.p2p.node.Node.State.STARTING;
import static bisq.network.p2p.node.Node.State.STOPPING;
import static bisq.network.p2p.node.Node.State.TERMINATED;
import static com.google.common.base.Preconditions.checkArgument;
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
 * - Notifies ConfidentialMessageListeners when a new proto has been received.
 */
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node implements Connection.Handler {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    public interface Listener {
        void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId);

        void onConnection(Connection connection);

        void onDisconnect(Connection connection, CloseReason closeReason);

        default void onShutdown(Node node) {
        }

        default void onStateChange(Node.State state) {
        }
    }

    @Getter
    @ToString
    public static final class Config {
        private final TransportType transportType;
        private final Set<TransportType> supportedTransportTypes;
        private final Set<Feature> features;
        private final TransportConfig transportConfig;
        private final int socketTimeout; // in ms
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;
        private final int maxNumConnectedPeers;

        public Config(TransportType transportType,
                      Set<TransportType> supportedTransportTypes,
                      Set<Feature> features,
                      TransportConfig transportConfig,
                      int socketTimeout,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime,
                      int maxNumConnectedPeers) {
            this.transportType = transportType;
            this.supportedTransportTypes = supportedTransportTypes;
            this.features = features;
            this.transportConfig = transportConfig;
            this.socketTimeout = socketTimeout;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
            this.maxNumConnectedPeers = maxNumConnectedPeers;
        }
    }

    private final Object executorLock = new Object();
    private volatile ThreadPoolExecutor executor;
    private final BanList banList;
    private final TransportService transportService;
    private final AuthorizationService authorizationService;
    private final int socketTimeout; // in ms
    private final Set<TransportType> supportedTransportTypes;
    private final Set<Feature> features;
    @Getter
    private final boolean isDefaultNode;
    @EqualsAndHashCode.Include
    @Getter
    private final TransportType transportType;
    @EqualsAndHashCode.Include
    @Getter
    private final NetworkId networkId;
    @Getter
    private final KeyBundle keyBundle;
    @Getter
    private final Map<Address, OutboundConnection> outboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Map<Address, InboundConnection> inboundConnectionsByAddress = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, ConnectionHandshake> connectionHandshakes = new ConcurrentHashMap<>();
    private Optional<Server> server = Optional.empty();
    private Optional<Capability> myCapability = Optional.empty();
    @Getter
    public final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    @Getter
    public final Observable<State> observableState = new Observable<>(State.NEW);
    @Getter
    public final NetworkLoadSnapshot networkLoadSnapshot;
    @Getter
    private final String nodeId;
    private final Config config;
    private Optional<CountDownLatch> startingStateLatch = Optional.empty();

    public Node(NetworkId networkId,
                boolean isDefaultNode,
                Config config,
                BanList banList,
                KeyBundleService keyBundleService,
                TransportService transportService,
                NetworkLoadSnapshot networkLoadSnapshot,
                AuthorizationService authorizationService) {
        this.networkId = networkId;
        keyBundle = keyBundleService.getOrCreateKeyBundle(networkId.getKeyId());
        this.isDefaultNode = isDefaultNode;
        this.config = config;
        transportType = config.getTransportType();
        supportedTransportTypes = config.getSupportedTransportTypes();
        features = config.getFeatures();
        socketTimeout = config.getSocketTimeout();
        this.banList = banList;
        this.transportService = transportService;
        this.authorizationService = authorizationService;
        this.networkLoadSnapshot = networkLoadSnapshot;
        nodeId = networkId.getId();
    }


    /* --------------------------------------------------------------------- */
    // Server
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Node> initializeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (startingStateLatch.isPresent() && startingStateLatch.get().getCount() > 0) {
                try {
                    log.info("Our node is still starting up. We block the calling thread until state is RUNNING or a throw and exception after a timeout. Node: {}", getNodeInfo());
                    boolean success = startingStateLatch.get().await(120, TimeUnit.SECONDS);
                    if (!success) {
                        String errorMessage = "We got called a repeated initialize. State has not change from STARTING to RUNNING in 120 sec. Node: " + getNodeInfo();
                        log.warn(errorMessage);
                        throw new RuntimeException(new TimeoutException(errorMessage));
                    } else {
                        log.debug("We are now in RUNNING state");
                    }
                } catch (InterruptedException e) {
                    log.warn("Thread got interrupted at initialize method", e);
                    Thread.currentThread().interrupt(); // Restore interrupted state
                }
            }
            synchronized (state) {
                switch (state.get()) {
                    case NEW: {
                        setState(STARTING);
                        startingStateLatch = Optional.of(new CountDownLatch(1));
                        createServerAndListen();
                        startingStateLatch.get().countDown();
                        setState(State.RUNNING);
                        break;
                    }
                    case STARTING: {
                        throw new IllegalStateException("STARTING state should never be reached here as we use a " +
                                "countDownLatch to block while in STARTING state. NetworkId=" + networkId + "; transportType=" + transportType);
                    }
                    case RUNNING: {
                        log.debug("Got called while already running. We ignore that call.");
                        break;
                    }
                    case STOPPING:
                        throw new IllegalStateException("Already stopping. NetworkId=" + networkId + "; transportType=" + transportType);
                    case TERMINATED:
                        throw new IllegalStateException("Already terminated. NetworkId=" + networkId + "; transportType=" + transportType);
                    default: {
                        throw new IllegalStateException("Unhandled state " + state.get());
                    }
                }
            }
            return this;
        }, getExecutor());
    }

    private void createServerAndListen() {
        ServerSocketResult serverSocketResult = transportService.getServerSocket(networkId, keyBundle, nodeId); // blocking
        myCapability = Optional.of(Capability.myCapability(serverSocketResult.getAddress(), new ArrayList<>(supportedTransportTypes), new ArrayList<>(features)));
        server = Optional.of(new Server(serverSocketResult,
                socketTimeout,
                socket -> handleNewClientSocketAsync(socket, myCapability.get()),
                exception -> {
                    handleException(exception);
                    // If server fails we shut down the node
                    shutdown();
                }));
    }

    private CompletableFuture<Void> handleNewClientSocketAsync(Socket socket, Capability myCapability) {
        return CompletableFuture.runAsync(() -> {
            ConnectionHandshake connectionHandshake = null;
            try {
                connectionHandshake = new ConnectionHandshake(socket,
                        banList,
                        myCapability,
                        authorizationService,
                        keyBundle);
                connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
                log.debug("Inbound handshake request at: {}", myCapability.getAddress());
                ConnectionHandshake.Result result = connectionHandshake.onSocket(networkLoadSnapshot.getCurrentNetworkLoad()); // Blocking call

                Address address = result.getPeersCapability().getAddress();
                log.debug("Inbound handshake completed: Initiated by {} to {}", address, myCapability.getAddress());

                // As time passed we check again if connection is still not available
                if (inboundConnectionsByAddress.containsKey(address)) {
                    log.warn("Have already an InboundConnection from {}. This can happen when a " +
                            "handshake was in progress while we received a new connection from that address. " +
                            "We close the existing connection (instead of closing the new socket) as the existing connection might be a stale connection.", address);
                    inboundConnectionsByAddress.get(address).shutdown(CloseReason.MAYBE_STALE_CONNECTION);
                }

                InboundConnection connection = createInboundConnection(socket, result);
                inboundConnectionsByAddress.put(connection.getPeerAddress(), connection);
                listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onConnection(connection)));
            } catch (Throwable throwable) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }

                handleException(throwable);
            } finally {
                if (connectionHandshake != null) {
                    connectionHandshake.shutdown();
                    connectionHandshakes.remove(connectionHandshake.getId());
                }
            }
        }, getExecutor());
    }

    private InboundConnection createInboundConnection(Socket socket, ConnectionHandshake.Result result) {
        NetworkLoadSnapshot peersNetworkLoadSnapshot = new NetworkLoadSnapshot(result.getPeersNetworkLoad());
        ConnectionThrottle connectionThrottle = new ConnectionThrottle(peersNetworkLoadSnapshot, networkLoadSnapshot, config);
        return new InboundConnection(authorizationService,
                result.getConnectionId(),
                socket,
                result.getPeersCapability(),
                peersNetworkLoadSnapshot,
                result.getConnectionMetrics(),
                connectionThrottle,
                this,
                this::handleException);
    }


    /* --------------------------------------------------------------------- */
    // Send
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Connection> sendAsync(EnvelopePayloadMessage envelopePayloadMessage, Address address) {
        try {
            return getOrCreateConnectionAsync(address)
                    .thenCompose(connection -> sendAsync(envelopePayloadMessage, connection));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Connection> sendAsync(EnvelopePayloadMessage envelopePayloadMessage,
                                                   Connection connection) {
        try {
            return connection.sendAsync(envelopePayloadMessage)
                    .handle((con, exception) -> {
                        if (exception != null) {
                            if (connection.isRunning() && !(exception.getCause() instanceof SocketException)) {
                                handleException(connection, exception);
                                log.debug("Send message failed", exception);
                                closeConnection(connection, CloseReason.EXCEPTION.exception(exception));
                            }
                            throw new ConnectionClosedException(connection);
                        }
                        return con;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Connection
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Connection> getOrCreateConnectionAsync(Address address) {
        if (outboundConnectionsByAddress.containsKey(address)) {
            return CompletableFuture.completedFuture(outboundConnectionsByAddress.get(address));
        } else if (inboundConnectionsByAddress.containsKey(address)) {
            return CompletableFuture.completedFuture(inboundConnectionsByAddress.get(address));
        } else {
            return createOutboundConnectionAsync(address);
        }
    }

    public boolean hasConnection(Address address) {
        return outboundConnectionsByAddress.containsKey(address) || inboundConnectionsByAddress.containsKey(address);
    }

    public Optional<Connection> findConnection(Connection connection) {
        if (connection instanceof OutboundConnection) {
            return Optional.ofNullable(outboundConnectionsByAddress.get(connection.getPeerAddress()));
        } else {
            return Optional.ofNullable(inboundConnectionsByAddress.get(connection.getPeerAddress()));
        }
    }

    public Optional<Connection> findConnection(Address address) {
        if (outboundConnectionsByAddress.containsKey(address)) {
            return Optional.of(outboundConnectionsByAddress.get(address));
        } else if (inboundConnectionsByAddress.containsKey(address)) {
            return Optional.of(inboundConnectionsByAddress.get(address));
        } else {
            return Optional.empty();
        }
    }


    /* --------------------------------------------------------------------- */
    // OutboundConnection
    /* --------------------------------------------------------------------- */

    private CompletableFuture<Connection> createOutboundConnectionAsync(Address address) {
        // myCapability is set once we have start our sever which happens in initialize()
        return myCapability.map(capability -> createOutboundConnectionAsync(address, capability))
                .orElseGet(() -> {
                    int port = networkId.getAddressByTransportTypeMap().get(transportType).getPort();
                    log.warn("We create an outbound connection but we have not initialized our server. " +
                            "We create a server on port {} now but clients better control node " +
                            "life cycle themselves.", port);
                    return initializeAsync()
                            .thenCompose(node -> {
                                checkArgument(myCapability.isPresent(), "myCapability must be present after initializeServer got called");
                                return createOutboundConnectionAsync(address, myCapability.get());
                            });
                });
    }

    private CompletableFuture<Connection> createOutboundConnectionAsync(Address address, Capability myCapability) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Create outbound connection to {}", address);
            return createOutboundConnection(address, myCapability);
        }, getExecutor());
    }

    private Connection createOutboundConnection(Address address, Capability myCapability) {
        if (banList.isBanned(address)) {
            throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + address);
        }

        Socket socket = createSocket(address); // Blocking call

        // As time passed we check again if connection is still not available
        Optional<OutboundConnection> outboundConnection = findOutboundConnectionAndCloseSocketIfPresent(address, socket);
        if (outboundConnection.isPresent()) {
            return outboundConnection.get();
        }

        try {
            ConnectionHandshake.Result result = startConnectionHandshake(address, socket, myCapability); // Blocking call
            log.debug("Create new outbound connection to {}", address);

            // As time passed we check again if connection is still not available
            outboundConnection = findOutboundConnectionAndCloseSocketIfPresent(address, socket);
            if (outboundConnection.isPresent()) {
                return outboundConnection.get();
            }

            if (!isDefaultNode) {
                log.info("We create an outbound connection to {} from a user node. node={}", address, getNodeInfo());
            }

            return createNewOutboundConnection(address, socket, result);
        } catch (Throwable throwable) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
            handleException(throwable);
            throw new ConnectionException(throwable);
        }
    }

    private Socket createSocket(Address address) {
        try {
            return transportService.getSocket(address, nodeId); // Blocking call
        } catch (IOException e) {
            handleException(e);
            throw new ConnectionException(e);
        }
    }

    private OutboundConnection createNewOutboundConnection(Address address,
                                                           Socket socket,
                                                           ConnectionHandshake.Result result) {
        OutboundConnection connection = null;
        try {
            NetworkLoadSnapshot peersNetworkLoadSnapshot = new NetworkLoadSnapshot(result.getPeersNetworkLoad());
            ConnectionThrottle connectionThrottle = new ConnectionThrottle(peersNetworkLoadSnapshot, networkLoadSnapshot, config);
            connection = new OutboundConnection(authorizationService,
                    result.getConnectionId(),
                    socket,
                    address,
                    result.getPeersCapability(),
                    peersNetworkLoadSnapshot,
                    result.getConnectionMetrics(),
                    connectionThrottle,
                    this,
                    this::handleException);
            outboundConnectionsByAddress.put(address, connection);

            OutboundConnection finalConnection = connection;
            listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onConnection(finalConnection)));
            return connection;
        } catch (Exception exception) {
            log.error("Creating outbound connection failed", exception);
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            if (connection != null) {
                connection.shutdown(CloseReason.EXCEPTION);
                outboundConnectionsByAddress.remove(address);
            }
            handleException(exception);
            throw new ConnectionException(exception);
        }
    }

    private ConnectionHandshake.Result startConnectionHandshake(Address address,
                                                                Socket socket,
                                                                Capability myCapability) {
        ConnectionHandshake connectionHandshake = null;
        try {
            connectionHandshake = new ConnectionHandshake(socket,
                    banList,
                    myCapability,
                    authorizationService,
                    keyBundle);

            connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
            log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.getAddress(), address);
            ConnectionHandshake.Result result = connectionHandshake.start(networkLoadSnapshot.getCurrentNetworkLoad(), address);
            log.debug("Outbound handshake completed: Initiated by {} to {}", myCapability.getAddress(), address);

            if (!address.isClearNetAddress()) {
                // For clearnet this check doesn't make sense because:
                // - the peer binds to 127.0.0.1, therefore reports 127.0.0.1 in the handshake
                // - we use the peer's public IP to connect to him
                checkArgument(address.equals(result.getPeersCapability().getAddress()),
                        "Peers reported address must match address we used to connect");
            }
            return result;
        } catch (Exception exception) {
            log.error("Starting outbound handshake to {} failed. {}", address, exception.getMessage());
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            handleException(exception);
            throw new ConnectionException(ConnectionException.Reason.HANDSHAKE_FAILED, exception);
        } finally {
            if (connectionHandshake != null) {
                connectionHandshake.shutdown();
                connectionHandshakes.remove(connectionHandshake.getId());
            }
        }
    }

    private Optional<OutboundConnection> findOutboundConnectionAndCloseSocketIfPresent(Address address, Socket socket) {
        if (outboundConnectionsByAddress.containsKey(address)) {
            log.warn("Has have already an OutboundConnection to {}. This can happen while we " +
                    "we waited for the socket creation at the createOutboundConnection method. " +
                    "We will close the socket and use the existing connection instead.", address);
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            // ofNullable in case the connection have been removed in the meantime.
            return Optional.ofNullable(outboundConnectionsByAddress.get(address));
        }
        return Optional.empty();
    }

    public Stream<Connection> getAllConnections() {
        return Stream.concat(outboundConnectionsByAddress.values().stream(), inboundConnectionsByAddress.values().stream());
    }

    public Stream<Connection> getAllActiveConnections() {
        return getAllConnections().filter(Connection::isRunning);
    }

    public Stream<OutboundConnection> getActiveOutboundConnections() {
        return getOutboundConnectionsByAddress().values().stream().filter(Connection::isRunning);
    }

    public Stream<InboundConnection> getActiveInboundConnections() {
        return getInboundConnectionsByAddress().values().stream().filter(Connection::isRunning);
    }

    public int getNumConnections() {
        return (int) getAllActiveConnections().count();
    }

    CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId) {
        return transportService.isPeerOnlineAsync(address, nodeId);
    }



    /* --------------------------------------------------------------------- */
    // Connection.Handler
    /* --------------------------------------------------------------------- */

    @Override
    public boolean isMessageAuthorized(EnvelopePayloadMessage envelopePayloadMessage,
                                       AuthorizationToken authorizationToken,
                                       Connection connection) {
        if (isShutdown()) {
            return false;
        }

        Optional<Address> optionalAddress = findMyAddress();
        checkArgument(optionalAddress.isPresent(), "My address must be present");
        String myAddress = optionalAddress.get().getFullAddress();
        boolean isAuthorized = authorizationService.isAuthorized(envelopePayloadMessage,
                authorizationToken,
                networkLoadSnapshot.getCurrentNetworkLoad(),
                networkLoadSnapshot.getPreviousNetworkLoad(),
                connection.getId(),
                myAddress);
        if (!isAuthorized) {
            log.warn("Message authorization failed. Peer={}; Message={}", connection.getPeerAddress(), StringUtils.truncate(envelopePayloadMessage.toString()));
            connection.shutdown(CloseReason.AUTHORIZATION_FAILED);

            //TODO See https://github.com/bisq-network/bisq2/issues/3693
            // banList.add(connection.getPeerAddress(), BanList.Reason.AUTHORIZATION_FAILED);
        }

        return isAuthorized;
    }

    @Override
    public void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection) {
        if (isShutdown()) {
            return;
        }

        closeIfOrphanedConnection(envelopePayloadMessage, connection);

        if (envelopePayloadMessage instanceof CloseConnectionMessage closeConnectionMessage) {
            log.debug("Received CloseConnectionMessage from {} with reason: {}",
                    connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
            closeConnection(connection, CloseReason.CLOSE_MSG_RECEIVED.details(closeConnectionMessage.getCloseReason().name()));
        }

        // Even we get a CloseConnectionMessage we notify listeners as we want to track it for instance for metrics
        listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onMessage(envelopePayloadMessage, connection, networkId)));
    }

    @Override
    public void handleConnectionClosed(Connection connection, CloseReason closeReason) {
        Address peerAddress = connection.getPeerAddress();
        log.debug("Got called onConnectionClosed. connection={}, peerAddress={}", connection, peerAddress);
        boolean wasRemoved = false;
        if (connection instanceof InboundConnection) {
            wasRemoved = inboundConnectionsByAddress.remove(peerAddress) != null;
            if (!wasRemoved) {
                log.debug("Did not had entry in inboundConnections at onConnectionClosed. " +
                                "This can happen if different threads triggered a close. connection={}, peerAddress={}",
                        connection, peerAddress);
            }
        } else if (connection instanceof OutboundConnection) {
            wasRemoved = outboundConnectionsByAddress.remove(peerAddress) != null;
            if (!wasRemoved) {
                log.debug("Did not had entry in outboundConnections at onConnectionClosed. " +
                                "This can happen if different threads triggered a close. connection={}, peerAddress={}",
                        connection, peerAddress);
            }
        }
        if (wasRemoved) {
            listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onDisconnect(connection, closeReason)));
        }
    }

    /* --------------------------------------------------------------------- */
    // Close
    /* --------------------------------------------------------------------- */

    public void closeConnection(Connection connection, CloseReason closeReason) {
        log.debug("Got called closeConnection for {}, closeReason={}", connection, closeReason);
        connection.shutdown(closeReason);
    }

    public CompletableFuture<Connection> closeConnectionGracefullyAsync(Connection connection,
                                                                        CloseReason closeReason) {
        connection.stopListening();
        return sendAsync(new CloseConnectionMessage(closeReason), connection)
                .orTimeout(100, TimeUnit.MILLISECONDS)
                .whenComplete((con, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to send close message: {}", ExceptionUtil.getRootCauseMessage(throwable));
                        connection.shutdown(closeReason);
                    } else {
                        connection.shutdown(CloseReason.CLOSE_MSG_SENT.details(closeReason.name()));
                    }
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown {}", this);
        if (isShutdown()) {
            return CompletableFuture.completedFuture(true);
        }
        setState(State.STOPPING);

        server.ifPresent(Server::shutdown);
        connectionHandshakes.values().forEach(ConnectionHandshake::shutdown);
        Stream<CompletableFuture<Connection>> futures = getAllConnections()
                .map(connection -> closeConnectionGracefullyAsync(connection, CloseReason.SHUTDOWN));
        return CompletableFutureUtils.allOf(futures)
                .orTimeout(10, SECONDS)
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        log.warn("Exception at node shutdown {}", ExceptionUtil.getRootCauseMessage(throwable));
                    }
                    outboundConnectionsByAddress.clear();
                    inboundConnectionsByAddress.clear();
                    listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onShutdown(this)));
                    listeners.clear();
                    setState(State.TERMINATED);

                    synchronized (executorLock) {
                        if (executor != null) {
                            ExecutorFactory.shutdownAndAwaitTermination(executor);
                            executor = null;
                        }
                    }
                })
                .handle((list, throwable) -> throwable == null);
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return transportService.getSocksProxy();
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

    public boolean notMyself(Address address) {
        return findMyAddress().stream().noneMatch(myAddress -> myAddress.equals(address));
    }

    public boolean isInitialized() {
        return getState().get() == Node.State.RUNNING;
    }

    @Override
    public String toString() {
        return findMyAddress().map(address -> "Node with address " + address)
                .orElseGet(() -> "Node with networkId " + networkId.getInfo());
    }

    public String getNodeInfo() {
        return getNetworkId().getInfo() + " @ " + getTransportType().name();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void handleException(Connection connection, Throwable exception) {
        log.debug("Got called handleException. connection={}, exception={}", connection, exception.getMessage());
        if (isShutdown()) {
            return;
        }
        if (connection.isRunning()) {
            handleException(exception);
        }
    }

    private void handleException(Throwable exception) {
        log.debug("Got called handleException. exception={}", exception.getMessage());

        if (isShutdown()) {
            return;
        }
        String msg = "Exception:";

        if (exception instanceof EOFException) {
            log.info("Exception: {}", ExceptionUtil.getRootCauseMessage(exception));
        } else if (exception instanceof ConnectException) {
            log.debug(msg, exception);
        } else if (exception instanceof SocketException) {
            log.debug(msg, exception);
        } else if (exception instanceof UnknownHostException) {
            log.warn("UnknownHostException. Might happen if we try to connect to wrong network type.", exception);
        } else if (exception instanceof SocketTimeoutException) {
            log.info("SocketTimeoutException: {}", ExceptionUtil.getRootCauseMessage(exception));
        } else if (exception instanceof IOException) {
            log.info("IOException: {}", ExceptionUtil.getRootCauseMessage(exception));
        } else if (exception instanceof ConnectionException connectionException) {
            if (connectionException.getCause() instanceof SocketTimeoutException) {
                handleException(connectionException.getCause());
                return;
            }
            if (connectionException.getReason() != null) {
                switch (connectionException.getReason()) {
                    case UNSPECIFIED:
                        log.error("Unspecified connectionException reason. {}", msg, exception);
                        break;
                    case INVALID_NETWORK_VERSION:
                        log.warn(msg, exception);
                        break;
                    case PROTOBUF_IS_NULL:
                        // This is usually the case when inputStream reach EOF
                        log.debug("Exception: {}", ExceptionUtil.getRootCauseMessage(exception));
                        break;
                    case AUTHORIZATION_FAILED:
                    case ONION_ADDRESS_VERIFICATION_FAILED:
                    case ADDRESS_BANNED:
                        log.warn(msg, exception);
                        break;
                    default:
                        log.error("Unhandled connectionException reason. {}", msg, exception);
                }
            }
        } else {
            log.error("Unhandled exception type {}", msg, exception);
        }
    }

    private void setState(State newState) {
        log.info("Set new state {} for networkId {}; transportType {}", newState, networkId, transportType);
        checkArgument(newState.ordinal() > state.get().ordinal(),
                "New state %s must have a higher ordinal as the current state %s. networkId={}",
                newState, state.get(), networkId);
        state.set(newState);
        observableState.set(newState);
        listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onStateChange(newState)));
    }

    private boolean isShutdown() {
        return getState().get() == STOPPING || getState().get() == TERMINATED;
    }

    private void closeIfOrphanedConnection(EnvelopePayloadMessage envelopePayloadMessage, Connection connection) {
        if (findConnection(connection).isEmpty()) {
            log.warn("We got handleNetworkMessage called from an orphaned connection which is not managed by our\n" +
                            "outboundConnectionsByAddress or inboundConnectionsByAddress maps.\n" +
                            "We close that connection to avoid memory leaks.\n" +
                            "envelopePayloadMessage={} connection={}",
                    StringUtils.truncate(envelopePayloadMessage), connection);
            connection.shutdown(CloseReason.ORPHANED_CONNECTION);
        }
    }


    /* --------------------------------------------------------------------- */
    // Not used yet
    /* --------------------------------------------------------------------- */

    void onNewIncomingConnection(InboundConnectionChannel inboundConnectionChannel) {
        // Not used yet and inboundConnectionChannel is not matching expected type in the listener
       /* try {
            inboundConnectionsByAddress.put(inboundConnectionChannel.getPeerAddress(), inboundConnectionChannel);
            listeners.forEach(listener -> DISPATCHER.submit(() -> listener.onConnection(inboundConnectionChannel)));
        } catch (Throwable throwable) {
            try {
                inboundConnectionChannel.getNetworkEnvelopeSocketChannel().close();
            } catch (IOException ignore) {
            }

            handleException(throwable);
        }*/
    }

    // Called by Inbound/Outbound ConnectionsManagers which are not used yet.
    void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage,
                              AuthorizationToken authorizationToken,
                              ConnectionChannel connection) {
        if (isShutdown()) {
            return;
        }
        String myAddress = findMyAddress().orElseThrow().getFullAddress();
        boolean isAuthorized = authorizationService.isAuthorized(envelopePayloadMessage,
                authorizationToken,
                networkLoadSnapshot.getCurrentNetworkLoad(),
                networkLoadSnapshot.getPreviousNetworkLoad(),
                connection.getId(),
                myAddress);
        if (isAuthorized) {
            if (envelopePayloadMessage instanceof CloseConnectionMessage closeConnectionMessage) {
                log.debug("Received CloseConnectionMessage from {} with reason: {}", connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
                // closeConnection(connection, CloseReason.CLOSE_MSG_RECEIVED.details(closeConnectionMessage.getCloseReason().name()));
            } else {
                // We got called from Connection on the dispatcher thread, so no mapping needed here.
                connection.notifyListeners(envelopePayloadMessage);
            }
        } else {
            //todo (Critical) should we add the connection to the ban list in that case or close the connection?
            log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(envelopePayloadMessage.toString()));
        }
    }

    private ThreadPoolExecutor createExecutor() {
        MaxSizeAwareQueue queue = new MaxSizeAwareQueue(100);
        // We use maxNumConnectedPeers (default 12) for the max pool size and add some extra tolerance as at startup we
        // create many connections in parallel.
        // After startup, it is expected that pool shrinks to 1-3 threads
        int maximumPoolSize = config.getMaxNumConnectedPeers() + 4;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                maximumPoolSize,
                5,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactoryWithCounter("Node-" + printAddresses()),
                new AbortPolicyWithLogging());
        queue.setExecutor(executor);
        return executor;
    }

    private synchronized ThreadPoolExecutor getExecutor() {
        synchronized (executorLock) {
            if (executor == null) {
                executor = createExecutor();
            }
        }
        return executor;
    }

    private String printAddresses() {
        String address = getNetworkId().getAddressByTransportTypeMap().get(transportType).toString();
        return transportType.name() + "-" + StringUtils.truncate(address, 20, StringUtils.UNICODE_ELLIPSIS);
    }
}
