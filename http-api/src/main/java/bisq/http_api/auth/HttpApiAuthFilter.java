package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.security.DigestUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private static final int MAX_BODY_SIZE_BYTES = 5 * 1024 * 1024; // 10 MB

    private final SecretKey secretKey;

    public HttpApiAuthFilter(String password) {
        this.secretKey = AuthUtils.getSecretKey(password);
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        URI requestUri = ctx.getUriInfo().getRequestUri();
        String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(requestUri);
        String timestamp = ctx.getHeaderString(AuthUtils.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = ctx.getHeaderString(AuthUtils.AUTH_HEADER);
        String bodySha256Hex = getBodySha256Hex(ctx);
        if (!AuthUtils.isValidAuthentication(secretKey, ctx.getMethod(), normalizedPathAndQuery, timestamp, receivedHmac, bodySha256Hex)) {
            log.warn("HttpRequest rejected: Invalid or missing authorization token");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    @Nullable
    private String getBodySha256Hex(ContainerRequestContext ctx) {
        if (ctx.getLength() <= 0) {
            return null;
        }
        if (ctx.getLength() > MAX_BODY_SIZE_BYTES) {
            log.warn("Request body too large: {} bytes, max: {}. This will result in auth failure", ctx.getLength(), MAX_BODY_SIZE_BYTES);
            return null;
        }
        try {
            InputStream entityStream = ctx.getEntityStream();
            byte[] bytes = entityStream.readAllBytes();
            ctx.setEntityStream(new ByteArrayInputStream(bytes));
            return Hex.encode(DigestUtil.sha256(bytes));
        } catch (Exception e) {
            log.error("Failed to read request body for authentication. This will result in auth failure", e);
            return null;
        }
    }
}