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

import javax.annotation.Nullable;
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
    public void filter(ContainerRequestContext ctx) throws IOException {
        String passwordHash = ctx.getHeaderString(AuthConstants.AUTH_HEADER);
        if (!isCorrectPassword(passwordHash)) {
            log.warn("HttpRequest rejected: Invalid or missing authorization token");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private boolean isCorrectPassword(@Nullable String receivedPassHash) {
        byte[] receivedPassHashBytes = null;
        if (receivedPassHash != null) {
            try {
                receivedPassHashBytes = Hex.decode(receivedPassHash);
            } catch (Exception e) {
                // no need to handle
            }
        }
        return receivedPassHashBytes != null && MessageDigest.isEqual(receivedPassHashBytes, passwordHash);
    }


    public static HttpApiAuthFilter from(CommonApiConfig config) {
        return new HttpApiAuthFilter(config.getPassword());
    }
}