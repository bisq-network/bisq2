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

import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
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
    protected interface Handler {
        void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken, Connection connection);

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
    private final NetworkLoadService peersNetworkLoadService;
    @Getter
    private final ConnectionMetrics connectionMetrics;

    private NetworkEnvelopeSocket networkEnvelopeSocket;
    private final Handler handler;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    @Nullable
    private Future<?> inputHandlerFuture;
    private final AtomicInteger sentMessageCounter = new AtomicInteger(0);
    private final Object writeLock = new Object();
    private volatile boolean isStopped;
    private volatile boolean listeningStopped;

    protected Connection(Socket socket,
                         Capability peersCapability,
                         NetworkLoadService peersNetworkLoadService,
                         ConnectionMetrics connectionMetrics,
                         Handler handler,
                         BiConsumer<Connection, Exception> errorHandler) {
        this.peersCapability = peersCapability;
        this.peersNetworkLoadService = peersNetworkLoadService;
        this.handler = handler;
        this.connectionMetrics = connectionMetrics;

        try {
            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(socket);
        } catch (IOException exception) {
            log.error("Could not create objectOutputStream/objectInputStream for socket " + socket, exception);
            errorHandler.accept(this, exception);
            close(CloseReason.EXCEPTION.exception(exception));
            return;
        }

        inputHandlerFuture = NetworkService.NETWORK_IO_POOL.submit(() -> {
            Thread.currentThread().setName("Connection.read-" + getThreadNameId());
            try {
                while (isInputStreamActive()) {
                    var proto = networkEnvelopeSocket.receiveNextEnvelope();
                    // parsing might need some time wo we check again if connection is still active
                    if (isInputStreamActive()) {
                        checkNotNull(proto, "Proto from NetworkEnvelope.parseDelimitedFrom(inputStream) must not be null");
                        long ts = System.currentTimeMillis();
                        NetworkEnvelope networkEnvelope = NetworkEnvelope.fromProto(proto);
                        long deserializeTime = System.currentTimeMillis() - ts;

                        networkEnvelope.verifyVersion();
                        EnvelopePayloadMessage envelopePayloadMessage = networkEnvelope.getEnvelopePayloadMessage();
                        log.debug("Received message: {} at: {}",
                                StringUtils.truncate(envelopePayloadMessage.toString(), 200), this);
                        connectionMetrics.onReceived(networkEnvelope, deserializeTime);
                        NetworkService.DISPATCHER.submit(() -> handler.handleNetworkMessage(envelopePayloadMessage,
                                networkEnvelope.getAuthorizationToken(),
                                this));
                    }
                }
            } catch (Exception exception) {
                //todo StreamCorruptedException from i2p at shutdown. prob it send some text data at shut down
                if (isInputStreamActive()) {
                    log.debug("Call shutdown from startListen read handler {} due exception={}", this, exception.toString());
                    close(CloseReason.EXCEPTION.exception(exception));
                    // EOFException expected if connection got closed
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

    @Override
    public String toString() {
        return "'" + getClass().getSimpleName() + " [peerAddress=" + getPeersCapability().getAddress() +
                ", socket=" + networkEnvelopeSocket +
                ", keyId=" + getId() + "]'";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package scope API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Connection send(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken) {
        if (isStopped) {
            log.warn("Message not sent as connection has been shut down already. Message={}, Connection={}",
                    StringUtils.truncate(envelopePayloadMessage.toString(), 200), this);
            throw new ConnectionClosedException(this);
        }
        try {
            NetworkEnvelope networkEnvelope = new NetworkEnvelope(authorizationToken, envelopePayloadMessage);
            boolean sent = false;
            long ts = System.currentTimeMillis();
            synchronized (writeLock) {
                try {
                    networkEnvelopeSocket.send(networkEnvelope);
                    sent = true;
                } catch (Throwable throwable) {
                    if (!isStopped) {
                        throw throwable;
                    }
                }
            }
            if (sent) {
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
            if (!isStopped) {
                log.error("Call shutdown from send {} due exception={}", this, exception.toString());
                close(CloseReason.EXCEPTION.exception(exception));
            }
            // We wrap any exception (also expected EOFException in case of connection close), to inform the caller 
            // that the "send proto" intent failed.
            throw new ConnectionException(exception);
        }
    }

    void stopListening() {
        listeningStopped = true;
    }

    void close(CloseReason closeReason) {
        if (isStopped) {
            log.debug("Shut down already in progress {}", this);
            return;
        }
        log.info("Close {}; \ncloseReason: {}", this, closeReason);
        isStopped = true;
        if (inputHandlerFuture != null) {
            inputHandlerFuture.cancel(true);
        }
        try {
            networkEnvelopeSocket.close();
        } catch (IOException ignore) {
        }
        NetworkService.DISPATCHER.submit(() -> {
            handler.handleConnectionClosed(this, closeReason);
            listeners.forEach(listener -> listener.onConnectionClosed(closeReason));
            listeners.clear();
        });
    }

    void notifyListeners(EnvelopePayloadMessage envelopePayloadMessage) {
        listeners.forEach(listener -> listener.onNetworkMessage(envelopePayloadMessage));
    }

    AtomicInteger getSentMessageCounter() {
        return sentMessageCounter;
    }

    boolean isStopped() {
        return isStopped;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private String getThreadNameId() {
        return StringUtils.truncate(getPeersCapability().getAddress().toString() + "-" + id.substring(0, 8));
    }

    private boolean isInputStreamActive() {
        return !listeningStopped && !isStopped && !Thread.currentThread().isInterrupted();
    }
}
