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

package bisq.api.access.filter;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.filter.authn.WebSocketHandshakeAuthenticationFilter;
import bisq.api.access.filter.meta.WebSocketHandshakeMetaDataEnrichment;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketFilter;

public class WebSocketFilterAddOn implements AddOn {
    private final ApiConfig apiConfig;
    private final SessionAuthenticationService sessionAuthenticationService;

    public WebSocketFilterAddOn(ApiConfig apiConfig,
                                SessionAuthenticationService sessionAuthenticationService) {
        this.apiConfig = apiConfig;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public void setup(NetworkListener listener, FilterChainBuilder builder) {
        int index = builder.indexOfType(WebSocketFilter.class);

        if (index < 0) {
            throw new IllegalStateException("WebSocketFilter not found. API security cannot be installed.");
        }

        // To intercept Websocket handshake we must place our filter before WebSocketFilter

        if (apiConfig.isSupportSessionHandling()) {
            builder.add(index, new WebSocketHandshakeAuthenticationFilter(sessionAuthenticationService));
            index++;
        }
        builder.add(index, new WebSocketHandshakeMetaDataEnrichment());
    }
}

