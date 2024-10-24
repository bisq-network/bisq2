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
import bisq.common.network.Address;
import bisq.common.network.AddressOwnershipProof;
import bisq.common.network.AddressOwnershipProofGenerator;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.OutboundConnection;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.network.p2p.node.ConnectionException.Reason.*;

@Slf4j
public class ConnectionHandshakeInitiator {
    private final Capability myCapability;
    private final AuthorizationService authorizationService;
    private final BanList banList;
    private final NetworkLoad myNetworkLoad;
    private final Address peerAddress;
    @Getter
    private final CompletableFuture<OutboundConnection> completableFuture = new CompletableFuture<>();
    private final AddressOwnershipProofGenerator addressOwnershipProofGenerator;

    public ConnectionHandshakeInitiator(Capability myCapability,
                                        AuthorizationService authorizationService,
                                        BanList banList,
                                        NetworkLoad myNetworkLoad,
                                        Address peerAddress,
                                        AddressOwnershipProofGenerator addressOwnershipProofGenerator) {
        this.myCapability = myCapability;
        this.authorizationService = authorizationService;
        this.banList = banList;
        this.myNetworkLoad = myNetworkLoad;
        this.peerAddress = peerAddress;
        this.addressOwnershipProofGenerator = addressOwnershipProofGenerator;
    }

    public NetworkEnvelope initiate() {
        Address myAddress = myCapability.getAddress();
        AddressOwnershipProof proof = addressOwnershipProofGenerator.generate(myAddress, peerAddress);
        Optional<byte[]> optionalProof = proof.getProof().map(ByteBuffer::array);

        ConnectionHandshake.Request request = new ConnectionHandshake.Request(myCapability, optionalProof, myNetworkLoad, proof.getSignatureDate());
        // As we do not know he peers load yet, we use the NetworkLoad.INITIAL_LOAD
        AuthorizationToken token = authorizationService.createToken(request,
                NetworkLoad.INITIAL_NETWORK_LOAD,
                peerAddress.getFullAddress(),
                0,
                new ArrayList<>());
        return new NetworkEnvelope(token, request);
    }

    public ConnectionHandshake.Response finish(List<NetworkEnvelope> responseNetworkEnvelopes) {
        if (responseNetworkEnvelopes.isEmpty()) {
            throw new ConnectionException("Received empty requests.");
        }

        if (responseNetworkEnvelopes.size() > 1) {
            throw new ConnectionException("Received multiple responses from client. requests=" + responseNetworkEnvelopes);
        }

        NetworkEnvelope responseNetworkEnvelope = responseNetworkEnvelopes.get(0);
        responseNetworkEnvelope.verifyVersion();

        if (!(responseNetworkEnvelope.getEnvelopePayloadMessage() instanceof ConnectionHandshake.Response response)) {
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
                myCapability.getAddress().getFullAddress());

        if (!isAuthorized) {
            throw new ConnectionException(AUTHORIZATION_FAILED, "ConnectionHandshake.Response authorization failed. AuthorizationToken=" + responseNetworkEnvelope.getAuthorizationToken());
        }

        log.debug("Servers capability {}, load={}", response.getCapability(), response.getNetworkLoad());
        return response;
    }
}
