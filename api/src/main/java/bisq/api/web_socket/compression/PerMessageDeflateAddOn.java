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

package bisq.api.web_socket.compression;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketFilter;

public class PerMessageDeflateAddOn implements AddOn {
    @Override
    public void setup(NetworkListener listener, FilterChainBuilder builder) {
        int index = builder.indexOfType(WebSocketFilter.class);

        if (index < 0) {
            throw new IllegalStateException("WebSocketFilter not found. WebSocket compression cannot be installed.");
        }

        // The filter must sit directly below the WebSocketFilter so that no other filter ever observes
        // compressed frames. Registering this add-on last puts it there.
        builder.add(index, new PerMessageDeflateFilter());
    }
}
