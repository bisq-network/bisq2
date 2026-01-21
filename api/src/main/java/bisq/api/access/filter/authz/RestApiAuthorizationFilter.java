package bisq.api.access.filter.authz;

import bisq.api.access.filter.Attributes;
import bisq.api.access.filter.RestApiFilter;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TODO
@Slf4j
@Provider
@Priority(Priorities.AUTHORIZATION)
public class RestApiAuthorizationFilter extends RestApiFilter {
    private final HttpEndpointValidator httpEndpointValidator;
    private final PermissionService<RestPermissionMapping> permissionService;

    public RestApiAuthorizationFilter(PermissionService<RestPermissionMapping> permissionService,
                                      Optional<List<String>> allowEndpoints,
                                      List<String> denyEndpoints) {
        this.permissionService = permissionService;
        this.httpEndpointValidator = new HttpEndpointValidator(allowEndpoints, denyEndpoints);
    }

    @Override
    public void doFilter(ContainerRequestContext context) {
        URI requestUri = context.getUriInfo().getRequestUri();
        try {
            httpEndpointValidator.validate(requestUri);

            String clientId = (String) context.getProperty(Attributes.CLIENT_ID);
            if (clientId == null) {
                throw new AuthorizationException("Missing authenticated device ID");
            }
            Optional<Set<Permission>> optionalPermissionSet = permissionService.findPermissions(clientId);
            if (optionalPermissionSet.isEmpty()) {
                throw new AuthorizationException("No permissions found for device " + clientId);
            }
            Set<Permission> granted = optionalPermissionSet.get();
            Permission required = permissionService.getPermissionMapping().getRequiredPermission(requestUri.getPath(), context.getMethod());
            if (!permissionService.hasPermission(granted, required)) {
                throw new AuthorizationException(String.format("Required permission %s not granted. Granted permissions: %s",
                        required.name(), granted));
            }
        } catch (AuthorizationException | IllegalArgumentException | ForbiddenException e) {
            log.warn("REST authz failed. requestUri={}", requestUri, e);
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        } catch (Exception e) {
            log.warn("REST authz failed unexpectedly. requestUri={}", requestUri, e);
            context.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
    }
}