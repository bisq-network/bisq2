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
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.authorization.AuthorizationToken;
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
 * Notifies messageListeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class Connection {
    interface Handler {
        void handleNetworkMessage(NetworkMessage networkMessage, AuthorizationToken authorizationToken, Connection connection);

        void handleConnectionClosed(Connection connection, CloseReason closeReason);
    }

    public interface Listener {
        void onNetworkMessage(NetworkMessage networkMessage);

        void onConnectionClosed(CloseReason closeReason);
    }

    @Getter
    protected final String id = StringUtils.createUid();
    @Getter
    private final Capability peersCapability;
    @Getter
    private final Load peersLoad;
    @Getter
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel;
    @Getter
    private final Metrics metrics;

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    @Getter
    private volatile boolean isStopped;
    private volatile boolean listeningStopped;
    @Getter
    private final AtomicInteger sentMessageCounter = new AtomicInteger(0);
    private final Object writeLock = new Object();

    protected Connection(Capability peersCapability,
                         Load peersLoad,
                         NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel,
                         Metrics metrics) {
        this.peersCapability = peersCapability;
        this.peersLoad = peersLoad;
        this.networkEnvelopeSocketChannel = networkEnvelopeSocketChannel;
        this.metrics = metrics;
    }

    Connection send(NetworkMessage networkMessage, AuthorizationToken authorizationToken) {
        if (isStopped) {
            log.warn("Message not sent as connection has been shut down already. Message={}, Connection={}",
                    StringUtils.truncate(networkMessage.toString(), 200), this);
            throw new ConnectionClosedException(this);
        }
        try {
            NetworkEnvelope networkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, authorizationToken, networkMessage);
            boolean sent = false;
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
                metrics.onSent(networkEnvelope);
                if (networkMessage instanceof CloseConnectionMessage) {
                    log.info("Sent {} from {}",
                            StringUtils.truncate(networkMessage.toString(), 300), this);
                } else {
                    log.debug("Sent {} from {}",
                            StringUtils.truncate(networkMessage.toString(), 300), this);
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
            listeners.forEach(listener -> listener.onConnectionClosed(closeReason));
            listeners.clear();
        });
    }

    void notifyListeners(NetworkMessage networkMessage) {
        listeners.forEach(listener -> listener.onNetworkMessage(networkMessage));
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
        return this instanceof OutboundConnection;
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

    abstract public boolean isPeerAddressVerified();
}
