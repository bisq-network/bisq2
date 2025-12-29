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

package bisq.http_api.auth;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;

import java.net.SocketAddress;
import java.util.Objects;

@Slf4j
public class WebSocketRequestMetadataFilter extends BaseFilter {
    public static final String ATTR_WS_USER_AGENT = "ws_user_agent";
    public static final String ATTR_WS_REMOTE_ADDRESS = "ws_remote_address";

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        Object message = ctx.getMessage();

        if (message instanceof HttpContent httpContent && httpContent.getHttpHeader() instanceof HttpRequestPacket request) {
            String upgradeHeader = request.getHeader("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                if (request.getAttribute(ATTR_WS_USER_AGENT) != null) {
                    return ctx.getInvokeAction();
                }

                Object peer = ctx.getConnection().getPeerAddress();
                if (peer instanceof SocketAddress) {
                    request.setAttribute(ATTR_WS_REMOTE_ADDRESS, peer.toString());
                }

                String ua = request.getHeader("User-Agent");
                request.setAttribute(ATTR_WS_USER_AGENT, Objects.requireNonNullElse(ua, "-"));
            }
        }

        // http api requests are handled using HttpApiAuthFilter
        return ctx.getInvokeAction();
    }
}
