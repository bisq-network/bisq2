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


import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.TransportConfig;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.ServerSocketResult;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peergroup.BanList;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.EqualsAndHashCode;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static bisq.network.NetworkService.DISPATCHER;
import static bisq.network.p2p.node.Node.State.*;
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
        private final TransportConfig transportConfig;
        private final int socketTimeout;

        public Config(TransportType transportType,
                      Set<TransportType> supportedTransportTypes,
                      TransportConfig transportConfig,
                      int socketTimeout) {
            this.transportType = transportType;
            this.supportedTransportTypes = supportedTransportTypes;
            this.transportConfig = transportConfig;
            this.socketTimeout = socketTimeout;
        }
    }

    private final BanList banList;
    private final TransportService transportService;
    private final AuthorizationService authorizationService;
    private final Config config;
    @Getter
    private final boolean isDefaultNode;
    @EqualsAndHashCode.Include
    @Getter
    private final TransportType transportType;
    @EqualsAndHashCode.Include
    @Getter
    private final NetworkId networkId;
    @Getter
    private final TorIdentity torIdentity;
    @Getter
    private final Map<Address, OutboundConnection> outboundConnectionsByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Map<Address, InboundConnection> inboundConnectionsByAddress = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, ConnectionHandshake> connectionHandshakes = new ConcurrentHashMap<>();
    private final RetryPolicy<Boolean> retryPolicy;
    private Optional<Server> server = Optional.empty();
    private Optional<Capability> myCapability = Optional.empty();
    @Getter
    public final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    @Getter
    public final Observable<State> observableState = new Observable<>(State.NEW);
    @Getter
    public final NetworkLoadService networkLoadService;

    public Node(NetworkId networkId,
                TorIdentity torIdentity,
                boolean isDefaultNode,
                Config config,
                BanList banList,
                TransportService transportService,
                NetworkLoadService networkLoadService,
                AuthorizationService authorizationService) {
        this.isDefaultNode = isDefaultNode;
        transportType = config.getTransportType();
        this.networkId = networkId;
        this.torIdentity = torIdentity;
        this.config = config;
        this.banList = banList;
        this.transportService = transportService;
        this.authorizationService = authorizationService;
        this.networkLoadService = networkLoadService;

        retryPolicy = RetryPolicy.<Boolean>builder()
                .handle(IllegalStateException.class)
                .handleResultIf(result -> state.get() == STARTING)
                .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(20))
                .withJitter(0.25)
                .withMaxDuration(Duration.ofMinutes(5)).withMaxRetries(20)
                .onRetry(e -> log.info("Retry to call initializeServer. AttemptCount={}.", e.getAttemptCount()))
                .onRetriesExceeded(e -> {
                    log.warn("InitializeServer failed. Max retries exceeded. We shutdown the node.");
                    shutdown();
                })
                .onSuccess(e -> log.debug("InitializeServer succeeded."))
                .build();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void initialize() {
        Failsafe.with(retryPolicy).run(this::doInitialize);
    }

    private void doInitialize() {
        synchronized (state) {
            switch (state.get()) {
                case NEW: {
                    setState(STARTING);
                    createServerAndListen();
                    setState(State.RUNNING);
                    break;
                }
                case STARTING: {
                    throw new IllegalStateException("Already starting. NetworkId=" + networkId + "; transportType=" + transportType);
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
    }

    private void createServerAndListen() {
        ServerSocketResult serverSocketResult = transportService.getServerSocket(networkId, torIdentity);
        myCapability = Optional.of(new Capability(serverSocketResult.getAddress(), new ArrayList<>(config.getSupportedTransportTypes())));
        server = Optional.of(new Server(serverSocketResult,
                socket -> onClientSocket(socket, serverSocketResult, myCapability.get()),
                exception -> {
                    handleException(exception);
                    // If server fails we shut down the node
                    shutdown();
                }));
    }

    private void onClientSocket(Socket socket, ServerSocketResult serverSocketResult, Capability myCapability) {
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket,
                banList,
                config.getSocketTimeout(),
                myCapability,
                authorizationService,
                torIdentity);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Inbound handshake request at: {}", myCapability.getAddress());
        try {
            ConnectionHandshake.Result result = connectionHandshake.onSocket(networkLoadService.getCurrentNetworkLoad()); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());

            Address address = result.getCapability().getAddress();
            log.debug("Inbound handshake completed: Initiated by {} to {}", address, myCapability.getAddress());

            // As time passed we check again if connection is still not available
            if (inboundConnectionsByAddress.containsKey(address)) {
                log.warn("Node {} have already an InboundConnection from {}. This can happen when a " + "handshake was in progress while we received a new connection from that address. " + "We will close the socket of that new connection and use the existing instead.", this, address);
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                return;
            }

            InboundConnection connection = new InboundConnection(socket,
                    serverSocketResult,
                    result.getCapability(),
                    new NetworkLoadService(result.getPeersNetworkLoad()),
                    result.getConnectionMetrics(),
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

    public void onNewIncomingConnection(InboundConnectionChannel inboundConnectionChannel) {
        try {
            // inboundConnectionsByAddress.put(inboundConnectionChannel.getPeerAddress(), inboundConnectionChannel);
            // DISPATCHER.submit(() -> listeners.forEach(listener -> listener.onConnection(inboundConnectionChannel)));
        } catch (Throwable throwable) {
            try {
                inboundConnectionChannel.getNetworkEnvelopeSocketChannel().close();
            } catch (IOException ignore) {
            }

            handleException(throwable);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Connection send(EnvelopePayloadMessage envelopePayloadMessage, Address address) {
        Connection connection = getConnection(address);
        return send(envelopePayloadMessage, connection);
    }

    public Connection send(EnvelopePayloadMessage envelopePayloadMessage, Connection connection) {
        if (connection.isStopped()) {
            throw new ConnectionClosedException(connection);
        }
        try {
            AuthorizationToken token = authorizationService.createToken(envelopePayloadMessage,
                    connection.getPeersNetworkLoadService().getCurrentNetworkLoad(),
                    connection.getPeerAddress().getFullAddress(),
                    connection.getSentMessageCounter().incrementAndGet());
            return connection.send(envelopePayloadMessage, token);
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
        } else if (inboundConnectionsByAddress.containsKey(address) && (allowUnverifiedAddress || inboundConnectionsByAddress.get(address).isPeerAddressVerified())) {
            return inboundConnectionsByAddress.get(address);
        } else {
            return createOutboundConnection(address);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OutboundConnection
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Connection createOutboundConnection(Address address) {
        return myCapability.map(capability -> createOutboundConnection(address, capability)).orElseGet(() -> {
            int port = networkId.getAddressByTransportTypeMap().get(transportType).getPort();
            log.warn("We create an outbound connection but we have not initialized our server. " +
                    "We create a server on port {} now but clients better control node " +
                    "life cycle themselves.", port);
            initialize();
            checkArgument(myCapability.isPresent(), "myCapability must be present after initializeServer got called");
            return createOutboundConnection(address, myCapability.get());
        });
    }

    private Connection createOutboundConnection(Address address, Capability myCapability) {
        if (banList.isBanned(address)) {
            throw new ConnectionException("Create outbound connection failed. PeerAddress is banned. address=" + address);
        }
        Socket socket;
        try {
            socket = transportService.getSocket(address); // Blocking call
        } catch (IOException e) {
            handleException(e);
            throw new ConnectionException(e);
        }

        // As time passed we check again if connection is still not available
        if (outboundConnectionsByAddress.containsKey(address)) {
            log.warn("Node {} has already an OutboundConnection to {}. This can happen while we " + "we waited for the socket creation at the createOutboundConnection method. " + "We will close the socket and use the existing connection instead.", this, address);
            try {
                socket.close();
            } catch (IOException ignore) {
            }
            return outboundConnectionsByAddress.get(address);
        }

        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket, banList, config.getSocketTimeout(), myCapability, authorizationService, torIdentity);
        connectionHandshakes.put(connectionHandshake.getId(), connectionHandshake);
        log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.getAddress(), address);
        try {
            ConnectionHandshake.Result result = connectionHandshake.start(networkLoadService.getCurrentNetworkLoad(), address); // Blocking call
            connectionHandshakes.remove(connectionHandshake.getId());
            log.debug("Outbound handshake completed: Initiated by {} to {}", myCapability.getAddress(), address);
            log.debug("Create new outbound connection to {}", address);
            if (!address.isClearNetAddress()) {
                // For clearnet this check doesn't make sense because:
                // - the peer binds to 127.0.0.1, therefore reports 127.0.0.1 in the handshake
                // - we use the peer's public IP to connect to him
                checkArgument(address.equals(result.getCapability().getAddress()), "Peers reported address must match address we used to connect");
            }

            // As time passed we check again if connection is still not available
            if (outboundConnectionsByAddress.containsKey(address)) {
                log.warn("Node {} has already an OutboundConnection to {}. This can happen when a " + "handshake was in progress while we started a new connection to that address and as the " + "handshake was not completed we did not consider that as an available connection. " + "We will close the socket of that new connection and use the existing instead.", this, address);
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                return outboundConnectionsByAddress.get(address);
            }

            OutboundConnection connection = new OutboundConnection(socket,
                    address,
                    result.getCapability(),
                    new NetworkLoadService(result.getPeersNetworkLoad()),
                    result.getConnectionMetrics(),
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
    public void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken, Connection connection) {
        if (isShutdown()) {
            return;
        }
        String myAddress = findMyAddress().orElseThrow().getFullAddress();
        boolean isAuthorized = authorizationService.isAuthorized(envelopePayloadMessage,
                authorizationToken,
                networkLoadService.getCurrentNetworkLoad(),
                networkLoadService.getPreviousNetworkLoad(),
                connection.getId(),
                myAddress);
        if (isAuthorized) {
            if (envelopePayloadMessage instanceof CloseConnectionMessage) {
                CloseConnectionMessage closeConnectionMessage = (CloseConnectionMessage) envelopePayloadMessage;
                log.debug("Node {} received CloseConnectionMessage from {} with reason: {}", this, connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
                closeConnection(connection, CloseReason.CLOSE_MSG_RECEIVED.details(closeConnectionMessage.getCloseReason().name()));
            } else {
                // We got called from Connection on the dispatcher thread, so no mapping needed here.
                connection.notifyListeners(envelopePayloadMessage);
                listeners.forEach(listener -> listener.onMessage(envelopePayloadMessage, connection, networkId));
            }
        } else {
            //todo handle
            log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(envelopePayloadMessage.toString()));
        }
    }

    public void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken, ConnectionChannel connection) {
        if (isShutdown()) {
            return;
        }
        String myAddress = findMyAddress().orElseThrow().getFullAddress();
        boolean isAuthorized = authorizationService.isAuthorized(envelopePayloadMessage,
                authorizationToken,
                networkLoadService.getCurrentNetworkLoad(),
                networkLoadService.getPreviousNetworkLoad(),
                connection.getId(),
                myAddress);
        if (isAuthorized) {
            if (envelopePayloadMessage instanceof CloseConnectionMessage) {
                CloseConnectionMessage closeConnectionMessage = (CloseConnectionMessage) envelopePayloadMessage;
                log.debug("Node {} received CloseConnectionMessage from {} with reason: {}", this, connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
                // closeConnection(connection, CloseReason.CLOSE_MSG_RECEIVED.details(closeConnectionMessage.getCloseReason().name()));
            } else {
                // We got called from Connection on the dispatcher thread, so no mapping needed here.
                connection.notifyListeners(envelopePayloadMessage);
            }
        } else {
            //todo handle
            log.warn("Message authorization failed. authorizedMessage={}", StringUtils.truncate(envelopePayloadMessage.toString()));
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
                log.debug("Node {} did not had entry in inboundConnections at onConnectionClosed. " + "This can happen if different threads triggered a close. connection={}, peerAddress={}", this, connection, peerAddress);
            }
        } else if (connection instanceof OutboundConnection) {
            wasRemoved = outboundConnectionsByAddress.remove(peerAddress) != null;
            if (!wasRemoved) {
                log.debug("Node {} did not had entry in outboundConnections at onConnectionClosed. " + "This can happen if different threads triggered a close. connection={}, peerAddress={}", this, connection, peerAddress);
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

    public CompletableFuture<Void> closeConnectionGracefullyAsync(Connection connection, CloseReason closeReason) {
        return runAsync(() -> closeConnectionGracefully(connection, closeReason), NetworkService.NETWORK_IO_POOL);
    }

    public void closeConnectionGracefully(Connection connection, CloseReason closeReason) {
        try {
            connection.stopListening();
            send(new CloseConnectionMessage(closeReason), connection);

            // Give a bit of delay before we close the connection.
            Thread.sleep(100);
        } catch (Throwable ignore) {
        }
        connection.close(CloseReason.CLOSE_MSG_SENT.details(closeReason.name()));
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("Node {} shutdown", this);
        if (isShutdown()) {
            return CompletableFuture.completedFuture(true);
        }
        setState(State.STOPPING);

        server.ifPresent(Server::shutdown);
        connectionHandshakes.values().forEach(ConnectionHandshake::shutdown);
        Stream<CompletableFuture<Void>> futures = getAllConnections()
                .map(connection -> closeConnectionGracefullyAsync(connection, CloseReason.SHUTDOWN));
        return CompletableFutureUtils.allOf(futures)
                .orTimeout(10, SECONDS)
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        log.warn("Exception at node shutdown", throwable);
                    }
                    outboundConnectionsByAddress.clear();
                    inboundConnectionsByAddress.clear();
                    listeners.forEach(listener -> listener.onShutdown(this));
                    listeners.clear();
                    setState(State.TERMINATED);
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

    public int getNumConnections() {
        return inboundConnectionsByAddress.size() + outboundConnectionsByAddress.size();
    }

    public boolean isInitialized() {
        return getState().get() == Node.State.RUNNING;
    }

    @Override
    public String toString() {
        return findMyAddress().map(Address::toString).orElse("null");
    }

    public String getNodeInfo() {
        return getNetworkId().getInfo() + " @ " + getTransportType().name();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleException(Connection connection, Throwable exception) {
        log.debug("Node {} got called handleException. connection={}, exception={}", this, connection, exception.getMessage());
        if (isShutdown()) {
            return;
        }
        if (connection.isRunning()) {
            handleException(exception);
        }
    }

    private void handleException(Throwable exception) {
        log.debug("Node {} got called handleException. exception={}", this, exception.getMessage());

        if (isShutdown()) {
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

    private void setState(State newState) {
        log.info("Set new state {} for networkId {}; transportType {}", newState, networkId, transportType);
        checkArgument(newState.ordinal() > state.get().ordinal(),
                "New state %s must have a higher ordinal as the current state %s. networkId={}",
                newState, state.get(), networkId);
        state.set(newState);
        observableState.set(newState);
        listeners.forEach(listener -> listener.onStateChange(newState));
    }

    private boolean isShutdown() {
        return getState().get() == STOPPING || getState().get() == TERMINATED;
    }
}
