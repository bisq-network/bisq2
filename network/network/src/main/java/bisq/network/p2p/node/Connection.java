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

import bisq.common.threading.ThreadName;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.DefaultPeerSocket;
import bisq.network.common.PeerSocket;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

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
        void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                  AuthorizationToken authorizationToken,
                                  Connection connection);

        void handleConnectionClosed(Connection connection, CloseReason closeReason);
    }

    public interface Listener {
        void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage);

        void onConnectionClosed(CloseReason closeReason);
    }

    @Getter
    private final String id = StringUtils.createUid();
    @Getter
    private final Capability peersCapability;
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
    private final AtomicInteger sentMessageCounter = new AtomicInteger(0);
    private final Object writeLock = new Object();
    private volatile boolean shutdownStarted;
    private volatile boolean listeningStopped;

    protected Connection(Socket socket,
                         Capability peersCapability,
                         NetworkLoadSnapshot peersNetworkLoadSnapshot,
                         ConnectionMetrics connectionMetrics,
                         ConnectionThrottle connectionThrottle,
                         Handler handler,
                         BiConsumer<Connection, Exception> errorHandler) {
        this.peersCapability = peersCapability;
        this.peersNetworkLoadSnapshot = peersNetworkLoadSnapshot;
        this.connectionThrottle = connectionThrottle;
        this.handler = handler;
        this.connectionMetrics = connectionMetrics;
        requestResponseManager = new RequestResponseManager(connectionMetrics);

        try {
            PeerSocket peerSocket = new DefaultPeerSocket(socket);
            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(peerSocket);
        } catch (IOException exception) {
            log.error("Could not create objectOutputStream/objectInputStream for socket {}", socket, exception);
            errorHandler.accept(this, exception);
            shutdown(CloseReason.EXCEPTION.exception(exception));
            return;
        }

        inputHandlerFuture = NetworkService.NETWORK_IO_POOL.submit(() -> {
            ThreadName.set(this, "read-" + getThreadNameId());
            try {
                while (isInputStreamActive()) {
                    var proto = networkEnvelopeSocket.receiveNextEnvelope();
                    // parsing might need some time wo we check again if connection is still active
                    if (!isInputStreamActive()) {
                        return;
                    }
                    checkNotNull(proto, "Proto from NetworkEnvelope.parseDelimitedFrom(inputStream) must not be null");

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
                    NetworkService.DISPATCHER.submit(() -> {
                        if (isInputStreamActive()) {
                            handler.handleNetworkMessage(envelopePayloadMessage,
                                    networkEnvelope.getAuthorizationToken(),
                                    this);
                        }
                    });
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package scope API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Connection send(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken) {
        if (isStopped()) {
            log.warn("Message not sent as connection has been shut down already. Message={}, Connection={}",
                    StringUtils.truncate(envelopePayloadMessage.toString(), 200), this);
            // We do not throw a ConnectionClosedException here
            return this;
        }

        connectionThrottle.throttleSendMessage();

        requestResponseManager.onSent(envelopePayloadMessage);

        try {
            NetworkEnvelope networkEnvelope = new NetworkEnvelope(authorizationToken, envelopePayloadMessage);
            boolean success = false;
            long ts = System.currentTimeMillis();
            synchronized (writeLock) {
                try {
                    networkEnvelopeSocket.send(networkEnvelope);
                    success = true;
                } catch (Exception exception) {
                    if (isRunning()) {
                        throw exception;
                    } else {
                        log.info("Send message at stopped connection {} failed with {}", this, ExceptionUtil.getRootCauseMessage(exception));
                    }
                }
            }
            if (success) {
                connectionMetrics.onSent(networkEnvelope, System.currentTimeMillis() - ts);
                if (envelopePayloadMessage instanceof CloseConnectionMessage) {
                    log.info("Sent {} from {}",
                            StringUtils.truncate(envelopePayloadMessage.toString(), 300), this);
                } else {
                    log.debug("Sent {} from {}",
                            StringUtils.truncate(envelopePayloadMessage.toString(), 300), this);
                }
            }
            return this;
        } catch (IOException exception) {
            if (isRunning()) {
                log.warn("Send message at {} failed with {}", this, ExceptionUtil.getRootCauseMessage(exception));
                shutdown(CloseReason.EXCEPTION.exception(exception));
            }
            // We wrap any exception (also expected EOFException in case of connection close), to leave handling of the exception to the caller.
            throw new ConnectionException(exception);
        }
    }

    void stopListening() {
        listeningStopped = true;
    }

    void shutdown(CloseReason closeReason) {
        if (isStopped()) {
            log.debug("Shut down already in progress {}", this);
            return;
        }
        log.info("Close {}; \ncloseReason: {}", this, closeReason);
        shutdownStarted = true;
        requestResponseManager.dispose();
        connectionMetrics.clear();
        if (inputHandlerFuture != null) {
            inputHandlerFuture.cancel(true);
        }
        try {
            networkEnvelopeSocket.close();
        } catch (IOException ignore) {
        }
        NetworkService.DISPATCHER.submit(() -> {
            handler.handleConnectionClosed(this, closeReason);
            listeners.forEach(listener -> {
                try {
                    listener.onConnectionClosed(closeReason);
                } catch (Exception e) {
                    log.error("Calling onConnectionClosed at listener {} failed", listener, e);
                }
            });
            listeners.clear();
        });
    }

    void notifyListeners(EnvelopePayloadMessage envelopePayloadMessage) {
        listeners.forEach(listener -> {
            try {
                listener.onNetworkMessage(envelopePayloadMessage);
            } catch (Exception e) {
                log.error("Calling onNetworkMessage at listener {} failed", listener, e);
            }
        });
    }

    AtomicInteger getSentMessageCounter() {
        return sentMessageCounter;
    }

    boolean isStopped() {
        return shutdownStarted || networkEnvelopeSocket.isClosed() || Thread.currentThread().isInterrupted();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private String getThreadNameId() {
        return StringUtils.truncate(getPeerAddress().toString() + "-" + id.substring(0, 8));
    }

    private boolean isInputStreamActive() {
        return !listeningStopped && isRunning();
    }
}
