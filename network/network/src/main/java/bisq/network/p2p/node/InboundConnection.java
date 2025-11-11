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

import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
public class InboundConnection extends Connection {
    InboundConnection(AuthorizationService authorizationService,
                      ChannelHandlerContext context,
                      String connectionId,
                      Capability peersCapability,
                      NetworkLoadSnapshot peersNetworkLoadSnapshot,
                      ConnectionMetrics connectionMetrics,
                      ConnectionThrottle connectionThrottle,
                      Handler handler,
                      BiConsumer<Connection, Exception> errorHandler) {
        super(authorizationService,
                context,
                connectionId,
                peersCapability,
                peersNetworkLoadSnapshot,
                connectionMetrics,
                connectionThrottle,
                handler,
                errorHandler);
    }
}
