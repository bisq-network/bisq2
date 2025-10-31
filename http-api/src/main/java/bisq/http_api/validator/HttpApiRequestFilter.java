package bisq.http_api.validator;

import bisq.http_api.config.CommonApiConfig;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class HttpApiRequestFilter implements ContainerRequestFilter {
    private final RequestValidator validator;

    public HttpApiRequestFilter(List<String> whitelist, List<String> blacklist) {
        this(new RequestValidator(whitelist, blacklist));
    }

    public HttpApiRequestFilter(RequestValidator requestValidator) {
        this.validator = requestValidator;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        URI requestUri = requestContext.getUriInfo().getRequestUri();
        if (!validator.hasValidComponents(requestUri)) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
        }
    }

    public static HttpApiRequestFilter from(CommonApiConfig config) {
        return new HttpApiRequestFilter(config.getWhiteListEndPoints(), config.getBlackListEndPoints());
    }
}