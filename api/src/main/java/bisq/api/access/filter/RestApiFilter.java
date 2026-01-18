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

import bisq.api.access.filter.authn.AllowUnauthenticated;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Priority(Priorities.AUTHENTICATION)
public abstract class RestApiFilter implements ContainerRequestFilter {
    @Context
    private ResourceInfo resourceInfo;

    public RestApiFilter() {
    }

    @Override
    public void filter(ContainerRequestContext context) {
        if (!isAllowUnauthenticated()) {
            doFilter(context);
        }
    }

    protected abstract void doFilter(ContainerRequestContext context);

    protected boolean isAllowUnauthenticated( ) {
        return resourceInfo.getResourceMethod().isAnnotationPresent(AllowUnauthenticated.class)
                || resourceInfo.getResourceClass().isAnnotationPresent(AllowUnauthenticated.class);
    }
}
