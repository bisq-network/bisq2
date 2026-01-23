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

package bisq.api.access.filter.authn;

import bisq.api.access.filter.Attributes;
import bisq.api.access.filter.Headers;
import bisq.api.access.filter.RestApiFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
@Priority(Priorities.AUTHENTICATION)
// @Provider omitted as we do manual registration
public class RestApiSessionAuthenticationFilter extends RestApiFilter {
    private final SessionAuthenticationService sessionAuthenticationService;

    public RestApiSessionAuthenticationFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public void doFilter(ContainerRequestContext context) {
        URI requestUri = null;
        try {
            requestUri = context.getUriInfo().getRequestUri();

            AuthenticatedSession session = sessionAuthenticationService.authenticate(
                    context.getHeaderString(Headers.CLIENT_ID),
                    context.getHeaderString(Headers.SESSION_ID)
            );

            context.setProperty(Attributes.IS_AUTHENTICATED, true);
            context.setProperty(Attributes.SESSION_ID, session.getSessionId());
            context.setProperty(Attributes.CLIENT_ID, session.getClientId());
        } catch (Exception e) {
            log.warn("Authentication failed. requestUri={}", requestUri, e);
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Unauthorized")
                            .build()
            );
        }
    }
}
