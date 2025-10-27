package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.http_api.config.CommonApiConfig;
import bisq.security.DigestUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.MessageDigest;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private final byte[] passwordHash;

    public HttpApiAuthFilter(String password) {
        this.passwordHash = DigestUtil.sha256(password.getBytes());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (passwordHash.length > 0) {
            byte[] authTokenBytes = null;
            try {
                authTokenBytes = Hex.decode(requestContext.getHeaderString(AuthConstants.AUTH_HEADER));
            } catch (Exception e) {
                // no need to handle
            }

            if (authTokenBytes == null || !MessageDigest.isEqual(authTokenBytes, passwordHash)) {
                log.warn("HttpRequest rejected: Invalid or missing authorization token");
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }
    }

    public static HttpApiAuthFilter from(CommonApiConfig config) {
        return new HttpApiAuthFilter(config.getPassword());
    }
}