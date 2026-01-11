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

import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.filter.authn.WebSocketHandshakeAuthenticationFilter;
import bisq.api.access.filter.meta.WebSocketHandshakeMetaDataEnrichment;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

public class AccessFilterAddOn implements AddOn {
    private final PermissionService<RestPermissionMapping> permissionService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public AccessFilterAddOn(PermissionService<RestPermissionMapping> permissionService,
                             SessionAuthenticationService sessionAuthenticationService) {
        this.permissionService = permissionService;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public void setup(NetworkListener listener, FilterChainBuilder builder) {
        int index = builder.indexOfType(HttpServerFilter.class);

        if (index < 0) {
            throw new IllegalStateException("HttpServerFilter not found. API security cannot be installed safely.");
        }

        // Any filter that reads HTTP headers must be placed strictly after HttpServerFilter
        builder.add(++index, new WebSocketHandshakeMetaDataEnrichment());
        builder.add(++index, new WebSocketHandshakeAuthenticationFilter(sessionAuthenticationService));
    }
}

