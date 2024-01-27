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
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an inbound or outbound connection to a peer node.
 * Listens for messages from the peer.
 * Send messages to the peer.
 * Notifies listeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class ConnectionChannel {
    interface Handler {
        void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken, ConnectionChannel connectionChannel);

        void handleConnectionClosed(ConnectionChannel connectionChannel, CloseReason closeReason);
    }

    public interface Listener {
        void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage);

        void onConnectionClosed(CloseReason closeReason);
    }

    @Getter
    protected final String id = StringUtils.createUid();
    @Getter
    private final Capability peersCapability;
    @Getter
    private final NetworkLoadSnapshot peersNetworkLoadSnapshot;
    @Getter
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel;
    @Getter
    private final ConnectionMetrics connectionMetrics;

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    @Getter
    private volatile boolean isStopped;
    private volatile boolean listeningStopped;
    @Getter
    private final AtomicInteger sentMessageCounter = new AtomicInteger(0);
    private final Object writeLock = new Object();

    protected ConnectionChannel(Capability peersCapability,
                                NetworkLoadSnapshot peersNetworkLoadSnapshot,
                                NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel,
                                ConnectionMetrics connectionMetrics) {
        this.peersCapability = peersCapability;
        this.peersNetworkLoadSnapshot = peersNetworkLoadSnapshot;
        this.networkEnvelopeSocketChannel = networkEnvelopeSocketChannel;
        this.connectionMetrics = connectionMetrics;
    }

    ConnectionChannel send(EnvelopePayloadMessage envelopePayloadMessage, AuthorizationToken authorizationToken) {
        if (isStopped) {
            log.warn("Message not sent as connection has been shut down already. Message={}, ConnectionChannel={}",
                    StringUtils.truncate(envelopePayloadMessage.toString(), 200), this);
            throw new ConnectionClosedException(this);
        }
        try {
            NetworkEnvelope networkEnvelope = new NetworkEnvelope(authorizationToken, envelopePayloadMessage);
            boolean sent = false;
            long ts = System.currentTimeMillis();
            synchronized (writeLock) {
                try {
                    networkEnvelopeSocketChannel.send(networkEnvelope);
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
        log.info("Close {}", this);
        isStopped = true;
        try {
            networkEnvelopeSocketChannel.close();
        } catch (IOException ignore) {
        }
        NetworkService.DISPATCHER.submit(() -> {
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

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Address getPeerAddress() {
        return peersCapability.getAddress();
    }

    // Only at outbound connections we can be sure that the peer address is correct.
    // The announced peer address in capability is not guaranteed to be valid.
    // For most cases that is sufficient as the peer would not gain anything if lying about their address
    // as it would make them unreachable for receiving messages from newly established connections. But there are
    // cases where we need to be sure that it is the real address, like if we might use the peer address for banning a
    // not correctly behaving peer.
    public boolean getPeerAddressVerified() {
        return isOutboundConnection();
    }

    public boolean isOutboundConnection() {
        return this instanceof OutboundConnectionChannel;
    }

    public boolean isRunning() {
        return !isStopped();
    }

    @Override
    public String toString() {
        return "'" + getClass().getSimpleName() + " [peerAddress=" + getPeersCapability().getAddress() +
                ", socket=" + networkEnvelopeSocketChannel +
                ", keyId=" + getId() + "]'";
    }

    private String getThreadNameId() {
        return StringUtils.truncate(getPeersCapability().getAddress().toString() + "-" + id.substring(0, 8));
    }

    private boolean isInputStreamActive() {
        return !listeningStopped && !isStopped && !Thread.currentThread().isInterrupted();
    }

    public abstract boolean isPeerAddressVerified();
}
