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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.StringUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Envelope;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.message.Version;
import network.misq.network.p2p.node.authorization.AuthorizationService;
import network.misq.network.p2p.node.authorization.AuthorizationToken;
import network.misq.network.p2p.services.peergroup.BanList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * At initial connection we exchange capabilities and require a valid AuthorizationToken (e.g. PoW).
 * The Client sends a Request and awaits for the servers Response.
 * The server awaits the Request and sends the Response.
 */
@Slf4j
class ConnectionHandshake {
    @Getter
    private final String id = UUID.randomUUID().toString();
    private final Socket socket;
    private final BanList banList;
    private final Capability capability;
    private final AuthorizationService authorizationService;

    private static record Request(AuthorizationToken token, Capability capability, Load load) implements Message {
    }

    private static record Response(AuthorizationToken token, Capability capability, Load load) implements Message {
    }

    static record Result(Capability capability, Load load) {
    }

    ConnectionHandshake(Socket socket, BanList banList, int socketTimeout, Capability capability, AuthorizationService authorizationService) {
        this.socket = socket;
        this.banList = banList;
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
    CompletableFuture<Result> start(Load myLoad) {
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("ConnectionHandshake.start-" + StringUtils.truncate(capability.address().toString()));
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                AuthorizationToken token = authorizationService.createToken(Request.class).get();
                Envelope requestEnvelope = new Envelope(new Request(token, capability, myLoad), Version.VERSION);
                log.debug("Client sends {}", requestEnvelope);
                objectOutputStream.writeObject(requestEnvelope);
                objectOutputStream.flush();

                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object msg = objectInputStream.readObject();
                if (!(msg instanceof Envelope responseEnvelope)) {
                    throw new ConnectionException("Received message not type of Envelope. " + msg.getClass().getSimpleName());
                }
                log.debug("Client received {}", msg);
                if (responseEnvelope.version() != Version.VERSION) {
                    throw new ConnectionException("Invalid version. responseEnvelope.version()=" +
                            responseEnvelope.version() + "; Version.VERSION=" + Version.VERSION);
                }
                if (!(responseEnvelope.payload() instanceof Response response)) {
                    throw new ConnectionException("ResponseEnvelope.payload() not type of Response. responseEnvelope=" +
                            responseEnvelope);
                }
                if (banList.isBanned(response.capability().address())) {
                    throw new ConnectionException("Peers address is in quarantine. response=" + response);
                }
                if (!authorizationService.isAuthorized(response.token())) {
                    throw new ConnectionException("Response authorization failed. response=" + response);
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
        }, NetworkService.NETWORK_IO_POOL);
    }

    // Server side protocol
    CompletableFuture<Result> onSocket(Load myLoad) {
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("ConnectionHandshake.onSocket-" + StringUtils.truncate(capability.address().toString()));
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object msg = objectInputStream.readObject();
                if (!(msg instanceof Envelope requestEnvelope)) {
                    throw new ConnectionException("Received message not type of Envelope. Received data=" + msg.getClass().getSimpleName());
                }
                log.debug("Server received {}", msg);
                if (requestEnvelope.version() != Version.VERSION) {
                    throw new ConnectionException("Invalid version. requestEnvelop.version()=" +
                            requestEnvelope.version() + "; Version.VERSION=" + Version.VERSION);
                }
                if (!(requestEnvelope.payload() instanceof Request request)) {
                    throw new ConnectionException("RequestEnvelope.payload() not type of Request. requestEnvelope=" +
                            requestEnvelope);
                }
                if (banList.isBanned(request.capability().address())) {
                    throw new ConnectionException("Peers address is in quarantine. request=" + request);
                }
                if (!authorizationService.isAuthorized(request.token())) {
                    throw new ConnectionException("Request authorization failed. request=" + request);
                }
                log.debug("Clients capability {}, load={}", request.capability(), request.load());

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                AuthorizationToken token = authorizationService.createToken(Response.class).get();
                objectOutputStream.writeObject(new Envelope(new Response(token, capability, myLoad), Version.VERSION));
                objectOutputStream.flush();

                return new Result(request.capability(), request.load());
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                if (e instanceof ConnectionException connectionException) {
                    throw connectionException;
                } else {
                    throw new ConnectionException(e);
                }
            }
        }, NetworkService.NETWORK_IO_POOL);
    }

    CompletableFuture<Void> shutdown() {
        try {
            socket.close();
        } catch (IOException ignore) {
        }
        return CompletableFuture.completedFuture(null);
    }
}