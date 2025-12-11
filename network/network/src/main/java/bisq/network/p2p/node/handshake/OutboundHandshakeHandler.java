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

import bisq.common.network.Address;
import bisq.common.util.StringUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.Socks5ProxyHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static bisq.network.p2p.node.ConnectionException.Reason.ADDRESS_BANNED;
import static bisq.network.p2p.node.ConnectionException.Reason.AUTHORIZATION_FAILED;
import static bisq.network.p2p.node.ConnectionException.Reason.PROTOBUF_IS_NULL;


@Slf4j
public class OutboundHandshakeHandler extends HandshakeHandler {
    private Address peerAddress;

    public OutboundHandshakeHandler(AuthorizationService authorizationService,
                                    BanList banList,
                                    Capability myCapability,
                                    NetworkLoad myNetworkLoad,
                                    KeyBundle myKeyBundle,
                                    Address peerAddress,
                                    Handler handler) {
        super(authorizationService,
                banList,
                myCapability,
                myNetworkLoad,
                myKeyBundle,
                handler);
        this.peerAddress = peerAddress;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        // The Socks5ProxyHandler does the handshake protocol with the proxy asynchronously and fires the
        // ProxyConnectionEvent once completed.
        if (event instanceof ProxyConnectionEvent) {
            log.info("SOCKS5 ready, sending handshake");
            start(context);

            // Socks5ProxyHandler not needed anymore
            context.pipeline().remove(Socks5ProxyHandler.class);
        }
        super.userEventTriggered(context, event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        // If there's a Socks5ProxyHandler in the pipeline, wait for ProxyConnectionEvent before starting the handshake.
        // Otherwise start the handshake immediately (direct connection).
        if (context.pipeline().get(Socks5ProxyHandler.class) == null) {
            start(context);
        }
        super.channelActive(context);
    }

    private void start(ChannelHandlerContext context) {
        try {
            ConnectionMetrics connectionMetrics = new ConnectionMetrics();

            Address myAddress = myCapability.getAddress();
            long signatureDate = System.currentTimeMillis();
            Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myKeyBundle.getTorKeyPair().getPrivateKey());

            ConnectionHandshake.Request request = new ConnectionHandshake.Request(myCapability, signature, myNetworkLoad, signatureDate);

            // As we do not know the peers networkLoad yet, we use the NetworkLoad.INITIAL_LOAD.
            NetworkLoad initialNetworkLoad = NetworkLoad.INITIAL_NETWORK_LOAD;

            // As we do not know the peers features yet, we use a set of minimal default features.
            Set<Feature> peersFeatures = Feature.DEFAULT_FEATURES;

            AuthorizationToken token = authorizationService.createToken(request,
                    initialNetworkLoad,
                    peerAddress.getFullAddress(),
                    0,
                    peersFeatures);
            NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
            ts = System.currentTimeMillis();

            context.writeAndFlush(requestNetworkEnvelope.completeProto());
            connectionMetrics.onSent(requestNetworkEnvelope, System.currentTimeMillis() - ts);
        } catch (Exception e) {
            //  networkEnvelopeSocket.close();
            if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            } else {
                // May be SocketTimeoutException, IOException or unexpected Exception
                throw new ConnectionException(e);
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, bisq.network.protobuf.NetworkEnvelope proto) {
        if (peerAddress == null) {
            log.error("peerAddress is expected to be not null.");
            return;
        }

        try {
            if (proto == null) {
                throw new ConnectionException(PROTOBUF_IS_NULL,
                        "Response NetworkEnvelope protobuf is null. peerAddress=" + peerAddress);
            }

            long startDeserializeTs = System.currentTimeMillis();
            NetworkEnvelope networkEnvelope = NetworkEnvelope.fromProto(proto);
            long deserializeTime = System.currentTimeMillis() - startDeserializeTs;

            networkEnvelope.verifyVersion();
            if (!(networkEnvelope.getEnvelopePayloadMessage() instanceof ConnectionHandshake.Response response)) {
                throw new ConnectionException("ResponseEnvelope.message() not type of Response. responseEnvelope=" +
                        networkEnvelope);
            }
            Capability peersCapability = response.getCapability();
            Address peersAddress = peersCapability.getAddress();
            if (banList.isBanned(peersAddress)) {
                throw new ConnectionException(ADDRESS_BANNED, "PeerAddress is banned. address=" + peersAddress);
            }

            String connectionId = StringUtils.createUid();
            Address myAddress = myCapability.getAddress();
            boolean isAuthorized = authorizationService.isAuthorized(response,
                    networkEnvelope.getAuthorizationToken(),
                    myNetworkLoad,
                    connectionId,
                    myAddress.getFullAddress());

            if (!isAuthorized) {
                throw new ConnectionException(AUTHORIZATION_FAILED,
                        "ConnectionHandshake.Response authorization failed at outbound connection attempt. AuthorizationToken=" +
                                networkEnvelope.getAuthorizationToken());
            }

            connectionMetrics.onReceived(networkEnvelope, deserializeTime);

            long rrt = System.currentTimeMillis() - ts;
            connectionMetrics.addRtt(rrt);

            NetworkLoad peersNetworkLoad = response.getNetworkLoad();
            log.debug("Peers capability {}, load={}", peersCapability, peersNetworkLoad);
            handler.onHandshakeCompleted(context, new Result(peersCapability, peersNetworkLoad, connectionMetrics, connectionId));

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

