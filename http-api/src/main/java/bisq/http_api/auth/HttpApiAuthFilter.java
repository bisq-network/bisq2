package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.http_api.access.AllowUnauthenticated;
import bisq.http_api.access.session.SessionService;
import bisq.http_api.access.session.SessionToken;
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
import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class HttpApiAuthFilter implements ContainerRequestFilter {
    private static final String BISQ_SESSION_ID_HEADER = "Bisq-Session-Id";
    private static final String BISQ_CLIENT_ID_HEADER = "Bisq-Client-Id";
    private static final int MAX_BODY_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int BUFFER_SIZE = 8192;

    @Context
    private ResourceInfo resourceInfo;

    private final SecretKey secretKey;
    private final Optional<SessionService> sessionService;

    public HttpApiAuthFilter(String password, Optional<SessionService> sessionService) {
        this.secretKey = AuthUtils.getSecretKey(password);
        this.sessionService = sessionService;
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        // Check if the endpoint is marked with @AllowUnauthenticated
        if (isUnauthenticatedEndpoint()) {
            log.debug("Allowing unauthenticated access to endpoint: {}", ctx.getUriInfo().getPath());
            return;
        }

        // Try HMAC auth first (desktop/CLI clients)
        if (isValidHmacAuth(ctx)) {
            return;
        }

        // Fall back to session-based auth (mobile clients that paired via QR code)
        if (isValidSessionAuth(ctx)) {
            return;
        }

        log.warn("HttpRequest rejected: No valid HMAC or session credentials for {}", ctx.getUriInfo().getPath());
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    private boolean isValidHmacAuth(ContainerRequestContext ctx) {
        URI requestUri = ctx.getUriInfo().getRequestUri();
        String normalizedPathAndQuery = AuthUtils.normalizePathAndQuery(requestUri);
        String timestamp = ctx.getHeaderString(AuthUtils.AUTH_TIMESTAMP_HEADER);
        String receivedHmac = ctx.getHeaderString(AuthUtils.AUTH_HEADER);
        String nonce = ctx.getHeaderString(AuthUtils.AUTH_NONCE_HEADER);
        try {
            String bodySha256Hex = getBodySha256Hex(ctx);
            return AuthUtils.isValidAuthentication(secretKey, ctx.getMethod(), normalizedPathAndQuery, nonce, timestamp, receivedHmac, bodySha256Hex);
        } catch (Exception e) {
            log.error("Error while reading body of request for HMAC authentication", e);
            return false;
        }
    }

    private boolean isValidSessionAuth(ContainerRequestContext ctx) {
        if (sessionService.isEmpty()) {
            return false;
        }

        String sessionId = ctx.getHeaderString(BISQ_SESSION_ID_HEADER);
        String clientId = ctx.getHeaderString(BISQ_CLIENT_ID_HEADER);

        if (sessionId == null || clientId == null) {
            return false;
        }

        Optional<SessionToken> token = sessionService.get().find(sessionId);
        if (token.isEmpty()) {
            log.debug("Session not found for sessionId: {}", sessionId);
            return false;
        }

        SessionToken sessionToken = token.get();
        if (sessionToken.isExpired()) {
            log.debug("Session expired for sessionId: {}", sessionId);
            return false;
        }

        if (!sessionToken.getClientId().equals(clientId)) {
            log.warn("Client ID mismatch for sessionId: {}", sessionId);
            return false;
        }

        log.debug("REST API request authenticated via session credentials (clientId: {})", clientId);
        return true;
    }

    private boolean isUnauthenticatedEndpoint() {
        if (resourceInfo == null) {
            log.warn("ResourceInfo is null - cannot check for @AllowUnauthenticated annotation. This is likely a JAX-RS context injection issue.");
            return false;
        }

        Method resourceMethod = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        // Check if method or class is annotated with @AllowUnauthenticated
        boolean isUnauthenticated = (resourceMethod != null && resourceMethod.isAnnotationPresent(AllowUnauthenticated.class)) ||
               (resourceClass != null && resourceClass.isAnnotationPresent(AllowUnauthenticated.class));

        if (isUnauthenticated) {
            log.info("Endpoint {} is marked as @AllowUnauthenticated",
                    resourceMethod != null ? resourceMethod.getName() : resourceClass != null ? resourceClass.getSimpleName() : "unknown");
        }

        return isUnauthenticated;
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