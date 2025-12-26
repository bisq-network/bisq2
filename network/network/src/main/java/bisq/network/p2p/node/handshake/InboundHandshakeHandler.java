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
import bisq.common.network.Address;
import bisq.common.util.StringUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.node.ConnectionException.Reason.ADDRESS_BANNED;
import static bisq.network.p2p.node.ConnectionException.Reason.AUTHORIZATION_FAILED;
import static bisq.network.p2p.node.ConnectionException.Reason.ONION_ADDRESS_VERIFICATION_FAILED;
import static bisq.network.p2p.node.ConnectionException.Reason.PROTOBUF_IS_NULL;


@Slf4j
public class InboundHandshakeHandler extends HandshakeHandler {
    public InboundHandshakeHandler(AuthorizationService authorizationService,
                                   BanList banList,
                                   Capability myCapability,
                                   NetworkLoad myNetworkLoad,
                                   KeyBundle myKeyBundle,
                                   Handler handler) {
        super(authorizationService,
                banList,
                myCapability,
                myNetworkLoad,
                myKeyBundle,
                handler);
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        super.channelActive(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, bisq.network.protobuf.NetworkEnvelope proto) {
        try {
            if (proto == null) {
                throw new ConnectionException(PROTOBUF_IS_NULL, "NetworkEnvelope protobuf is null");
            }

            long ts = System.currentTimeMillis();
            NetworkEnvelope networkEnvelope = NetworkEnvelope.fromProto(proto);
            long deserializeTime = System.currentTimeMillis() - ts;

            networkEnvelope.verifyVersion();

            if (!(networkEnvelope.getEnvelopePayloadMessage() instanceof ConnectionHandshake.Request request)) {
                throw new ConnectionException("RequestEnvelope.message() not type of Request. requestEnvelope=" +
                        networkEnvelope);
            }
            Capability requestersCapability = request.getCapability();
            Address peerAddress = requestersCapability.getAddress();

            //TODO banList not implemented yet to get set banned addresses.
            if (banList.isBanned(peerAddress)) {
                throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + peerAddress);
            }

            Address myAddress = myCapability.getAddress();
            // As the request did not know our load at the initial request, they used the NetworkLoad.INITIAL_LOAD for the AuthorizationToken.
            String connectionId = StringUtils.createUid();
            boolean isAuthorized = authorizationService.isAuthorized(request,
                    networkEnvelope.getAuthorizationToken(),
                    NetworkLoad.INITIAL_NETWORK_LOAD,
                    connectionId,
                    myAddress.getFullAddress());
            if (!isAuthorized) {
                throw new ConnectionException(AUTHORIZATION_FAILED,
                        "Authorization of inbound connection request failed. AuthorizationToken=" +
                                networkEnvelope.getAuthorizationToken());
            }

            if (myAddress.isTorAddress() && peerAddress.isTorAddress()) {
                if (!OnionAddressValidation.verify(myAddress, peerAddress, request.getSignatureDate(), request.getAddressOwnershipProof())) {
                    throw new ConnectionException(ONION_ADDRESS_VERIFICATION_FAILED, "Peer couldn't proof its onion address: " + peerAddress.getFullAddress() +
                            ", Proof: " + Hex.encode(request.getAddressOwnershipProof().orElseThrow()));
                }
            }
            NetworkLoad peersNetworkLoad = request.getNetworkLoad();
            log.debug("Clients capability {}, load={}", requestersCapability, peersNetworkLoad);
            connectionMetrics.onReceived(networkEnvelope, deserializeTime);

            // We reply with the same version as the peer has to avoid pow hash check failures
            Capability responseCapability = Capability.withVersion(myCapability, requestersCapability.getVersion());
            ConnectionHandshake.Response response = new ConnectionHandshake.Response(responseCapability, myNetworkLoad);
            AuthorizationToken token = authorizationService.createToken(response,
                    peersNetworkLoad,
                    peerAddress.getFullAddress(),
                    0,
                    requestersCapability.getFeatures());
            NetworkEnvelope responseNetworkEnvelope = new NetworkEnvelope(token, response);
            long startSendTs = System.currentTimeMillis();

            context.writeAndFlush(responseNetworkEnvelope.completeProto());

            connectionMetrics.onSent(responseNetworkEnvelope, System.currentTimeMillis() - startSendTs);
            connectionMetrics.addRtt(System.currentTimeMillis() - ts);
            handler.onHandshakeCompleted(context, new Result(requestersCapability, peersNetworkLoad, connectionMetrics, connectionId));

             context.pipeline().remove(this);
        } catch (Exception exception) {
            if (exception instanceof ConnectionException connectionException) {
                throw connectionException;
            } else {
                throw new ConnectionException(exception);
            }
        }
    }
}

