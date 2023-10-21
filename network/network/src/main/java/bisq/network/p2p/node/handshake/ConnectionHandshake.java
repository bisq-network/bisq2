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

package bisq.network.p2p.node.handshake;

import bisq.common.util.StringUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.data.ConnectionMetrics;
import bisq.network.p2p.node.data.NetworkLoad;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.p2p.vo.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;

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
        private final NetworkLoad networkLoad;

        public Request(Capability capability, NetworkLoad networkLoad) {
            this.capability = capability;
            this.networkLoad = networkLoad;
        }

        @Override
        public bisq.network.protobuf.NetworkMessage toProto() {
            return getNetworkMessageBuilder().setConnectionHandshakeRequest(
                            bisq.network.protobuf.ConnectionHandshake.Request.newBuilder()
                                    .setCapability(capability.toProto())
                                    .setNetworkLoad(networkLoad.toProto()))
                    .build();
        }

        public static Request fromProto(bisq.network.protobuf.ConnectionHandshake.Request proto) {
            return new Request(Capability.fromProto(proto.getCapability()),
                    NetworkLoad.fromProto(proto.getNetworkLoad()));
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Response implements NetworkMessage {
        private final Capability capability;
        private final NetworkLoad networkLoad;

        public Response(Capability capability, NetworkLoad networkLoad) {
            this.capability = capability;
            this.networkLoad = networkLoad;
        }

        @Override
        public bisq.network.protobuf.NetworkMessage toProto() {
            return getNetworkMessageBuilder().setConnectionHandshakeResponse(
                            bisq.network.protobuf.ConnectionHandshake.Response.newBuilder()
                                    .setCapability(capability.toProto())
                                    .setNetworkLoad(networkLoad.toProto()))
                    .build();
        }

        public static Response fromProto(bisq.network.protobuf.ConnectionHandshake.Response proto) {
            return new Response(Capability.fromProto(proto.getCapability()),
                    NetworkLoad.fromProto(proto.getNetworkLoad()));
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Result {
        private final Capability capability;
        private final NetworkLoad networkLoad;
        private final ConnectionMetrics connectionMetrics;

        Result(Capability capability, NetworkLoad networkLoad, ConnectionMetrics connectionMetrics) {
            this.capability = capability;
            this.networkLoad = networkLoad;
            this.connectionMetrics = connectionMetrics;
        }
    }

    public ConnectionHandshake(Socket socket,
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
    public Result start(NetworkLoad myNetworkLoad, Address peerAddress) {
        try {
            ConnectionMetrics connectionMetrics = new ConnectionMetrics();
            Request request = new Request(capability, myNetworkLoad);
            // As we do not know he peers load yet, we use the Load.INITIAL_LOAD
            AuthorizationToken token = authorizationService.createToken(request,
                    NetworkLoad.INITIAL_NETWORK_LOAD,
                    peerAddress.getFullAddress(),
                    0);
            NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
            long ts = System.currentTimeMillis();

            networkEnvelopeSocket.send(requestNetworkEnvelope);
            connectionMetrics.onSent(requestNetworkEnvelope);

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
                    myNetworkLoad,
                    StringUtils.createUid(),
                    myAddress);

            if (!isAuthorized) {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            connectionMetrics.onReceived(responseNetworkEnvelope);
            connectionMetrics.addRtt(System.currentTimeMillis() - ts);
            log.debug("Servers capability {}, load={}", response.getCapability(), response.getNetworkLoad());
            return new Result(response.getCapability(), response.getNetworkLoad(), connectionMetrics);
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
    public Result onSocket(NetworkLoad myNetworkLoad) {
        try {
            ConnectionMetrics connectionMetrics = new ConnectionMetrics();
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
            // As the request did not know our load at the initial request, they used the Load.INITIAL_LOAD for the
            // AuthorizationToken.
            boolean isAuthorized = authorizationService.isAuthorized(request,
                    requestNetworkEnvelope.getAuthorizationToken(),
                    NetworkLoad.INITIAL_NETWORK_LOAD,
                    StringUtils.createUid(),
                    myAddress);
            if (!isAuthorized) {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            log.debug("Clients capability {}, load={}", request.getCapability(), request.getNetworkLoad());
            connectionMetrics.onReceived(requestNetworkEnvelope);

            Response response = new Response(capability, myNetworkLoad);
            AuthorizationToken token = authorizationService.createToken(response, request.getNetworkLoad(), peerAddress.getFullAddress(), 0);
            NetworkEnvelope responseNetworkEnvelope = new NetworkEnvelope(token, response);
            networkEnvelopeSocket.send(responseNetworkEnvelope);

            connectionMetrics.onSent(responseNetworkEnvelope);
            connectionMetrics.addRtt(System.currentTimeMillis() - ts);
            return new Result(request.getCapability(), request.getNetworkLoad(), connectionMetrics);
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

    public void shutdown() {
        // todo close pending requests but do not close sockets
    }
}