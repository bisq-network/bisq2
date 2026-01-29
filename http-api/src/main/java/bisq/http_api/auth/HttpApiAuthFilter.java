package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.http_api.access.AllowUnauthenticated;
import bisq.security.DigestUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private static final int MAX_BODY_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int BUFFER_SIZE = 8192;

    @Context
    private ResourceInfo resourceInfo;

    private final SecretKey secretKey;

    public HttpApiAuthFilter(String password) {
        this.secretKey = AuthUtils.getSecretKey(password);
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        // Check if the endpoint is marked with @AllowUnauthenticated
        if (isUnauthenticatedEndpoint()) {
            log.debug("Allowing unauthenticated access to endpoint: {}", ctx.getUriInfo().getPath());
            return;
        }

        URI requestUri = ctx.getUriInfo().getRequestUri();
        String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(requestUri);
        String timestamp = ctx.getHeaderString(AuthUtils.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = ctx.getHeaderString(AuthUtils.AUTH_HEADER);
        String nonce = ctx.getHeaderString(AuthUtils.AUTH_NONCE_HEADER);
        try {
            String bodySha256Hex = getBodySha256Hex(ctx);
            if (!AuthUtils.isValidAuthentication(secretKey, ctx.getMethod(), normalizedPathAndQuery, nonce, timestamp, receivedHmac, bodySha256Hex)) {
                log.warn("HttpRequest rejected: Invalid or missing authorization token");
                ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        } catch (Exception e) {
            log.error("Error while reading body of request for authentication", e);
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private boolean isUnauthenticatedEndpoint() {
        if (resourceInfo == null) {
            return false;
        }

        Method resourceMethod = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        // Check if method or class is annotated with @AllowUnauthenticated
        return (resourceMethod != null && resourceMethod.isAnnotationPresent(AllowUnauthenticated.class)) ||
               (resourceClass != null && resourceClass.isAnnotationPresent(AllowUnauthenticated.class));
    }

    @Nullable
    private String getBodySha256Hex(ContainerRequestContext ctx) {
        // Content-Length may be spoofed; But it is an important indicator of a present body set by clients
        int declaredLength = ctx.getLength();

        if (declaredLength == 0) {
            return null;
        }

        // If length is known and too large, return early
        if (declaredLength > MAX_BODY_SIZE_BYTES) {
            throw new RuntimeException("Request body exceeds maximum allowed size of " + MAX_BODY_SIZE_BYTES + " bytes");
        }

        try {
            InputStream entityStream = ctx.getEntityStream();
            // Use declared length if larger, otherwise use default buffer size to
            // reduce unnecessary array copying if a small number was used maliciously
            int initialCapacity = Math.max(declaredLength, BUFFER_SIZE);
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(initialCapacity)) {
                byte[] chunk = new byte[BUFFER_SIZE];
                int total = 0;
                int read;
                while ((read = entityStream.read(chunk)) != -1) {
                    total += read;
                    if (total > MAX_BODY_SIZE_BYTES) {
                        throw new RuntimeException("Request body exceeds maximum allowed size of " + MAX_BODY_SIZE_BYTES + " bytes");
                    }
                    buffer.write(chunk, 0, read);
                }
                byte[] bytes = buffer.toByteArray();

                // Empty body is valid - return null to indicate no body content
                if (bytes.length == 0) {
                    return null;
                }

                ctx.setEntityStream(new ByteArrayInputStream(bytes));
                return Hex.encode(DigestUtil.sha256(bytes));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read request body for authentication. This will result in auth failure", e);
        }
    }
}