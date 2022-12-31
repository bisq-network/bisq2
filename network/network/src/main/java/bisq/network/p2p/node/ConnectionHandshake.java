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
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.services.peergroup.BanList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * At initial connection we exchange capabilities and require a valid AuthorizationToken (e.g. PoW).
 * The Client sends a Request and awaits for the servers Response.
 * The server awaits the Request and sends the Response.
 */
@Slf4j
public final class ConnectionHandshake {
    @Getter
    private final String id = StringUtils.createUid();
    private final BanList banList;
    private final Capability capability;
    private final AuthorizationService authorizationService;

    private NetworkEnvelopeSocket networkEnvelopeSocket;

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request implements NetworkMessage {
        private final Capability capability;
        private final Load load;

        public Request(Capability capability, Load load) {
            this.capability = capability;
            this.load = load;
        }

        @Override
        public bisq.network.protobuf.NetworkMessage toProto() {
            return getNetworkMessageBuilder().setConnectionHandshakeRequest(
                            bisq.network.protobuf.ConnectionHandshake.Request.newBuilder()
                                    .setCapability(capability.toProto())
                                    .setLoad(load.toProto()))
                    .build();
        }

        public static Request fromProto(bisq.network.protobuf.ConnectionHandshake.Request proto) {
            return new Request(Capability.fromProto(proto.getCapability()),
                    Load.fromProto(proto.getLoad()));
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Response implements NetworkMessage {
        private final Capability capability;
        private final Load load;

        public Response(Capability capability, Load load) {
            this.capability = capability;
            this.load = load;
        }

        @Override
        public bisq.network.protobuf.NetworkMessage toProto() {
            return getNetworkMessageBuilder().setConnectionHandshakeResponse(
                            bisq.network.protobuf.ConnectionHandshake.Response.newBuilder()
                                    .setCapability(capability.toProto())
                                    .setLoad(load.toProto()))
                    .build();
        }

        public static Response fromProto(bisq.network.protobuf.ConnectionHandshake.Response proto) {
            return new Response(Capability.fromProto(proto.getCapability()),
                    Load.fromProto(proto.getLoad()));
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    static final class Result {
        private final Capability capability;
        private final Load load;
        private final Metrics metrics;

        Result(Capability capability, Load load, Metrics metrics) {
            this.capability = capability;
            this.load = load;
            this.metrics = metrics;
        }
    }

    ConnectionHandshake(Socket socket,
                        BanList banList,
                        int socketTimeout,
                        Capability capability,
                        AuthorizationService authorizationService) {
        this.banList = banList;
        this.capability = capability;
        this.authorizationService = authorizationService;

        try {
            // socket.setTcpNoDelay(true);
            // socket.setSoLinger(true, 100);
            socket.setSoTimeout(socketTimeout);

            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Client side protocol
    Result start(Load myLoad, Address peerAddress) {
        try {
            Metrics metrics = new Metrics();
            Request request = new Request(capability, myLoad);
            AuthorizationToken token = authorizationService.createToken(request,
                    Load.INITIAL_LOAD,
                    peerAddress.getFullAddress(),
                    0);
            NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
            long ts = System.currentTimeMillis();

            networkEnvelopeSocket.send(requestNetworkEnvelope);
            metrics.onSent(requestNetworkEnvelope);

            bisq.network.protobuf.NetworkEnvelope responseProto = networkEnvelopeSocket.receiveNextEnvelope();
            if (responseProto == null) {
                throw new ConnectionException("Response NetworkEnvelope protobuf is null");
            }

            NetworkEnvelope responseNetworkEnvelope = NetworkEnvelope.fromProto(responseProto);
            if (responseNetworkEnvelope.getVersion() != NetworkEnvelope.VERSION) {
                throw new ConnectionException("Invalid version. responseEnvelope.version()=" +
                        responseNetworkEnvelope.getVersion() + "; Version.VERSION=" + NetworkEnvelope.VERSION);
            }
            if (!(responseNetworkEnvelope.getNetworkMessage() instanceof Response)) {
                throw new ConnectionException("ResponseEnvelope.message() not type of Response. responseEnvelope=" +
                        responseNetworkEnvelope);
            }
            Response response = (Response) responseNetworkEnvelope.getNetworkMessage();
            if (banList.isBanned(response.getCapability().getAddress())) {
                throw new ConnectionException("Peers address is in quarantine. response=" + response);
            }

            String myAddress = capability.getAddress().getFullAddress();
            boolean isAuthorized = authorizationService.isAuthorized(response,
                    responseNetworkEnvelope.getAuthorizationToken(),
                    myLoad,
                    UUID.randomUUID().toString(),
                    myAddress);

            if (!isAuthorized) {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            metrics.onReceived(responseNetworkEnvelope);
            metrics.addRtt(System.currentTimeMillis() - ts);
            log.debug("Servers capability {}, load={}", response.getCapability(), response.getLoad());
            return new Result(response.getCapability(), response.getLoad(), metrics);
        } catch (Exception e) {
            try {
                networkEnvelopeSocket.close();
            } catch (IOException ignore) {
            }
            if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else {
                throw new ConnectionException(e);
            }
        }
    }

    // Server side protocol
    Result onSocket(Load myLoad) {
        try {
            Metrics metrics = new Metrics();
            bisq.network.protobuf.NetworkEnvelope requestProto = networkEnvelopeSocket.receiveNextEnvelope();
            if (requestProto == null) {
                throw new ConnectionException("Request NetworkEnvelope protobuf is null");
            }
            NetworkEnvelope requestNetworkEnvelope = NetworkEnvelope.fromProto(requestProto);

            long ts = System.currentTimeMillis();
            if (requestNetworkEnvelope.getVersion() != NetworkEnvelope.VERSION) {
                throw new ConnectionException("Invalid version. requestEnvelop.version()=" +
                        requestNetworkEnvelope.getVersion() + "; Version.VERSION=" + NetworkEnvelope.VERSION);
            }
            if (!(requestNetworkEnvelope.getNetworkMessage() instanceof Request)) {
                throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                        requestNetworkEnvelope);
            }
            Request request = (Request) requestNetworkEnvelope.getNetworkMessage();
            Address peerAddress = request.getCapability().getAddress();
            if (banList.isBanned(peerAddress)) {
                throw new ConnectionException("Peers address is in quarantine. request=" + request);
            }

            String myAddress = capability.getAddress().getFullAddress();
            boolean isAuthorized = authorizationService.isAuthorized(request,
                    requestNetworkEnvelope.getAuthorizationToken(),
                    Load.INITIAL_LOAD,
                    UUID.randomUUID().toString(),
                    myAddress);
            if (!isAuthorized) {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            log.debug("Clients capability {}, load={}", request.getCapability(), request.getLoad());
            metrics.onReceived(requestNetworkEnvelope);

            Response response = new Response(capability, myLoad);
            AuthorizationToken token = authorizationService.createToken(response, request.getLoad(), peerAddress.getFullAddress(), 0);
            NetworkEnvelope responseNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, response);
            networkEnvelopeSocket.send(responseNetworkEnvelope);

            metrics.onSent(responseNetworkEnvelope);
            metrics.addRtt(System.currentTimeMillis() - ts);
            return new Result(request.getCapability(), request.getLoad(), metrics);
        } catch (Exception e) {
            try {
                networkEnvelopeSocket.close();
            } catch (IOException ignore) {
            }
            if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else {
                throw new ConnectionException(e);
            }
        }
    }

    void shutdown() {
        // todo close pending requests but do not close sockets
    }
}