package bisq.http_api.auth;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private final SecretKey secretKey;

    public HttpApiAuthFilter(String password) {
        this.secretKey = AuthConstants.getSecretKey(password);
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String timestamp = ctx.getHeaderString(AuthConstants.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = ctx.getHeaderString(AuthConstants.AUTH_HEADER);
        if (!AuthConstants.isValidAuthentication(secretKey, timestamp, receivedHmac)) {
            log.warn("HttpRequest rejected: Invalid or missing authorization token");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}