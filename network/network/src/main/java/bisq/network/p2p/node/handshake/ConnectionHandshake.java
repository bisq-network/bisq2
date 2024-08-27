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

import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.network.common.Address;
import bisq.network.common.DefaultPeerSocket;
import bisq.network.common.PeerSocket;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocket;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

import static bisq.network.p2p.node.ConnectionException.Reason.*;
import static com.google.common.base.Preconditions.checkArgument;

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
    private final KeyBundle myKeyBundle;
    private NetworkEnvelopeSocket networkEnvelopeSocket;

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Request implements EnvelopePayloadMessage {
        private final Capability capability;
        private final Optional<byte[]> addressOwnershipProof;
        private final NetworkLoad networkLoad;
        private final long signatureDate;

        public Request(Capability capability,
                       Optional<byte[]> addressOwnershipProof,
                       NetworkLoad networkLoad,
                       long signatureDate) {
            this.capability = capability;
            this.addressOwnershipProof = addressOwnershipProof;
            this.networkLoad = networkLoad;
            this.signatureDate = signatureDate;

            verify();
        }

        @Override
        public void verify() {
            addressOwnershipProof.ifPresent(signature -> {
                // Tor signature has 64 bytes
                checkArgument(signature.length == 64,
                        "Signature not of the expected size. signature.length=" + signature.length);
            });
        }

        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
            return newEnvelopePayloadMessageBuilder().setConnectionHandshakeRequest(toValueProto(serializeForHash));
        }

        @Override
        public bisq.network.protobuf.ConnectionHandshake.Request toValueProto(boolean serializeForHash) {
            return resolveValueProto(serializeForHash);
        }

        @Override
        public bisq.network.protobuf.ConnectionHandshake.Request.Builder getValueBuilder(boolean serializeForHash) {
            bisq.network.protobuf.ConnectionHandshake.Request.Builder builder = bisq.network.protobuf.ConnectionHandshake.Request.newBuilder()
                    .setCapability(capability.toProto(serializeForHash))
                    .setNetworkLoad(networkLoad.toProto(serializeForHash))
                    .setSignatureDate(signatureDate);
            addressOwnershipProof.ifPresent(e -> builder.setAddressOwnershipProof(ByteString.copyFrom(e)));
            return builder;
        }

        public static Request fromProto(bisq.network.protobuf.ConnectionHandshake.Request proto) {
            return new Request(Capability.fromProto(proto.getCapability()),
                    proto.hasAddressOwnershipProof() ? Optional.of(proto.getAddressOwnershipProof().toByteArray()) : Optional.empty(),
                    NetworkLoad.fromProto(proto.getNetworkLoad()),
                    proto.getSignatureDate());
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

            verify();
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
            return newEnvelopePayloadMessageBuilder().setConnectionHandshakeResponse(toValueProto(serializeForHash));
        }

        @Override
        public bisq.network.protobuf.ConnectionHandshake.Response toValueProto(boolean serializeForHash) {
            return resolveValueProto(serializeForHash);
        }

        @Override
        public bisq.network.protobuf.ConnectionHandshake.Response.Builder getValueBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.ConnectionHandshake.Response.newBuilder()
                    .setCapability(capability.toProto(serializeForHash))
                    .setNetworkLoad(networkLoad.toProto(serializeForHash));
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
        private final Capability peersCapability;
        private final NetworkLoad peersNetworkLoad;
        private final ConnectionMetrics connectionMetrics;

        Result(Capability peersCapability, NetworkLoad peersNetworkLoad, ConnectionMetrics connectionMetrics) {
            this.peersCapability = peersCapability;
            this.peersNetworkLoad = peersNetworkLoad;
            this.connectionMetrics = connectionMetrics;
        }
    }

    public ConnectionHandshake(Socket socket,
                               BanList banList,
                               int socketTimeout,
                               Capability capability,
                               AuthorizationService authorizationService,
                               KeyBundle myKeyBundle) {
        this.banList = banList;
        this.capability = capability;
        this.authorizationService = authorizationService;
        this.myKeyBundle = myKeyBundle;

        try {
            // socket.setTcpNoDelay(true);
            // socket.setSoLinger(true, 100);
            socket.setSoTimeout(socketTimeout);

            PeerSocket peerSocket = new DefaultPeerSocket(socket);
            this.networkEnvelopeSocket = new NetworkEnvelopeSocket(peerSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Client side protocol
    public Result start(NetworkLoad myNetworkLoad, Address peerAddress) {
        try {
            ConnectionMetrics connectionMetrics = new ConnectionMetrics();

            Address myAddress = capability.getAddress();
            long signatureDate = System.currentTimeMillis();
            Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myKeyBundle.getTorKeyPair().getPrivateKey());

            Request request = new Request(capability, signature, myNetworkLoad, signatureDate);
            // As we do not know he peers load yet, we use the NetworkLoad.INITIAL_LOAD
            AuthorizationToken token = authorizationService.createToken(request,
                    NetworkLoad.INITIAL_NETWORK_LOAD,
                    peerAddress.getFullAddress(),
                    0,
                    new ArrayList<>());
            NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
            long ts = System.currentTimeMillis();
            networkEnvelopeSocket.send(requestNetworkEnvelope);
            connectionMetrics.onSent(requestNetworkEnvelope, System.currentTimeMillis() - ts);

            bisq.network.protobuf.NetworkEnvelope responseProto = networkEnvelopeSocket.receiveNextEnvelope();
            if (responseProto == null) {
                throw new ConnectionException(PROTOBUF_IS_NULL,
                        "Response NetworkEnvelope protobuf is null. peerAddress=" + peerAddress);
            }

            long startDeserializeTs = System.currentTimeMillis();
            NetworkEnvelope responseNetworkEnvelope = NetworkEnvelope.fromProto(responseProto);
            long deserializeTime = System.currentTimeMillis() - startDeserializeTs;

            responseNetworkEnvelope.verifyVersion();
            if (!(responseNetworkEnvelope.getEnvelopePayloadMessage() instanceof Response response)) {
                throw new ConnectionException("ResponseEnvelope.message() not type of Response. responseEnvelope=" +
                        responseNetworkEnvelope);
            }
            Address address = response.getCapability().getAddress();
            if (banList.isBanned(address)) {
                throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + address);
            }

            boolean isAuthorized = authorizationService.isAuthorized(response,
                    responseNetworkEnvelope.getAuthorizationToken(),
                    myNetworkLoad,
                    StringUtils.createUid(),
                    myAddress.getFullAddress());

            if (!isAuthorized) {
                throw new ConnectionException(AUTHORIZATION_FAILED, "ConnectionHandshake.Response authorization failed at outbound connection attempt. AuthorizationToken=" + responseNetworkEnvelope.getAuthorizationToken());
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
                throw new ConnectionException(PROTOBUF_IS_NULL,
                        "Request NetworkEnvelope protobuf is null");
            }
            long ts = System.currentTimeMillis();
            NetworkEnvelope requestNetworkEnvelope = NetworkEnvelope.fromProto(requestProto);
            long deserializeTime = System.currentTimeMillis() - ts;

            requestNetworkEnvelope.verifyVersion();

            if (!(requestNetworkEnvelope.getEnvelopePayloadMessage() instanceof Request request)) {
                throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                        requestNetworkEnvelope);
            }
            Capability requestersCapability = request.getCapability();
            Address peerAddress = requestersCapability.getAddress();
            if (banList.isBanned(peerAddress)) {
                throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + peerAddress);
            }

            Address myAddress = capability.getAddress();
            // As the request did not know our load at the initial request, they used the NetworkLoad.INITIAL_LOAD for the
            // AuthorizationToken.
            boolean isAuthorized = authorizationService.isAuthorized(request,
                    requestNetworkEnvelope.getAuthorizationToken(),
                    NetworkLoad.INITIAL_NETWORK_LOAD,
                    StringUtils.createUid(),
                    myAddress.getFullAddress());
            if (!isAuthorized) {
                throw new ConnectionException(AUTHORIZATION_FAILED, "Authorization of inbound connection request failed. AuthorizationToken=" + requestNetworkEnvelope.getAuthorizationToken());
            }

            if (!OnionAddressValidation.verify(myAddress, peerAddress, request.getSignatureDate(), request.getAddressOwnershipProof())) {
                throw new ConnectionException(ONION_ADDRESS_VERIFICATION_FAILED, "Peer couldn't proof its onion address: " + peerAddress.getFullAddress() +
                        ", Proof: " + Hex.encode(request.getAddressOwnershipProof().orElseThrow()));
            }

            log.debug("Clients capability {}, load={}", requestersCapability, request.getNetworkLoad());
            connectionMetrics.onReceived(requestNetworkEnvelope, deserializeTime);

            // We reply with the same version as the peer has to avoid pow hash check failures
            Capability responseCapability = Capability.withVersion(capability, requestersCapability.getVersion());
            Response response = new Response(responseCapability, myNetworkLoad);
            AuthorizationToken token = authorizationService.createToken(response,
                    request.getNetworkLoad(),
                    peerAddress.getFullAddress(),
                    0,
                    requestersCapability.getFeatures());
            NetworkEnvelope responseNetworkEnvelope = new NetworkEnvelope(token, response);
            long startSendTs = System.currentTimeMillis();
            networkEnvelopeSocket.send(responseNetworkEnvelope);
            connectionMetrics.onSent(responseNetworkEnvelope, System.currentTimeMillis() - startSendTs);
            connectionMetrics.addRtt(System.currentTimeMillis() - ts);
            return new Result(requestersCapability, request.getNetworkLoad(), connectionMetrics);
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
        // todo (Critical) close pending requests but do not close sockets
    }
}