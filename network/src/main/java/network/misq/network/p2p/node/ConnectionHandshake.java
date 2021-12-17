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

package network.misq.network.p2p.node;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.network.p2p.message.Envelope;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.message.Version;
import network.misq.network.p2p.node.authorization.AuthorizationService;
import network.misq.network.p2p.node.authorization.AuthorizationToken;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * At initial connection we exchange capabilities and require a valid AuthorizationToken (e.g. PoW).
 * The Client sends a Request and awaits for the servers Response.
 * The server awaits the Request and sends the Response.
 */
@Slf4j
public class ConnectionHandshake {
    private final static AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);
    private final Socket socket;
    private final Capability capability;
    private final AuthorizationService authorizationService;

    public static record Request(AuthorizationToken token, Capability capability, Load load) implements Message {
    }

    public static record Response(AuthorizationToken token, Capability capability, Load load) implements Message {
    }

    public static record Result(Capability capability, Load load) {
    }

    public ConnectionHandshake(Socket socket, int socketTimeout, Capability capability, AuthorizationService authorizationService) {
        this.socket = socket;
        this.capability = capability;
        this.authorizationService = authorizationService;

        try {
            // socket.setTcpNoDelay(true);
            // socket.setSoLinger(true, 100);
            socket.setSoTimeout(socketTimeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // Client side protocol
    public CompletableFuture<Result> start(Load myLoad) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                AuthorizationToken token = authorizationService.createToken(Request.class).get();
                Envelope requestEnvelope = new Envelope(new Request(token, capability, myLoad));
                log.debug("Client sends {}", requestEnvelope);
                objectOutputStream.writeObject(requestEnvelope);
                objectOutputStream.flush();

                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object msg = objectInputStream.readObject();
                log.debug("Client received {}", msg);
                String simpleName = msg.getClass().getSimpleName();
                if (!(msg instanceof Envelope responseEnvelope)) {
                    throw new ConnectionException("Received message not type of Envelope. " + simpleName);
                }
                if (responseEnvelope.getVersion() != Version.VERSION) {
                    throw new ConnectionException("Invalid network version. " + simpleName);
                }
                if (!(responseEnvelope.getPayload() instanceof Response response)) {
                    throw new ConnectionException("Received envelope.payload not type of Response. " + simpleName);
                }
                if (!authorizationService.isAuthorized(response.token())) {
                    throw new ConnectionException("Response authorization failed. " + simpleName);
                }

                log.debug("Servers capability {}, load={}", response.capability(), response.load());
                return new Result(response.capability(), response.load());
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                if (e instanceof ConnectionException handShakeException) {
                    throw handShakeException;
                } else {
                    throw new ConnectionException(e);
                }
            }
        }, ExecutorFactory.getSingleThreadExecutor("ConnectionHandshake-client-" + ATOMIC_INTEGER.incrementAndGet()));
    }

    // Server side protocol
    public CompletableFuture<Result> onSocket(Load myLoad) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object msg = objectInputStream.readObject();
                log.debug("Server received {}", msg);
                String simpleName = msg.getClass().getSimpleName();
                if (!(msg instanceof Envelope requestEnvelope)) {
                    throw new ConnectionException("Received message not type of Envelope. " + simpleName);
                }
                if (requestEnvelope.getVersion() != Version.VERSION) {
                    throw new ConnectionException("Invalid network version. " + simpleName);
                }
                if (!(requestEnvelope.getPayload() instanceof Request request)) {
                    throw new ConnectionException("Received envelope.payload not type of Request. " + simpleName);
                }
                if (!authorizationService.isAuthorized(request.token())) {
                    throw new ConnectionException("Request authorization failed. " + simpleName);
                }
                log.debug("Clients capability {}, load={}", request.capability(), request.load());

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                AuthorizationToken token = authorizationService.createToken(Response.class).get();
                objectOutputStream.writeObject(new Envelope(new Response(token, capability, myLoad)));
                objectOutputStream.flush();

                return new Result(request.capability(), request.load());
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                if (e instanceof ConnectionException handShakeException) {
                    throw handShakeException;
                } else {
                    throw new ConnectionException(e);
                }
            }
        }, ExecutorFactory.getSingleThreadExecutor("ConnectionHandshake-server-" + ATOMIC_INTEGER.incrementAndGet()));
    }
}