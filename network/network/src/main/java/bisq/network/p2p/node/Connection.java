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
import bisq.common.network.DefaultPeerSocket;
import bisq.common.network.PeerSocket;
import bisq.common.threading.AbortPolicyWithLogging;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.MaxSizeAwareDeque;
import bisq.common.threading.MaxSizeAwareQueue;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.NetworkExecutors;
import bisq.network.p2p.message.CloseConnectionMessage;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;


/**
 * Represents an inbound or outbound connection to a peer node.
 * Listens for messages from the peer.
 * Send messages to the peer.
 * Notifies messageListeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class Connection {
    public static Comparator<Connection> comparingDate() {
        return Comparator.comparingLong(Connection::getCreated);
    }

    public static Comparator<Connection> comparingNumPendingRequests() {
        return Comparator.comparingLong(o -> o.getRequestResponseManager().numPendingRequests());
    }

    protected interface Handler {
        boolean isMessageAuthorized(EnvelopePayloadMessage envelopePayloadMessage,
                                    AuthorizationToken authorizationToken,
                                    Connection connection);

        void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                  Connection connection);

        void handleConnectionClosed(Connection connection, CloseReason closeReason);
    }

    public interface Listener {
        void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage);

        void onConnectionClosed(CloseReason closeReason);
    }

    private final AuthorizationService authorizationService;
    @Getter
    private final String id;
    @Getter
    protected final Capability peersCapability;
    @Getter
    private final NetworkLoadSnapshot peersNetworkLoadSnapshot;
    @Getter
    private final ConnectionMetrics connectionMetrics;
    @Getter
    private final RequestResponseManager requestResponseManager;

    private NetworkEnvelopeSocket networkEnvelopeSocket;
    private final ConnectionThrottle connectionThrottle;
    private final Handler handler;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    @Nullable
    private Future<?> inputHandlerFuture;
    // We use counter value 0 in the handshake, thus we start here with 1 as it's not the first message
    @Getter(AccessLevel.PACKAGE)
    private final AtomicInteger sentMessageCounter = new AtomicInteger(1);
    private final Object writeLock = new Object();
    private volatile boolean shutdownStarted;
    private volatile boolean listeningStopped;
    private final ThreadPoolExecutor readExecutor;
    private final ThreadPoolExecutor sendExecutor;

    protected Connection(AuthorizationService authorizationService,
                         String connectionId,
                         Socket socket,
                         Capability peersCapability,
                         NetworkLoadSnapshot peersNetworkLoadSnapshot,
                         ConnectionMetrics connectionMetrics,
                         ConnectionThrottle connectionThrottle,
                         Handler handler,
                         BiConsumer<Connection, Exception> errorHandler) {
        this.authorizationService = authorizationService;
        this.id = connectionId;
        this.peersCapability = peersCapability;
        this.peersNetworkLoadSnapshot = peersNetworkLoadSnapshot;
        this.connectionThrottle = connectionThrottle;
        this.handler = handler;
        this.connectionMetrics = connectionMetrics;
        requestResponseManager = new RequestResponseManager(connectionMetrics);

        readExecutor = createReadExecutor();
        sendExecutor = createSendExecutor();

        try {
            PeerSocket peerSocket = new DefaultPeerSocket(socket);
            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(peerSocket);
        } catch (IOException exception) {
            log.error("Could not create objectOutputStream/objectInputStream for socket {}", socket, exception);
            errorHandler.accept(this, exception);
            shutdown(CloseReason.EXCEPTION.exception(exception));
            return;
        }

        inputHandlerFuture = readExecutor.submit(() -> {
            try {
                long readTs = 0;
                while (isInputStreamActive()) {
                    if (readTs != 0) {
                        log.debug("Processing message took {} ms. Wait for new message from {}. ", System.currentTimeMillis() - readTs, getPeerAddress());
                    } else {
                        log.debug("Wait for new message from {}", getPeerAddress());
                    }
                    var proto = networkEnvelopeSocket.receiveNextEnvelope();
                    readTs = System.currentTimeMillis();
                    if (proto == null) {
                        log.info("Proto from networkEnvelopeSocket.receiveNextEnvelope() is null. " +
                                "This is expected if the input stream has reached EOF. We shut down the connection.");
                        shutdown(CloseReason.EXCEPTION.exception(new EOFException("Input stream reached EOF")));
                        return;
                    }

                    // receiveNextEnvelope might need some time wo we check again if connection is still active
                    if (!isInputStreamActive()) {
                        return;
                    }

                    connectionThrottle.throttleReceiveMessage();
                    // ThrottleReceiveMessage can cause a delay by Thread.sleep
                    if (!isInputStreamActive()) {
                        return;
                    }
                    long ts = System.currentTimeMillis();
                    NetworkEnvelope networkEnvelope = NetworkEnvelope.fromProto(proto);
                    long deserializeTime = System.currentTimeMillis() - ts;
                    networkEnvelope.verifyVersion();
                    connectionMetrics.onReceived(networkEnvelope, deserializeTime);

                    EnvelopePayloadMessage envelopePayloadMessage = networkEnvelope.getEnvelopePayloadMessage();
                    log.debug("Received message: {} at: {}",
                            StringUtils.truncate(envelopePayloadMessage.toString(), 200), this);
                    requestResponseManager.onReceived(envelopePayloadMessage);

                    if (isInputStreamActive()) {
                        boolean isMessageAuthorized = handler.isMessageAuthorized(envelopePayloadMessage,
                                networkEnvelope.getAuthorizationToken(),
                                this);
                        if (isMessageAuthorized) {
                            handler.handleNetworkMessage(envelopePayloadMessage, this);
                            listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onNetworkMessage(envelopePayloadMessage)));
                        }
                    }
                }
            } catch (Exception exception) {
                //todo (deferred) StreamCorruptedException from i2p at shutdown. prob it send some text data at shut down
                if (!shutdownStarted) {
                    log.debug("Exception at input handler on {}", this, exception);
                    shutdown(CloseReason.EXCEPTION.exception(exception));

                    // EOFException expected if connection got closed (Socket closed message)
                    if (!(exception instanceof EOFException)) {
                        errorHandler.accept(this, exception);
                    }
                }
            }
        });
    }

    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Address getPeerAddress() {
        return peersCapability.getAddress();
    }

    public boolean isOutboundConnection() {
        return this instanceof OutboundConnection;
    }

    public boolean isRunning() {
        return !isStopped();
    }

    public long getCreated() {
        return getConnectionMetrics().getCreated();
    }

    public Date getCreationDate() {
        return getConnectionMetrics().getCreationDate();
    }

    public boolean createdBefore(long date) {
        return getCreated() < date;
    }

    @Override
    public String toString() {
        return "'" + getClass().getSimpleName() + " [peerAddress=" + getPeerAddress() +
                ", keyId=" + getId() + "]'";
    }


    /* --------------------------------------------------------------------- */
    // Package scope API
    /* --------------------------------------------------------------------- */

    CompletableFuture<Connection> sendAsync(EnvelopePayloadMessage envelopePayloadMessage) {
        return CompletableFuture.supplyAsync(() -> {
            if (isStopped()) {
                throw new ConnectionClosedException(this);
            }
            AuthorizationToken authorizationToken = createAuthorizationToken(envelopePayloadMessage);
            connectionThrottle.throttleSendMessage();
            if (isStopped()) {
                throw new ConnectionClosedException(this);
            }
            try {
                NetworkEnvelope networkEnvelope = createNetworkEnvelope(envelopePayloadMessage, authorizationToken);
                long ts = System.currentTimeMillis();
                synchronized (writeLock) {
                    networkEnvelopeSocket.send(networkEnvelope);
                }
                requestResponseManager.onSent(envelopePayloadMessage);
                connectionMetrics.onSent(networkEnvelope, System.currentTimeMillis() - ts);
                if (envelopePayloadMessage instanceof CloseConnectionMessage) {
                    log.info("Sent {} from {}", StringUtils.truncate(envelopePayloadMessage.toString(), 300), this);
                }
            } catch (Exception exception) {
                if (exception instanceof ConnectionException connectionException) {
                    throw connectionException;
                }
                throw new ConnectionException(exception);
            }
            return this;
        }, sendExecutor);
    }

    private NetworkEnvelope createNetworkEnvelope(EnvelopePayloadMessage envelopePayloadMessage,
                                                  AuthorizationToken authorizationToken) {
        try {
            // The verify method inside NetworkEnvelope constructor could throw an exception.
            // This would be only the case if our data we want to send is invalid.
            return new NetworkEnvelope(authorizationToken, envelopePayloadMessage);
        } catch (Exception exception) {
            if (isRunning()) {
                log.warn("Cannot create NetworkEnvelope. {}", ExceptionUtil.getRootCauseMessage(exception));
                shutdown(CloseReason.EXCEPTION.exception(exception));
            }
            // We wrap any exception (also expected EOFException in case of connection close), to leave handling of the exception to the caller.
            throw new ConnectionException(exception);
        }
    }

    private AuthorizationToken createAuthorizationToken(EnvelopePayloadMessage envelopePayloadMessage) {
        return authorizationService.createToken(envelopePayloadMessage,
                peersNetworkLoadSnapshot.getCurrentNetworkLoad(),
                getPeerAddress().getFullAddress(),
                sentMessageCounter.getAndIncrement(),
                peersCapability.getFeatures());
    }

    void stopListening() {
        listeningStopped = true;
    }

    void shutdown(CloseReason closeReason) {
        if (isStopped()) {
            log.debug("Shut down already in progress {}", this);
            return;
        }
        if (closeReason != CloseReason.SHUTDOWN) {
            log.info("Close {}; \ncloseReason: {}", this, closeReason);
        }
        shutdownStarted = true;
        requestResponseManager.dispose();
        connectionMetrics.clear();
        if (inputHandlerFuture != null) {
            inputHandlerFuture.cancel(true);
        }
        try {
            if (networkEnvelopeSocket != null) {
                networkEnvelopeSocket.close();
            }
        } catch (IOException ignore) {
        }
        handler.handleConnectionClosed(this, closeReason);
        listeners.forEach(listener -> NetworkExecutors.getNotifyExecutor().submit(() -> listener.onConnectionClosed(closeReason)));
        listeners.clear();

        ExecutorFactory.shutdownAndAwaitTermination(readExecutor);
        ExecutorFactory.shutdownAndAwaitTermination(sendExecutor);
    }

    boolean isStopped() {
        return shutdownStarted
                || networkEnvelopeSocket == null
                || networkEnvelopeSocket.isClosed()
                || Thread.currentThread().isInterrupted();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private boolean isInputStreamActive() {
        return !listeningStopped && isRunning();
    }

    private ThreadPoolExecutor createReadExecutor() {
        MaxSizeAwareDeque deque = new MaxSizeAwareDeque(100);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                3,
                5,
                TimeUnit.SECONDS,
                deque,
                ExecutorFactory.getThreadFactoryWithCounter("Connection.read-" + StringUtils.truncate(getPeerAddress(), 12)),
                new AbortPolicyWithLogging());
        deque.setExecutor(executor);
        return executor;
    }

    private ThreadPoolExecutor createSendExecutor() {
        MaxSizeAwareQueue queue = new MaxSizeAwareQueue(100);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                3,
                5,
                TimeUnit.SECONDS,
                queue,
                ExecutorFactory.getThreadFactoryWithCounter("Connection.send-" + StringUtils.truncate(getPeerAddress(), 12)),
                new AbortPolicyWithLogging());
        queue.setExecutor(executor);
        return executor;
    }
}
