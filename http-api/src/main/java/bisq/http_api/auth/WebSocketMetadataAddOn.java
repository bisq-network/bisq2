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

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

public class WebSocketMetadataAddOn implements AddOn {

    @Override
    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        // WebSocketRequestMetadataFilter must be inserted before the HttpServerFilter to
        // properly intercept and capture WebSocket metadata during the upgrade handshake.
        int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);
        if (httpServerFilterIdx >= 0) {
            builder.add(httpServerFilterIdx, new WebSocketRequestMetadataFilter());
        } else {
            throw new RuntimeException("Expected HttpServerFilter to be present but was not. This prevents WebSocketRequestMetadataFilter from setting up correctly.");
        }
    }
}
