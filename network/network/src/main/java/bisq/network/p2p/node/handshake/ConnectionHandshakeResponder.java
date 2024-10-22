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

import bisq.common.data.Pair;
import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.network.common.Address;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static bisq.network.p2p.node.ConnectionException.Reason.*;

@Slf4j
public class ConnectionHandshakeResponder {
    private final BanList banList;
    private final Capability myCapability;
    private final NetworkLoad myNetworkLoad;
    private final AuthorizationService authorizationService;
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel;

    public ConnectionHandshakeResponder(BanList banList,
                                        Capability myCapability,
                                        NetworkLoad myNetworkLoad,
                                        AuthorizationService authorizationService,
                                        NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel) {
        this.banList = banList;
        this.myCapability = myCapability;
        this.myNetworkLoad = myNetworkLoad;
        this.authorizationService = authorizationService;
        this.networkEnvelopeSocketChannel = networkEnvelopeSocketChannel;
    }

    public Pair<ConnectionHandshake.Request, NetworkEnvelope> verifyAndBuildRespond() throws IOException {
        List<NetworkEnvelope> requestEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        validateRequestEnvelopes(requestEnvelopes);

        NetworkEnvelope requestProto = requestEnvelopes.get(0);
        NetworkEnvelope requestNetworkEnvelope = parseAndValidateRequest(requestProto);

        ConnectionHandshake.Request request = (ConnectionHandshake.Request) requestNetworkEnvelope.getEnvelopePayloadMessage();
        verifyPeerIsNotBanned(request);
        verifyPoW(requestNetworkEnvelope);

        Capability requestersCapability = request.getCapability();
        Address peerAddress = requestersCapability.getAddress();
        Address myAddress = myCapability.getAddress();
        if (!OnionAddressValidation.verify(myAddress, peerAddress, request.getSignatureDate(), request.getAddressOwnershipProof())) {
            throw new ConnectionException(ONION_ADDRESS_VERIFICATION_FAILED, "Peer couldn't proof its onion address: " + peerAddress.getFullAddress() +
                    ", Proof: " + Hex.encode(request.getAddressOwnershipProof().orElseThrow()));
        }

        NetworkEnvelope responseEnvelope = createResponseEnvelope(myNetworkLoad,
                request.getNetworkLoad(),
                peerAddress,
                requestersCapability.getFeatures());

        return new Pair<>(request, responseEnvelope);
    }

    private void validateRequestEnvelopes(List<NetworkEnvelope> requestEnvelopes) {
        if (requestEnvelopes.isEmpty()) {
            throw new ConnectionException("Received empty requests.");
        }

        if (requestEnvelopes.size() > 1) {
            throw new ConnectionException("Received multiple requests from client. requests=" + requestEnvelopes);
        }
    }

    private void verifyPoW(NetworkEnvelope requestNetworkEnvelope) {
        ConnectionHandshake.Request request = (ConnectionHandshake.Request) requestNetworkEnvelope.getEnvelopePayloadMessage();
        String myAddress = myCapability.getAddress().getFullAddress();
        // As the request did not know our load at the initial request, they used the NetworkLoad.INITIAL_LOAD for the
        // AuthorizationToken.
        boolean isAuthorized = authorizationService.isAuthorized(
                request,
                requestNetworkEnvelope.getAuthorizationToken(),
                NetworkLoad.INITIAL_NETWORK_LOAD,
                StringUtils.createUid(),
                myAddress
        );

        if (!isAuthorized) {
            throw new ConnectionException(AUTHORIZATION_FAILED, "ConnectionHandshake.Request authorization failed. AuthorizationToken=" + requestNetworkEnvelope.getAuthorizationToken());
        }

        log.debug("Clients capability {}, load={}", request.getCapability(), request.getNetworkLoad());
    }

    private NetworkEnvelope parseAndValidateRequest(NetworkEnvelope requestNetworkEnvelope) {
        requestNetworkEnvelope.verifyVersion();

        validateNetworkMessage(requestNetworkEnvelope);
        return requestNetworkEnvelope;
    }

    private void verifyPeerIsNotBanned(ConnectionHandshake.Request request) {
        Address peerAddress = request.getCapability().getAddress();
        if (banList.isBanned(peerAddress)) {
            throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + peerAddress);
        }
    }

    private NetworkEnvelope createResponseEnvelope(NetworkLoad myNetworkLoad,
                                                   NetworkLoad peerNetworkLoad,
                                                   Address peerAddress,
                                                   List<Feature> requestersFeatures) {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(myCapability, myNetworkLoad);
        AuthorizationToken token = authorizationService.createToken(response,
                peerNetworkLoad,
                peerAddress.getFullAddress(),
                0,
                requestersFeatures);
        return new NetworkEnvelope(token, response);
    }

    private void validateNetworkMessage(NetworkEnvelope requestNetworkEnvelope) {
        if (!(requestNetworkEnvelope.getEnvelopePayloadMessage() instanceof ConnectionHandshake.Request)) {
            throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                    requestNetworkEnvelope);
        }
    }
}
