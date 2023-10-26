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
import bisq.common.util.StringUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.common.Address;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ConnectionHandshakeResponder {

    private final BanList banList;
    private final Capability capability;
    private final NetworkLoad myNetworkLoad;
    private final AuthorizationService authorizationService;
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel;

    public ConnectionHandshakeResponder(BanList banList,
                                        Capability capability,
                                        NetworkLoad myNetworkLoad,
                                        AuthorizationService authorizationService,
                                        NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel) {
        this.banList = banList;
        this.capability = capability;
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

        Address peerAddress = request.getCapability().getAddress();
        NetworkEnvelope responseEnvelope = createResponseEnvelope(myNetworkLoad, request.getNetworkLoad(), peerAddress);

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
        String myAddress = capability.getAddress().getFullAddress();
        // As the request did not know our load at the initial request, they used the NetworkLoad.INITIAL_LOAD for the
        // AuthorizationToken.
        boolean isAuthorized = authorizationService.isAuthorized(
                request,
                requestNetworkEnvelope.getAuthorizationToken(),
                NetworkLoad.INITIAL_LOAD,
                StringUtils.createUid(),
                myAddress
        );

        if (isAuthorized) {
            log.debug("Peer {} passed PoW authorization.",
                    ((ConnectionHandshake.Request) requestNetworkEnvelope.getEnvelopePayloadMessage()).getCapability().getAddress());
        } else {
            throw new ConnectionException("Request authorization failed. request=" + request);
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
            throw new ConnectionException("Peers address is in quarantine. request=" + request);
        }
    }

    private NetworkEnvelope createResponseEnvelope(NetworkLoad myNetworkLoad, NetworkLoad peerNetworkLoad, Address peerAddress) {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(capability, myNetworkLoad);
        AuthorizationToken token = authorizationService.createToken(response, peerNetworkLoad, peerAddress.getFullAddress(), 0);
        return new NetworkEnvelope(token, response);
    }

    private void validateNetworkMessage(NetworkEnvelope requestNetworkEnvelope) {
        if (!(requestNetworkEnvelope.getEnvelopePayloadMessage() instanceof ConnectionHandshake.Request)) {
            throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                    requestNetworkEnvelope);
        }
    }
}
