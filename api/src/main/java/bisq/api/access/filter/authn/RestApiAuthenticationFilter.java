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
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Priority(Priorities.AUTHENTICATION)
// @Provider omitted as we do manual registration
public class RestApiAuthenticationFilter implements ContainerRequestFilter {
    private final SessionAuthenticationService sessionAuthenticationService;

    public RestApiAuthenticationFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        try {
            URI requestUri = context.getUriInfo().getRequestUri();
            String bodySha256Hex = AuthUtils.getBodySha256Hex(context);

            AuthenticatedSession session = sessionAuthenticationService.authenticate(
                    context.getHeaderString(Headers.SESSION_ID),
                    context.getMethod(),
                    requestUri,
                    context.getHeaderString(Headers.NONCE),
                    context.getHeaderString(Headers.TIMESTAMP),
                    context.getHeaderString(Headers.SIGNATURE),
                    Optional.ofNullable(bodySha256Hex)
            );

            context.setProperty(Attributes.IS_AUTHENTICATED, true);
            context.setProperty(Attributes.SESSION_ID, session.getSessionId());
            context.setProperty(Attributes.DEVICE_ID, session.getDeviceId());

        } catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity(e.getMessage())
                            .build()
            );
        }
    }
}
