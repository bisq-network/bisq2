package bisq.api.access.filter.authz;

import bisq.api.access.filter.Headers;
import bisq.api.access.filter.RestApiFilter;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// TODO
@Slf4j
@Provider
@Priority(Priorities.AUTHORIZATION)
public class RestApiAuthorizationFilter extends RestApiFilter {
    private final UriValidator uriValidator;
    private final PermissionService permissionService;
    // Owned here rather than reached through PermissionService: the mapping answers what a REST
    // path requires, which is this filter's question and nobody else's. Built like uriValidator
    // above, for the same reason.
    private final RestPermissionMapping permissionMapping;

    public RestApiAuthorizationFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
        this.uriValidator = new UriValidator();
        this.permissionMapping = new RestPermissionMapping();
    }

    @Override
    public void doFilter(ContainerRequestContext context) {
        URI requestUri = context.getUriInfo().getRequestUri();
        try {
            uriValidator.validate(requestUri);

            String clientId = context.getHeaderString(Headers.CLIENT_ID);
            if (clientId == null) {
                throw new AuthorizationException("Missing clientId");
            }
            Optional<Set<Permission>> optionalPermissionSet = permissionService.findPermissions(clientId);
            if (optionalPermissionSet.isEmpty()) {
                throw new AuthorizationException("No permissions found for client " + clientId);
            }
            Set<Permission> granted = optionalPermissionSet.get();
            Permission required = permissionMapping.getRequiredPermission(requestUri.getPath(), context.getMethod());
            if (!permissionService.hasPermission(granted, required)) {
                // Structured body so clients can distinguish "this grant lacks one permission"
                // (e.g. paired before the node gained a feature — fixable by re-pairing) from a
                // plain 403 and prompt the user instead of showing a generic error. The caller
                // already knows its own grant, so naming the missing permission leaks nothing.
                log.warn("REST authz failed: required permission {} not granted. requestUri={}", required.name(), requestUri);
                context.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "permission_not_granted",
                                "required", required.name()))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
                return;
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