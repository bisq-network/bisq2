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
import bisq.network.common.Address;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peergroup.BanList;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

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
    private final TorIdentity myTorIdentity;

    private NetworkEnvelopeSocket networkEnvelopeSocket;

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request implements EnvelopePayloadMessage {
        private final Capability capability;
        private final byte[] addressOwnershipProof;
        private final NetworkLoad networkLoad;

        public Request(Capability capability, byte[] addressOwnershipProof, NetworkLoad networkLoad) {
            this.capability = capability;
            this.addressOwnershipProof = addressOwnershipProof;
            this.networkLoad = networkLoad;
        }

        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
            var builder = bisq.network.protobuf.ConnectionHandshake.Request.newBuilder()
                    .setCapability(capability.toProto())
                    .setNetworkLoad(networkLoad.toProto());

            if (addressOwnershipProof != null) {
                builder.setAddressOwnershipProof(ByteString.copyFrom(addressOwnershipProof));
            }

            return getNetworkMessageBuilder()
                    .setConnectionHandshakeRequest(builder.build())
                    .build();
        }

        public static Request fromProto(bisq.network.protobuf.ConnectionHandshake.Request proto) {
            ByteString addressOwnershipProofString = proto.getAddressOwnershipProof();
            byte[] addressOwnershipProof = !addressOwnershipProofString.isEmpty() ?
                    addressOwnershipProofString.toByteArray() : null;
            return new Request(Capability.fromProto(proto.getCapability()),
                    addressOwnershipProof, NetworkLoad.fromProto(proto.getNetworkLoad()));
        }

        @Override
        public double getCostFactor() {
            return 0.05;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Response implements EnvelopePayloadMessage {
        private final Capability capability;
        private final NetworkLoad networkLoad;

        public Response(Capability capability, NetworkLoad networkLoad) {
            this.capability = capability;
            this.networkLoad = networkLoad;
        }

        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
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

        @Override
        public double getCostFactor() {
            return 0.05;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Result {
        private final Capability capability;
        private final NetworkLoad peersNetworkLoad;
        private final ConnectionMetrics connectionMetrics;

        Result(Capability capability, NetworkLoad peersNetworkLoad, ConnectionMetrics connectionMetrics) {
            this.capability = capability;
            this.peersNetworkLoad = peersNetworkLoad;
            this.connectionMetrics = connectionMetrics;
        }
    }

    public ConnectionHandshake(Socket socket,
                               BanList banList,
                               int socketTimeout,
                               Capability capability,
                               AuthorizationService authorizationService,
                               TorIdentity myTorIdentity) {
        this.banList = banList;
        this.capability = capability;
        this.authorizationService = authorizationService;
        this.myTorIdentity = myTorIdentity;

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

            Address myAddress = capability.getAddress();
            byte[] addressOwnershipProof = null;
            if (myAddress.isTorAddress()) {
                String dataToSign = myAddress.getFullAddress() + "|" + peerAddress.getFullAddress();
                addressOwnershipProof = myTorIdentity.signMessage(dataToSign.getBytes());
            }

            Request request = new Request(capability, addressOwnershipProof, myNetworkLoad);
            // As we do not know he peers load yet, we use the NetworkLoad.INITIAL_LOAD
            AuthorizationToken token = authorizationService.createToken(request,
                    NetworkLoad.INITIAL_LOAD,
                    peerAddress.getFullAddress(),
                    0);
            NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
            long ts = System.currentTimeMillis();
            networkEnvelopeSocket.send(requestNetworkEnvelope);
            connectionMetrics.onSent(requestNetworkEnvelope, System.currentTimeMillis() - ts);

            bisq.network.protobuf.NetworkEnvelope responseProto = networkEnvelopeSocket.receiveNextEnvelope();
            if (responseProto == null) {
                throw new ConnectionException("Response NetworkEnvelope protobuf is null");
            }

            long startDeserializeTs = System.currentTimeMillis();
            NetworkEnvelope responseNetworkEnvelope = NetworkEnvelope.fromProto(responseProto);
            long deserializeTime = System.currentTimeMillis() - startDeserializeTs;

            responseNetworkEnvelope.verifyVersion();
            if (!(responseNetworkEnvelope.getEnvelopePayloadMessage() instanceof Response)) {
                throw new ConnectionException("ResponseEnvelope.message() not type of Response. responseEnvelope=" +
                        responseNetworkEnvelope);
            }
            Response response = (Response) responseNetworkEnvelope.getEnvelopePayloadMessage();
            if (banList.isBanned(response.getCapability().getAddress())) {
                throw new ConnectionException("Peers address is in quarantine. response=" + response);
            }

            boolean isAuthorized = authorizationService.isAuthorized(response,
                    responseNetworkEnvelope.getAuthorizationToken(),
                    myNetworkLoad,
                    StringUtils.createUid(),
                    myAddress.getFullAddress());

            if (!isAuthorized) {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            connectionMetrics.onReceived(responseNetworkEnvelope, deserializeTime);

            long rrt = System.currentTimeMillis() - ts;
            connectionMetrics.addRtt(rrt);

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
            long ts = System.currentTimeMillis();
            NetworkEnvelope requestNetworkEnvelope = NetworkEnvelope.fromProto(requestProto);
            long deserializeTime = System.currentTimeMillis() - ts;

            requestNetworkEnvelope.verifyVersion();

            if (!(requestNetworkEnvelope.getEnvelopePayloadMessage() instanceof Request)) {
                throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                        requestNetworkEnvelope);
            }
            Request request = (Request) requestNetworkEnvelope.getEnvelopePayloadMessage();
            Address peerAddress = request.getCapability().getAddress();
            if (banList.isBanned(peerAddress)) {
                throw new ConnectionException("Peers address is in quarantine. request=" + request);
            }

            String myAddress = capability.getAddress().getFullAddress();
            // As the request did not know our load at the initial request, they used the NetworkLoad.INITIAL_LOAD for the
            // AuthorizationToken.
            boolean isAuthorized = authorizationService.isAuthorized(request,
                    requestNetworkEnvelope.getAuthorizationToken(),
                    NetworkLoad.INITIAL_LOAD,
                    StringUtils.createUid(),
                    myAddress);
            if (isAuthorized) {
                log.info("Peer {} proofed ownership of its onion address successfully.", peerAddress.getFullAddress());
            } else {
                throw new ConnectionException("Request authorization failed. request=" + request);
            }

            if (peerAddress.isTorAddress()) {
                String addressOwnershipMessage = peerAddress.getFullAddress() + "|" + myAddress;
                byte[] addressOwnershipProof = request.getAddressOwnershipProof();
                boolean isProofValid = TorIdentity.verifyMessage(peerAddress.getHost(),
                        addressOwnershipMessage.getBytes(),
                        addressOwnershipProof);
                if (!isProofValid) {
                    throw new ConnectionException("Peer couldn't proof its onion address: " + peerAddress.getFullAddress() +
                            ", Proof: " + Arrays.toString(addressOwnershipProof));
                }
            }

            log.debug("Clients capability {}, load={}", request.getCapability(), request.getNetworkLoad());
            connectionMetrics.onReceived(requestNetworkEnvelope, deserializeTime);

            Response response = new Response(capability, myNetworkLoad);
            AuthorizationToken token = authorizationService.createToken(response, request.getNetworkLoad(), peerAddress.getFullAddress(), 0);
            NetworkEnvelope responseNetworkEnvelope = new NetworkEnvelope(token, response);
            long startSendTs = System.currentTimeMillis();
            networkEnvelopeSocket.send(responseNetworkEnvelope);
            connectionMetrics.onSent(responseNetworkEnvelope, System.currentTimeMillis() - startSendTs);
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