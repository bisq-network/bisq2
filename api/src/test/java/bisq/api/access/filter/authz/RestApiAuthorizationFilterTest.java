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

package bisq.api.access.filter.authz;

import bisq.api.access.filter.Headers;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestApiAuthorizationFilterTest {

    @SuppressWarnings("unchecked")
    private static PermissionService<RestPermissionMapping> permissionService(Set<Permission> granted) {
        RestPermissionMapping mapping = mock(RestPermissionMapping.class);
        when(mapping.getRequiredPermission("/api/v1/offerbook/markets", "GET")).thenReturn(Permission.OFFERBOOK);
        PermissionService<RestPermissionMapping> permissionService = mock(PermissionService.class);
        when(permissionService.getPermissionMapping()).thenReturn(mapping);
        when(permissionService.findPermissions("client-1")).thenReturn(Optional.of(granted));
        when(permissionService.hasPermission(granted, Permission.OFFERBOOK))
                .thenReturn(granted.contains(Permission.OFFERBOOK));
        return permissionService;
    }

    private static ContainerRequestContext requestContext() {
        ContainerRequestContext context = mock(ContainerRequestContext.class, RETURNS_DEEP_STUBS);
        when(context.getUriInfo().getRequestUri()).thenReturn(URI.create("http://127.0.0.1:8090/api/v1/offerbook/markets"));
        when(context.getMethod()).thenReturn("GET");
        when(context.getHeaderString(Headers.CLIENT_ID)).thenReturn("client-1");
        return context;
    }

    @Test
    void missingPermissionAborts403WithStructuredBodyNamingThePermission() {
        // The connect app parses this body to prompt "re-pair to enable new features" instead of
        // a generic error — the case where a client paired before the node gained a permission.
        Set<Permission> grantedWithoutOfferbook = EnumSet.of(Permission.SETTINGS, Permission.TRADES);
        RestApiAuthorizationFilter filter = new RestApiAuthorizationFilter(permissionService(grantedWithoutOfferbook));
        ContainerRequestContext context = requestContext();

        filter.doFilter(context);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(captor.capture());
        Response response = captor.getValue();
        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        // Filter-aborted responses bypass @Produces resolution, so the type must be set explicitly.
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity())
                .isEqualTo(Map.of("error", "permission_not_granted", "required", Permission.OFFERBOOK.name()));
    }

    @Test
    void grantedPermissionPassesWithoutAborting() {
        RestApiAuthorizationFilter filter = new RestApiAuthorizationFilter(permissionService(EnumSet.allOf(Permission.class)));
        ContainerRequestContext context = requestContext();

        filter.doFilter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownClientAborts403WithoutTheStructuredBody() {
        // Revoked/unknown clients keep the bare 403: the session-path handling in the client
        // already routes that case to re-pairing, and the body must not aid probing.
        @SuppressWarnings("unchecked")
        PermissionService<RestPermissionMapping> permissionService = mock(PermissionService.class, RETURNS_DEEP_STUBS);
        when(permissionService.findPermissions("client-1")).thenReturn(Optional.empty());
        RestApiAuthorizationFilter filter = new RestApiAuthorizationFilter(permissionService);
        ContainerRequestContext context = requestContext();

        filter.doFilter(context);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        assertThat(captor.getValue().getEntity()).isNull();
    }
}
