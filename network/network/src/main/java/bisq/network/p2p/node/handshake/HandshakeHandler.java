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

import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class HandshakeHandler extends SimpleChannelInboundHandler<bisq.network.protobuf.NetworkEnvelope> {
    @Getter
    public static final class Result {
        private final Capability peersCapability;
        private final NetworkLoad peersNetworkLoad;
        private final ConnectionMetrics connectionMetrics;
        private final String connectionId;

        Result(Capability peersCapability,
               NetworkLoad peersNetworkLoad,
               ConnectionMetrics connectionMetrics,
               String connectionId) {
            this.peersCapability = peersCapability;
            this.peersNetworkLoad = peersNetworkLoad;
            this.connectionMetrics = connectionMetrics;
            this.connectionId = connectionId;
        }
    }

    public interface Handler {
        void onHandshakeCompleted(ChannelHandlerContext context, Result result);

        void onClosed(Channel channel);
    }

    protected final Capability myCapability;
    protected final NetworkLoad myNetworkLoad;
    protected final BanList banList;
    protected final AuthorizationService authorizationService;
    protected final KeyBundle myKeyBundle;
    protected final Handler handler;
    protected final ConnectionMetrics connectionMetrics = new ConnectionMetrics();

    protected long ts;

    public HandshakeHandler(AuthorizationService authorizationService,
                            BanList banList,
                            Capability myCapability,
                            NetworkLoad myNetworkLoad,
                            KeyBundle myKeyBundle,
                            Handler handler) {
        this.myCapability = myCapability;
        this.myNetworkLoad = myNetworkLoad;
        this.banList = banList;
        this.authorizationService = authorizationService;
        this.myKeyBundle = myKeyBundle;
        this.handler = handler;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        handler.onClosed(ctx.channel());
    }
}

