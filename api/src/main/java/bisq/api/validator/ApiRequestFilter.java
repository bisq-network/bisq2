package bisq.api.validator;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class ApiRequestFilter implements ContainerRequestFilter {
    private final RequestValidator validator;

    public ApiRequestFilter(Optional<List<String>> allowEndpoints, List<String> denyEndpoints) {
        this(new RequestValidator(allowEndpoints, denyEndpoints));
    }

    public ApiRequestFilter(RequestValidator requestValidator) {
        this.validator = requestValidator;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        URI requestUri = requestContext.getUriInfo().getRequestUri();
        if (!validator.hasValidComponents(requestUri)) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
        }
    }
}