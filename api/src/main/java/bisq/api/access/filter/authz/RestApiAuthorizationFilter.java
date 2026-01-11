package bisq.api.access.filter.authz;

import bisq.api.access.filter.Headers;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// TODO
@Slf4j
@Provider
@Priority(Priorities.AUTHORIZATION)
public class RestApiAuthorizationFilter implements ContainerRequestFilter {
    private final HttpEndpointValidator httpEndpointValidator;
    private final PermissionService<RestPermissionMapping> permissionService;

    public RestApiAuthorizationFilter(PermissionService<RestPermissionMapping> permissionService,
                                      Optional<List<String>> allowEndpoints,
                                      List<String> denyEndpoints) {
        this.permissionService = permissionService;
        this.httpEndpointValidator = new HttpEndpointValidator(allowEndpoints, denyEndpoints);
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        URI requestUri = context.getUriInfo().getRequestUri();
        try {
            String path = requestUri.getPath();
            httpEndpointValidator.validate(path);

            String deviceId = context.getHeaderString(Headers.DEVICE_ID);
            if (deviceId == null) {
                throw new AuthorizationException("Missing device ID header");
            }
            Optional<Set<Permission>> optionalPermissionSet = permissionService.findPermissions(UUID.fromString(deviceId));
            if (optionalPermissionSet.isEmpty()) {
                throw new AuthorizationException("No permissions found for device " + deviceId);
            }
            Set<Permission> granted = optionalPermissionSet.get();
            Permission required = permissionService.getPermissionMapping().getRequiredPermission(path, context.getMethod());
            if (!permissionService.hasPermission(granted, required)) {
                throw new AuthorizationException(String.format("Required permission %s not granted. Granted permissions: %s", required.name(), granted));
            }
        } catch (Exception e) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }
}