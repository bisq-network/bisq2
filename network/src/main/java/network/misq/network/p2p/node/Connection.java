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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.StringUtils;
import network.misq.network.p2p.message.Envelope;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.message.Version;
import network.misq.network.p2p.node.authorization.AuthorizedMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

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
        void onMessage(Message messag, Connection connection);

        void onConnectionClosed(Connection connection);
    }

    public interface Listener {
        void onMessage(Message message);

        void onConnectionClosed();
    }


    @Getter
    protected final String id = UUID.randomUUID().toString();
    @Getter
    private final Capability peersCapability;
    @Getter
    private final Load peersLoad;
    @Getter
    private final Metrics metrics;

    private final Socket socket;
    private final Handler handler;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private ExecutorService writeExecutor;
    private ExecutorService readExecutor;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    @Getter
    private volatile boolean isStopped;

    protected Connection(Socket socket,
                         Capability peersCapability,
                         Load peersLoad,
                         Handler handler) {
        this.socket = socket;
        this.peersCapability = peersCapability;
        this.peersLoad = peersLoad;
        this.handler = handler;
        metrics = new Metrics();
    }

    void startListen(Consumer<Exception> errorHandler) {
        writeExecutor = ExecutorFactory.getSingleThreadExecutor("Connection.outputExecutor-" + getThreadNameId());
        readExecutor = ExecutorFactory.getSingleThreadExecutor("Connection.inputHandler-" + getThreadNameId());

        // TODO java serialisation is just for dev, will be replaced by custom serialisation
        // Type-Length-Value Format is considered to be used:
        // https://github.com/lightningnetwork/lightning-rfc/blob/master/01-messaging.md#type-length-value-format
        // ObjectOutputStream need to be set before objectInputStream otherwise we get blocked...
        // https://stackoverflow.com/questions/14110986/new-objectinputstream-blocks/14111047
        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            log.error("Could not create objectOutputStream/objectInputStream", e);
            errorHandler.accept(e);
        }
        readExecutor.execute(() -> {
            try {
                while (isNotStopped()) {
                    Object msg = objectInputStream.readObject();
                    if (isNotStopped()) {
                        String simpleName = msg.getClass().getSimpleName();
                        if (!(msg instanceof Envelope envelope)) {
                            throw new ConnectionException("Received message not type of Envelope. " + simpleName);
                        }
                        if (envelope.getVersion() != Version.VERSION) {
                            throw new ConnectionException("Invalid network version. " + simpleName);
                        }
                        log.debug("Received message: {} at: {}", envelope, toString());
                        metrics.received(envelope.getPayload());
                        handler.onMessage(envelope.getPayload(), this);
                    }
                }
            } catch (Exception exception) {
                //todo StreamCorruptedException from i2p at shutdown. prob it send some text data at shut down
                if (!isStopped) {
                    log.debug("Call shutdown from startListen read handler {} due exception={}", this, exception.toString());
                    shutdown();
                    // EOFException expected if connection got closed
                    if (!(exception instanceof EOFException)) {
                        errorHandler.accept(exception);
                    }
                }
            }
        });
    }

    CompletableFuture<Connection> send(AuthorizedMessage message) {
        if (isStopped) {
            log.warn("Message not sent as connection has been shut down already. Message={}, Connection={}",
                    StringUtils.truncate(message.toString(), 200), this);
            throw new CompletionException(new ConnectionClosedException(this));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Envelope envelope = new Envelope(message);
                objectOutputStream.writeObject(envelope);
                objectOutputStream.flush();
                metrics.sent(message);
                log.debug("Sent {} from {}",
                        StringUtils.truncate(message.toString(), 300), this);
                return this;
            } catch (IOException exception) {
                if (!isStopped) {
                    log.debug("Call shutdown from send {} due exception={}", this, exception.toString());
                    shutdown();
                }
                // We wrap any exception (also expected EOFException in case of connection close), to inform the caller 
                // that the "send message" intent failed.
                throw new CompletionException(exception);
            }
        }, writeExecutor);
    }

    CompletableFuture<Void> shutdown() {
        if (isStopped) {
            log.debug("Shut down already in progress {}", this);
            return CompletableFuture.completedFuture(null);
        }
        log.debug("Shut down {}", this);
        isStopped = true;
        handler.onConnectionClosed(this);
        listeners.forEach(Connection.Listener::onConnectionClosed);
        listeners.clear();
        return CompletableFuture.runAsync(() -> {
            ExecutorFactory.shutdownAndAwaitTermination(readExecutor, 1, TimeUnit.SECONDS);
            ExecutorFactory.shutdownAndAwaitTermination(writeExecutor, 1, TimeUnit.SECONDS);
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        });
    }

    void notifyListeners(Message message) {
        listeners.forEach(listener -> listener.onMessage(message));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Address getPeerAddress() {
        return peersCapability.address();
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
        return "'" + getClass().getSimpleName() + " [peerAddress=" + getPeersCapability().address() +
                ", socket=" + socket +
                ", id=" + getId() + "]'";
    }

    private String getThreadNameId() {
        return getPeersCapability().address().toString() + "-" + id.substring(0, 8);
    }

    private boolean isNotStopped() {
        return !isStopped && !Thread.currentThread().isInterrupted();
    }

    abstract public boolean isPeerAddressVerified();
}
