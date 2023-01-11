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

import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.services.peergroup.BanList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class ConnectionHandshakeResponder {

    private final NetworkEnvelopeSocket networkEnvelopeSocket;
    private final BanList banList;
    private final Capability capability;
    private final AuthorizationService authorizationService;

    public ConnectionHandshakeResponder(NetworkEnvelopeSocket networkEnvelopeSocket,
                                        BanList banList,
                                        Capability capability,
                                        AuthorizationService authorizationService) {
        this.networkEnvelopeSocket = networkEnvelopeSocket;
        this.banList = banList;
        this.capability = capability;
        this.authorizationService = authorizationService;
    }

    public ConnectionHandshake.Result verifyPoW(Load myLoad) {
        try {
            Metrics metrics = new Metrics();

            bisq.network.protobuf.NetworkEnvelope requestProto = networkEnvelopeSocket.receiveNextEnvelope();
            NetworkEnvelope requestNetworkEnvelope = parseAndValidateRequest(requestProto);

            long ts = System.currentTimeMillis();

            ConnectionHandshake.Request request = (ConnectionHandshake.Request) requestNetworkEnvelope.getNetworkMessage();
            verifyPeerIsNotBanned(request);

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

            Address peerAddress = request.getCapability().getAddress();
            NetworkEnvelope responseNetworkEnvelope = createResponseEnvelope(myLoad, request.getLoad(), peerAddress);
            networkEnvelopeSocket.send(responseNetworkEnvelope);

            metrics.onSent(responseNetworkEnvelope);
            metrics.addRtt(System.currentTimeMillis() - ts);
            return new ConnectionHandshake.Result(request.getCapability(), request.getLoad(), metrics);
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

    private NetworkEnvelope parseAndValidateRequest(bisq.network.protobuf.NetworkEnvelope requestProto) {
        if (requestProto == null) {
            throw new ConnectionException("Request NetworkEnvelope protobuf is null");
        }
        NetworkEnvelope requestNetworkEnvelope = NetworkEnvelope.fromProto(requestProto);

        validateEnvelopeVersion(requestNetworkEnvelope);
        validateNetworkMessage(requestNetworkEnvelope);

        return requestNetworkEnvelope;
    }

    private void verifyPeerIsNotBanned(ConnectionHandshake.Request request) {
        Address peerAddress = request.getCapability().getAddress();
        if (banList.isBanned(peerAddress)) {
            throw new ConnectionException("Peers address is in quarantine. request=" + request);
        }
    }

    private NetworkEnvelope createResponseEnvelope(Load myLoad, Load peerLoad, Address peerAddress) {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(capability, myLoad);
        AuthorizationToken token = authorizationService.createToken(response, peerLoad, peerAddress.getFullAddress(), 0);
        return new NetworkEnvelope(NetworkEnvelope.VERSION, token, response);
    }

    private void validateEnvelopeVersion(NetworkEnvelope requestNetworkEnvelope) {
        if (requestNetworkEnvelope.getVersion() != NetworkEnvelope.VERSION) {
            throw new ConnectionException("Invalid version. requestEnvelop.version()=" +
                    requestNetworkEnvelope.getVersion() + "; Version.VERSION=" + NetworkEnvelope.VERSION);
        }
    }

    private void validateNetworkMessage(NetworkEnvelope requestNetworkEnvelope) {
        if (!(requestNetworkEnvelope.getNetworkMessage() instanceof ConnectionHandshake.Request)) {
            throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                    requestNetworkEnvelope);
        }
    }
}
