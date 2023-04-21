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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static bisq.network.NetworkService.DISPATCHER;
import static bisq.network.NetworkService.NETWORK_IO_POOL;
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
public class Node implements Connection.Handler {
    public static final String DEFAULT = "default";

    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    public interface Listener {
        void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId);

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

    private final Transport transport;
    private final AuthorizationService authorizationService;
    @Getter
    private final String nodeId;
    @Getter
    private final Transport.Type transportType;
    private final PeerConnectionsManager peerConnectionsManager;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final RetryPolicy<Boolean> retryPolicy;
    @Getter
    public AtomicReference<State> state = new AtomicReference<>(State.NEW);
    @Getter
    public Observable<State> observableState = new Observable<>(State.NEW);

    public Node(BanList banList,
                Config config,
                String nodeId) {
        transportType = config.getTransportType();
        transport = getTransport(transportType, config.getTransportConfig());
        authorizationService = config.getAuthorizationService();
        this.nodeId = nodeId;

        peerConnectionsManager = new PeerConnectionsManager(config, nodeId, banList, authorizationService, transportType, transport);

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

    public void initialize(int port) {
        Failsafe.with(retryPolicy).run(() -> doInitialize(port));
    }

    private void doInitialize(int port) {
        synchronized (state) {
            switch (state.get()) {
                case NEW: {
                    setState(STARTING);
                    transport.initialize();
                    peerConnectionsManager.start(this, port);
                    setState(State.RUNNING);
                    break;
                }
                case STARTING: {
                    throw new IllegalStateException("Already starting. NodeId=" + nodeId + "; transportType=" + transportType);
                }
                case RUNNING: {
                    log.debug("Got called while already running. We ignore that call.");
                    break;
                }
                case STOPPING:
                    throw new IllegalStateException("Already stopping. NodeId=" + nodeId + "; transportType=" + transportType);
                case TERMINATED:
                    throw new IllegalStateException("Already terminated. NodeId=" + nodeId + "; transportType=" + transportType);
                default: {
                    throw new IllegalStateException("Unhandled state " + state.get());
                }
            }
        }
    }

    public void onNewConnection(Connection connection) {
        try {
            DISPATCHER.submit(() -> listeners.forEach(listener -> listener.onConnection(connection)));
        } catch (Throwable throwable) {
            try {
                connection.getNetworkEnvelopeSocketChannel().close();
            } catch (IOException ignore) {
            }

            handleException(throwable);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Connection> sendAsync(NetworkMessage networkMessage, Address address) {
        return getConnectionAsync(address)
                .thenApplyAsync(connection -> sendAsync(networkMessage, connection), NETWORK_IO_POOL);
    }

    public Connection sendAsync(NetworkMessage networkMessage, Connection connection) {
        if (connection.isStopped()) {
            throw new ConnectionClosedException(connection);
        }
        try {
            AuthorizationToken token = authorizationService.createToken(networkMessage,
                    connection.getPeersLoad(),
                    connection.getPeerAddress().getFullAddress(),
                    connection.getSentMessageCounter().incrementAndGet());
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

    public CompletableFuture<? extends Connection> getConnectionAsync(Address address) {
        return peerConnectionsManager.getConnection(address);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OutboundConnection
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<Connection> getAllConnections() {
        return peerConnectionsManager.getAllConnections();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection.Handler
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleNetworkMessage(NetworkMessage networkMessage, AuthorizationToken authorizationToken, Connection connection) {
        if (isShutdown()) {
            return;
        }
        String myAddress = findMyAddress().orElseThrow().getFullAddress();
        boolean isAuthorized = authorizationService.isAuthorized(networkMessage,
                authorizationToken,
                getMyLoad(),
                connection.getId(),
                myAddress);
        if (isAuthorized) {
            if (networkMessage instanceof CloseConnectionMessage) {
                CloseConnectionMessage closeConnectionMessage = (CloseConnectionMessage) networkMessage;
                log.debug("Node {} received CloseConnectionMessage from {} with reason: {}", this, connection.getPeerAddress(), closeConnectionMessage.getCloseReason());
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
        // TODO
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Close
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void closeConnection(Connection connection, CloseReason closeReason) {
        log.debug("Node {} got called closeConnection for {}", this, connection);
        connection.close(closeReason);
        NetworkService.DISPATCHER.submit(() -> {
            listeners.forEach(listener -> listener.onDisconnect(connection, closeReason));
        });
    }

    public CompletableFuture<Void> closeConnectionGracefullyAsync(Connection connection, CloseReason closeReason) {
        return runAsync(() -> closeConnectionGracefully(connection, closeReason), NETWORK_IO_POOL);
    }

    public void closeConnectionGracefully(Connection connection, CloseReason closeReason) {
        try {
            connection.stopListening();
            sendAsync(new CloseConnectionMessage(closeReason), connection);

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

        peerConnectionsManager.shutdown();
        Stream<CompletableFuture<Void>> futures = getAllConnections()
                .map(connection -> closeConnectionGracefullyAsync(connection, CloseReason.SHUTDOWN));
        return CompletableFutureUtils.allOf(futures)
                .orTimeout(10, SECONDS)
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        log.warn("Exception at node shutdown", throwable);
                    }
                    transport.shutdown();
                    listeners.forEach(listener -> listener.onShutdown(this));
                    listeners.clear();
                    setState(State.TERMINATED);
                })
                .handle((list, throwable) -> throwable == null);
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
        return peerConnectionsManager.findMyAddress();
    }

    public int getNumConnections() {
        return peerConnectionsManager.getNumConnections();
    }

    public Collection<InboundConnection> getInboundConnections() {
        return peerConnectionsManager.getInboundConnections();
    }

    public Collection<OutboundConnection> getOutboundConnections() {
        return peerConnectionsManager.getOutboundConnections();
    }

    public boolean isInitialized() {
        return getState().get() == Node.State.RUNNING;
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
        log.info("Set new state {} for nodeId {}; transportType {}", newState, nodeId, transportType);
        checkArgument(newState.ordinal() > state.get().ordinal(),
                "New state %s must have a higher ordinal as the current state %s. nodeId={}",
                newState, state.get(), nodeId);
        state.set(newState);
        observableState.set(newState);
        listeners.forEach(listener -> listener.onStateChange(newState));
    }

    private boolean isShutdown() {
        return getState().get() == STOPPING || getState().get() == TERMINATED;
    }
}
